package com.homeservices.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProviderSuggestion {

    private String id;

    private String name;

    private String serviceArea;

    private Integer experience;
}