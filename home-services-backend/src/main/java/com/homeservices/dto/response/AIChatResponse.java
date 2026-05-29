package com.homeservices.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AIChatResponse {

    private String category;

    private String response;

    private List<ProviderSuggestion> providers;

    /** Present only for MEDIUM or HIGH severity issues. Null for LOW severity. */
    private CostEstimate costEstimate;

    @Data
    @Builder
    public static class CostEstimate {
        private int min;
        private int max;
        private String severity;   // LOW | MEDIUM | HIGH
        private int visitCharge;
        private String estimatedTime;
        private String recommendation;
    }
}
