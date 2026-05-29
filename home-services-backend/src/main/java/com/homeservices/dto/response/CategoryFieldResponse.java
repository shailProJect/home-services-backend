package com.homeservices.dto.response;

import com.homeservices.entity.CategoryField.FieldType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CategoryFieldResponse {
  private UUID id;
  private String fieldLabel;
  private String fieldKey;
  private FieldType fieldType;
  private String options;
  private String placeholder;
  private boolean required;
  private int sortOrder;
}
