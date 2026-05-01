package com.homeservices.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ProviderServiceResponse {
    private UUID id;
    private UUID providerId;
    private String providerName;
    private UUID categoryId;
    private String categoryName;
    private String serviceName;
    private BigDecimal price;
    private Integer durationMinutes;
    private boolean active;
}
