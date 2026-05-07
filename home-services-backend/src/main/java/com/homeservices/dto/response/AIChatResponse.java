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
}