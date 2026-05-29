package com.homeservices.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryRequest {

  /** Unique internal key, auto-uppercased, e.g. "GARBAGE_COLLECTOR" */
  @NotBlank
  private String name;

  /** Pretty label shown in the UI, e.g. "Garbage Collector" */
  @NotBlank
  private String displayName;

  private String description;

  /** Emoji or icon name, e.g. "🗑️" */
  private String icon;

  private boolean active = true;

  @Valid
  private List<CategoryFieldRequest> fields = new ArrayList<>();
}
