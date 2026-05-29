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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GrokAIService(
            ProviderRepository providerRepository,
            @Value("${spring.ai.groq.api-key}") String apiKey,
            @Value("${spring.ai.groq.base-url}") String baseUrl) {
        this.providerRepository = providerRepository;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Primary chat endpoint — used for both text and voice chat
    // ──────────────────────────────────────────────────────────────────────────

    public AIChatResponse ask(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        log.info("AI chat request: {}", userMessage);

        String category = detectCategory(userMessage);
        List<Provider> providers = providerRepository.findTop5ByVerifiedTrueAndActiveTrue();

        String providerContext = providers.isEmpty()
                ? "No providers currently available."
                : providers.stream()
                        .map(p -> "• %s | %d yrs exp | Area: %s | Rating: %.1f"
                                .formatted(
                                        p.getUser().getName(),
                                        p.getExperienceYears(),
                                        p.getServiceArea(),
                                        p.getRating() != null ? p.getRating() : 0.0))
                        .collect(Collectors.joining("\n"));

        /*
         * Structured response prompt — AI returns a JSON block so we can
         * extract costEstimate and severityLevel reliably without a second API call.
         */
        String systemPrompt = """
                You are an expert AI assistant for ApnaAdmi Home Appliance Repair Services (India).
                
                Your job:
                1. Diagnose the appliance problem described by the user.
                2. Explain likely causes clearly (2-3 sentences).
                3. If the issue is MEDIUM or HIGH severity, provide a cost estimate in Indian Rupees.
                4. Suggest the best available technician from the list below.
                5. Never recommend dangerous DIY repairs for electrical/gas appliances.
                
                Available Technicians:
                %s
                
                IMPORTANT — Respond ONLY in this exact JSON format (no markdown, no extra text):
                {
                  "message": "Your friendly diagnosis and advice here. Be concise and practical.",
                  "severity": "LOW|MEDIUM|HIGH",
                  "costMin": 0,
                  "costMax": 0,
                  "visitCharge": 200,
                  "estimatedTime": "1-2 hours",
                  "recommendation": "Brief recommendation if severity is MEDIUM or HIGH, else empty string"
                }
                
                For LOW severity: set costMin=0, costMax=0 and leave recommendation empty.
                For MEDIUM/HIGH: provide realistic INR cost ranges.
                """.formatted(providerContext);

        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0.4
        );

        try {
            @SuppressWarnings("rawtypes")
            Map response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            clientResponse -> clientResponse.bodyToMono(String.class).map(body -> {
                                log.error("Groq API Error: {}", body);
                                return new RuntimeException(body);
                            })
                    )
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            @SuppressWarnings("unchecked")
            List<?> choices = (List<?>) response.get("choices");
            @SuppressWarnings("unchecked")
            Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
            @SuppressWarnings("unchecked")
            Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
            String rawContent = message.get("content").toString().trim();

            // Parse the structured JSON response
            return parseStructuredResponse(rawContent, category, providers);

        } catch (Exception e) {
            log.error("AI Service Error", e);
            throw new RuntimeException("Unable to generate AI response: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private AIChatResponse parseStructuredResponse(
            String rawContent, String category, List<Provider> providers) {

        String aiMessage;
        AIChatResponse.CostEstimate costEstimate = null;

        try {
            // Strip potential markdown code fences
            String json = rawContent
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);

            aiMessage = (String) parsed.getOrDefault("message", rawContent);
            String severity = (String) parsed.getOrDefault("severity", "LOW");
            int costMin = toInt(parsed.get("costMin"));
            int costMax = toInt(parsed.get("costMax"));
            int visitCharge = toInt(parsed.getOrDefault("visitCharge", 200));
            String estimatedTime = (String) parsed.getOrDefault("estimatedTime", "");
            String recommendation = (String) parsed.getOrDefault("recommendation", "");

            // Only attach cost card for MEDIUM or HIGH severity issues
            if (!"LOW".equalsIgnoreCase(severity) && costMax > 0) {
                costEstimate = AIChatResponse.CostEstimate.builder()
                        .min(costMin)
                        .max(costMax)
                        .severity(severity.toUpperCase())
                        .visitCharge(visitCharge)
                        .estimatedTime(estimatedTime)
                        .recommendation(recommendation)
                        .build();
            }
        } catch (Exception e) {
            // If JSON parsing fails, use raw content as message
            log.warn("Could not parse structured AI response, using raw text. Error: {}", e.getMessage());
            aiMessage = rawContent;
        }

        List<ProviderSuggestion> suggestions = providers.stream()
                .map(p -> ProviderSuggestion.builder()
                        .id(p.getId().toString())
                        .name(p.getUser().getName())
                        .serviceArea(p.getServiceArea())
                        .experience(p.getExperienceYears())
                        .build())
                .toList();

        return AIChatResponse.builder()
                .category(category)
                .response(aiMessage)
                .providers(suggestions)
                .costEstimate(costEstimate)
                .build();
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return 0; }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Category detection
    // ──────────────────────────────────────────────────────────────────────────

    private String detectCategory(String message) {
        String m = message.toLowerCase();
        if (m.contains("ac") || m.contains("air conditioner") || m.contains("airconditioner"))
            return "AC_REPAIR";
        if (m.contains("washing machine") || m.contains("washer"))
            return "WASHING_MACHINE";
        if (m.contains("fridge") || m.contains("refrigerator"))
            return "REFRIGERATOR";
        if (m.contains("tv") || m.contains("television"))
            return "TV_REPAIR";
        if (m.contains("microwave"))
            return "MICROWAVE";
        if (m.contains("geyser") || m.contains("water heater"))
            return "GEYSER";
        if (m.contains("dishwasher"))
            return "DISHWASHER";
        return "GENERAL_APPLIANCE";
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Standalone cost estimation (used by /chat/estimate-cost endpoint)
    // ──────────────────────────────────────────────────────────────────────────

    public Map<String, Object> estimateCost(String appliance, String problem, boolean urgent) {

        String prompt = """
                You are a home appliance repair cost estimation expert (India).
                
                Appliance: %s
                Problem: %s
                Urgent Service: %s
                
                Respond ONLY in this JSON format (no markdown, no extra text):
                {
                  "estimatedMin": 0,
                  "estimatedMax": 0,
                  "visitCharge": 200,
                  "partsLikelyNeeded": [],
                  "severity": "LOW|MEDIUM|HIGH",
                  "estimatedTime": "",
                  "recommendation": ""
                }
                """.formatted(appliance, problem, urgent ? "YES (add 20%% surcharge)" : "NO");

        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(
                        Map.of("role", "system", "content", "You are an expert appliance repair cost estimator for India."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2
        );

        try {
            @SuppressWarnings("rawtypes")
            Map response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            @SuppressWarnings("unchecked")
            List<?> choices = (List<?>) response.get("choices");
            @SuppressWarnings("unchecked")
            Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
            @SuppressWarnings("unchecked")
            Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
            String aiReply = message.get("content").toString()
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            return objectMapper.readValue(aiReply, Map.class);

        } catch (Exception e) {
            log.error("Estimate Cost Error", e);
            throw new RuntimeException("Unable to estimate repair cost: " + e.getMessage());
        }
    }
}
