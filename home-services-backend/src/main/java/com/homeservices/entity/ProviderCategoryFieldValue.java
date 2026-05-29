package com.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Stores the value a provider submitted for a dynamic CategoryField during registration or profile
 * update.
 *
 * Example row: provider_id = <uuid> field_id = <uuid of "Waste Type" field> value = "Organic"
 */
@Entity
@Table(name = "provider_category_field_values",
    uniqueConstraints = @UniqueConstraint(columnNames = {"provider_id", "field_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderCategoryFieldValue {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_id", nullable = false)
  private Provider provider;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "field_id", nullable = false)
  private CategoryField field;

  @Column(columnDefinition = "TEXT")
  private String value;
}
