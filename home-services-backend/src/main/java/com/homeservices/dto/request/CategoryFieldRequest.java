package com.homeservices.dto.request;

import com.homeservices.entity.CategoryField.FieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryFieldRequest {

  @NotBlank
  private String fieldLabel;

  @NotBlank
  private String fieldKey;

  @NotNull
  private FieldType fieldType;

  /** Comma-separated list — only used when fieldType == SELECT */
  private String options;

  private String placeholder;

  private boolean required = false;

  private int sortOrder = 0;
}
