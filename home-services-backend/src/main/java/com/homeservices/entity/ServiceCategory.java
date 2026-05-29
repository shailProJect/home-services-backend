package com.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "service_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCategory {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(name = "display_name", nullable = false)
  @Builder.Default
  private String displayName = "";

  @Column(columnDefinition = "TEXT")
  private String description;

  private String icon;

  @Column(nullable = false)
  @Builder.Default
  private boolean active = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true,
      fetch = FetchType.LAZY)
  @OrderBy("sortOrder ASC")
  @Builder.Default
  private List<CategoryField> fields = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    LocalDateTime now = LocalDateTime.now(); 
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
