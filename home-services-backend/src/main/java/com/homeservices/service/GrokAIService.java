package com.homeservices.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeservices.dto.response.AIChatResponse;
import com.homeservices.dto.response.ProviderSuggestion;
import com.homeservices.entity.Provider;
import com.homeservices.repository.ProviderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GrokAIService {

  private final ProviderRepository providerRepository;
  private final WebClient webClient;

  public GrokAIService(ProviderRepository providerRepository,
      @Value("${spring.ai.groq.api-key}") String apiKey,
      @Value("${spring.ai.groq.base-url}") String baseUrl) {
    this.providerRepository = providerRepository;
    this.webClient =
        WebClient.builder().baseUrl(baseUrl).defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json").build();
  }

  public AIChatResponse ask(String userMessage) {
    if (userMessage == null || userMessage.isBlank()) {
      throw new IllegalArgumentException("Message cannot be empty");
    }
    log.info("AI request: {}", userMessage);

    String category = detectCategory(userMessage);
    List<Provider> providers = providerRepository.findTop5ByVerifiedTrueAndActiveTrue();

    // BUG FIX: original code called .stream().map().collect() result was discarded —
    // providerContext remained null for ALL non-empty lists, so AI never saw provider data.
    String providerContext = providers.isEmpty() ? "No providers currently available."
        : providers.stream().map(p -> "• %s | %d yrs exp | Area: %s"
            .formatted(p.getUser().getName(), p.getExperienceYears(), p.getServiceArea()))
            .collect(Collectors.joining("\n"));

    String systemPrompt = """
        You are an AI assistant for ApnaAdmi Home Appliance Services.
        Help users troubleshoot appliance issues, explain likely causes, suggest safe repairs,
        and recommend suitable technicians. Be concise and practical.
        Never recommend dangerous DIY repairs.

        Available Providers:
        %s
        """.formatted(providerContext);

    Map<String, Object> requestBody = Map.of("model", "llama-3.3-70b-versatile", "messages",
        List.of(Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userMessage)),
        "temperature", 0.7);

    try {
      @SuppressWarnings("rawtypes")
      Map response = webClient.post().uri("/chat/completions").bodyValue(requestBody).retrieve()
          .onStatus(status -> status.isError(),
              clientResponse -> clientResponse.bodyToMono(String.class).map(body -> {
                log.error("Groq API Error: {}", body);
                return new RuntimeException(body);
              }))
          .bodyToMono(Map.class).timeout(Duration.ofSeconds(20)).block();

      @SuppressWarnings("unchecked")
      List<?> choices = (List<?>) response.get("choices");
      @SuppressWarnings("unchecked")
      Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
      @SuppressWarnings("unchecked")
      Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
      String aiReply = message.get("content").toString();

      List<ProviderSuggestion> suggestions =
          providers.stream()
              .map(p -> ProviderSuggestion.builder().id(p.getId().toString())
                  .name(p.getUser().getName()).serviceArea(p.getServiceArea())
                  .experience(p.getExperienceYears()).build())
              .toList();

      return AIChatResponse.builder().category(category).response(aiReply).providers(suggestions)
          .build();

    } catch (Exception e) {
      log.error("AI Service Error", e);
      throw new RuntimeException("Unable to generate AI response: " + e.getMessage());
    }
  }

  private String detectCategory(String message) {
    String m = message.toLowerCase();
    if (m.contains("ac") || m.contains("air conditioner"))
      return "AC_REPAIR";
    if (m.contains("washing machine"))
      return "WASHING_MACHINE";
    if (m.contains("fridge") || m.contains("refrigerator"))
      return "REFRIGERATOR";
    if (m.contains("tv") || m.contains("television"))
      return "TV_REPAIR";
    if (m.contains("microwave"))
      return "MICROWAVE";
    return "GENERAL_APPLIANCE";
  }

  public Map<String, Object> estimateCost(String appliance, String problem, boolean urgent) {

    String prompt = """
        You are a home appliance repair cost estimation AI.

        Estimate repair cost in Indian Rupees for:

        Appliance: %s
        Problem: %s
        Urgent Service: %s

        Return JSON only in this format:
        {
          "estimatedMin": 0,
          "estimatedMax": 0,
          "visitCharge": 0,
          "partsLikelyNeeded": [],
          "severity": "LOW|MEDIUM|HIGH",
          "estimatedTime": "",
          "recommendation": ""
        }
        """.formatted(appliance, problem, urgent ? "YES" : "NO");

    Map<String, Object> requestBody = Map.of("model", "llama-3.3-70b-versatile", "messages",
        List.of(
            Map.of("role", "system", "content", "You are an expert appliance repair assistant."),
            Map.of("role", "user", "content", prompt)),
        "temperature", 0.3);

    try {

      @SuppressWarnings("rawtypes")
      Map response = webClient.post().uri("/chat/completions").bodyValue(requestBody).retrieve()
          .bodyToMono(Map.class).timeout(Duration.ofSeconds(20)).block();

      @SuppressWarnings("unchecked")
      List<?> choices = (List<?>) response.get("choices");

      @SuppressWarnings("unchecked")
      Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);

      @SuppressWarnings("unchecked")
      Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");

      String aiReply = message.get("content").toString();

      // Convert JSON string response into Map
      ObjectMapper mapper = new ObjectMapper();

      return mapper.readValue(aiReply, Map.class);

    } catch (Exception e) {
      log.error("Estimate Cost Error", e);

      throw new RuntimeException("Unable to estimate repair cost: " + e.getMessage());
    }
  }
}
