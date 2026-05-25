package com.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Singleton table — always has exactly ONE row (id = 1). Stores admin-configurable platform
 * settings that override compiled constants.
 *
 * Usage: - FREE_SERVICE_LIMIT in ProviderSubscription.java is now the *default* fallback. - At
 * runtime, SubscriptionService reads the limit from this table instead.
 */
@Entity
@Table(name = "platform_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformSettings {

  /** Always 1 — singleton row */
  @Id
  private Long id;

  /**
   * How many free bookings a provider gets before needing a subscription. Default: 10. Admin can
   * raise or lower this at any time.
   */
  @Column(nullable = false)
  @Builder.Default
  private int freeServiceLimit = 10;

  /**
   * Optional promotional offer message shown on the provider subscription page. Example: "🎉
   * Special offer: Get 1 Month FREE with annual plan!" Set to null / empty to hide the banner.
   */
  @Column(columnDefinition = "TEXT")
  private String offerBannerText;

  /**
   * Optional discount percentage (0–100) applied to all plans while the offer is active. 0 means no
   * discount.
   */
  @Column(nullable = false)
  @Builder.Default
  private int offerDiscountPercent = 0;

  /**
   * When the current offer expires. Null = no expiry / no offer.
   */
  private LocalDateTime offerExpiresAt;

  /** Who last changed the settings */
  @Column(length = 100)
  private String lastUpdatedBy;

  @UpdateTimestamp
  private LocalDateTime updatedAt;
}
