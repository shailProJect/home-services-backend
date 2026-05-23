package com.homeservices.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReviewResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private UUID providerId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
