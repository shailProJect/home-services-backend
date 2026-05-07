package com.homeservices.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ServiceCategoryResponse {
    private UUID id;
    private String name;
}