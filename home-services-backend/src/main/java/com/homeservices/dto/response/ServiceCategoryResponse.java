package com.homeservices.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * UPDATED — now includes displayName, description, icon, active, timestamps, and the list of
 * dynamic fields for this category.
 */
@Data
@Builder
public class ServiceCategoryResponse {
  private UUID id;
  private String name; // internal key, e.g. "GARBAGE_COLLECTOR"
  private String displayName; // pretty label, e.g. "Garbage Collector"
  private String description;
  private String icon;
  private boolean active;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /** Dynamic registration fields for this category */
  private List<CategoryFieldResponse> fields;
}
