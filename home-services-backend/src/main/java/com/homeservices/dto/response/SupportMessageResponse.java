package com.homeservices.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SupportMessageResponse {
    private String id;
    private String content;
    private String senderRole;   // "USER" | "ADMIN"
    private LocalDateTime createdAt;
    private boolean read;
}
