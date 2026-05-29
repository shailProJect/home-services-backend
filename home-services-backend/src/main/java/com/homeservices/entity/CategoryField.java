package com.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * A dynamic field that belongs to a ServiceCategory.
 *
 * Example: for "GARBAGE_COLLECTOR" category, admin may add: - fieldLabel: "Waste Type", fieldType:
 * SELECT, options: "General,Organic,Recyclable" - fieldLabel: "Vehicle No", fieldType: TEXT,
 * required: true - fieldLabel: "Has Uniform", fieldType: CHECKBOX, required: false
 *
 * These fields are rendered dynamically on the provider registration form and stored as
 * ProviderCategoryFieldValue entries.
 */
@Entity
@Table(name = "category_fields")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryField {

  public enum FieldType {
    TEXT, // plain text input
    NUMBER, // numeric input
    SELECT, // dropdown with options
    CHECKBOX, // boolean toggle
    TEXTAREA, // multi-line text
    FILE // document/image upload
  }

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id", nullable = false)
  private ServiceCategory category;

  /** Label shown to the provider, e.g. "Waste Type" */
  @Column(nullable = false)
  private String fieldLabel;

  /** Internal key used when saving values, e.g. "waste_type" */
  @Column(nullable = false)
  private String fieldKey;

  /** Input type */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private FieldType fieldType = FieldType.TEXT;

  /** Comma-separated options for SELECT fields, e.g. "General,Organic,Recyclable" */
  @Column(columnDefinition = "TEXT")
  private String options;

  /** Placeholder / hint text */
  private String placeholder;

  /** Whether the field must be filled before registration */
  @Column(nullable = false)
  @Builder.Default
  private boolean required = false;

  /** Display order within the form */
  @Column(nullable = false)
  @Builder.Default
  private int sortOrder = 0;
}
