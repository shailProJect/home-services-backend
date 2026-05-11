package com.homeservices.dto.response;

import com.homeservices.entity.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
  private UUID id;
  private String name;
  private String email;
  private String phone;
  private Role role;
  private boolean enabled;
  private LocalDateTime createdAt;
  private boolean phoneVerified;
  private boolean emailVerified;
  private String address;
}
