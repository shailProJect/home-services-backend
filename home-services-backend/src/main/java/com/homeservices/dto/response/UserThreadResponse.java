package com.homeservices.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserThreadResponse {
    private String userId;
    private String userName;
    private String userEmail;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
}
