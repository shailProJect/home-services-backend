package com.homeservices.service;

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

  private final WebClient webClient;

  public GrokAIService(@Value("${spring.ai.groq.api-key}") String apiKey,
      @Value("${spring.ai.groq.base-url}") String baseUrl) {

    this.webClient =
        WebClient.builder().baseUrl(baseUrl).defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json").build();
  }

  public String ask(String message) {

    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("Message cannot be empty");
    }

    log.info("Sending request to Groq: {}", message);

    Map<String, Object> request = Map.of("model", "llama-3.1-8b-instant", "messages",
        List.of(Map.of("role", "user", "content", message)), "temperature", 0.7);

    try {
      return webClient.post().uri("/chat/completions").bodyValue(request).retrieve()

          // ✅ show real Groq error
          .onStatus(status -> status.isError(),
              response -> response.bodyToMono(String.class).map(body -> {
                log.error("Groq API error: {}", body);
                return new RuntimeException("Groq API error: " + body);
              }))

          .bodyToMono(Map.class).timeout(Duration.ofSeconds(10))

          .map(res -> {
            var choices = (List<?>) res.get("choices");
            var first = (Map<?, ?>) choices.get(0);
            var msg = (Map<?, ?>) first.get("message");
            return msg.get("content").toString();
          }).block();

    } catch (Exception e) {
      log.error("Error calling Groq API", e);
      throw new RuntimeException("Groq API failed: " + e.getMessage(), e);
    }
  }
}
