package com.homeservices.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ImageDiagnosisResponse {
    /** AI-identified appliance type */
    private String applianceType;

    /** AI-identified problem */
    private String problemDetected;

    /** Detailed diagnosis text */
    private String diagnosis;

    /** Suggested service category */
    private String suggestedCategory;

    /** Estimated cost range */
    private Double estimatedCostMin;
    private Double estimatedCostMax;

    /** Urgency level: LOW, MEDIUM, HIGH */
    private String urgencyLevel;

    /** Recommended providers */
    private List<ProviderSuggestion> suggestedProviders;
}
