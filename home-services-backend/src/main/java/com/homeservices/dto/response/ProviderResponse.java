package com.homeservices.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ProviderResponse {
    private UUID id;
    private UUID userId;
    private String name;
    private String email;
    private String phone;
    private Integer experienceYears;
    private String serviceArea;
    private Double latitude;
    private Double longitude;
    private boolean verified;
    private boolean active;
    private Double rating;
}
