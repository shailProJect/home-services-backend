package com.homeservices.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProviderProfileRequest {

    private Integer experienceYears;

    @NotBlank(message = "Service area is required")
    private String serviceArea;

    private Double latitude;

    private Double longitude;
}
