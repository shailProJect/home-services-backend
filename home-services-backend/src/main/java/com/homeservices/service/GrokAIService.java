package com.homeservices.service;

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

    log.info("AI request received: {}", userMessage);

    // ─────────────────────────────────────────────────────────────
    // STEP 1: Detect category from user issue
    // ─────────────────────────────────────────────────────────────

    String category = detectCategory(userMessage);

    // ─────────────────────────────────────────────────────────────
    // STEP 2: Fetch providers from database
    // ─────────────────────────────────────────────────────────────

    List<Provider> providers = providerRepository.findTop5ByVerifiedTrueAndActiveTrue();

    String providerContext = null;

    if (providers.isEmpty()) {

      providerContext = "No providers currently available.";

    } else {

      providers.stream().map((Provider p) -> """
          Provider Name: %s
          Experience: %s years
          Service Area: %s
          """.formatted(p.getUser().getName(), p.getExperienceYears(), p.getServiceArea()));
    }

    // ─────────────────────────────────────────────────────────────
    // STEP 3: Build AI Prompt
    // ─────────────────────────────────────────────────────────────

    String systemPrompt = """
        You are an AI assistant for ApnaAdmi Home Appliance Services.

        Your responsibilities:
        - Help users troubleshoot appliance issues
        - Explain likely causes
        - Suggest safe repair advice
        - Recommend suitable technicians
        - Keep responses concise and practical
        - Never recommend dangerous repairs
        - Recommend service categories if relevant

        Available Providers:
        %s
        """.formatted(providerContext);

    Map<String, Object> request = Map.of("model", "llama-3.3-70b-versatile", "messages",
        List.of(Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userMessage)),
        "temperature", 0.7);

    try {

      Map response = webClient.post().uri("/chat/completions").bodyValue(request).retrieve()

          .onStatus(status -> status.isError(),
              clientResponse -> clientResponse.bodyToMono(String.class).map(errorBody -> {
                log.error("Groq API Error: {}", errorBody);
                return new RuntimeException(errorBody);
              }))

          .bodyToMono(Map.class).timeout(Duration.ofSeconds(20)).block();

      // ─────────────────────────────────────────────────────────
      // Extract AI response
      // ─────────────────────────────────────────────────────────

      List<?> choices = (List<?>) response.get("choices");

      Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);

      Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");

      String aiReply = message.get("content").toString();

      // ─────────────────────────────────────────────────────────
      // Build provider suggestions
      // ─────────────────────────────────────────────────────────

      List<ProviderSuggestion> providerSuggestions = providers.stream()
          .map((Provider provider) -> ProviderSuggestion.builder()
              .id(provider.getId().toString())
              .name(provider.getUser().getName())
              .serviceArea(provider.getServiceArea())
              .experience(provider.getExperienceYears())
              .build())
          .toList();

      return AIChatResponse.builder().category(category).response(aiReply)
          .providers(providerSuggestions).build();

    } catch (Exception e) {

      log.error("AI Service Error", e);

      throw new RuntimeException("Unable to generate AI response: " + e.getMessage());
    }
  }

  // ─────────────────────────────────────────────────────────────
  // Simple category detection
  // ─────────────────────────────────────────────────────────────

  private String detectCategory(String message) {

    String msg = message.toLowerCase();

    if (msg.contains("ac") || msg.contains("air conditioner")) {
      return "AC_REPAIR";
    }

    if (msg.contains("washing machine")) {
      return "WASHING_MACHINE";
    }

    if (msg.contains("fridge") || msg.contains("refrigerator")) {
      return "REFRIGERATOR";
    }

    if (msg.contains("tv") || msg.contains("television")) {
      return "TV_REPAIR";
    }

    if (msg.contains("microwave")) {
      return "MICROWAVE";
    }

    return "GENERAL_APPLIANCE";
  }
}
