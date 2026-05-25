package com.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Singleton table — always has exactly ONE row (id = 1).
 * Stores all admin-configurable platform settings:
 *   - free tier booking limit
 *   - promotional offer banner + discount
 *   - subscription plan prices (admin can override defaults at any time)
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

    // ── Free tier ────────────────────────────────────────────────────────────

    @Column(nullable = false)
    @Builder.Default
    private int freeServiceLimit = 10;

    // ── Subscription plan prices (INR) ───────────────────────────────────────
    // Defaults mirror the hardcoded values in SubscriptionPlan enum.
    // Admin can change these at any time; SubscriptionService reads these live.

    /** Price for the DAY plan (default ₹99) */
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal priceDayPlan = BigDecimal.valueOf(99);

    /** Price for the WEEK plan (default ₹299) */
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal priceWeekPlan = BigDecimal.valueOf(299);

    /** Price for the MONTH plan (default ₹999) */
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal priceMonthPlan = BigDecimal.valueOf(999);

    /** Price for the YEAR plan (default ₹9999) */
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal priceYearPlan = BigDecimal.valueOf(9999);

    // ── Promotional offer ────────────────────────────────────────────────────

    @Column(columnDefinition = "TEXT")
    private String offerBannerText;

    @Column(nullable = false)
    @Builder.Default
    private int offerDiscountPercent = 0;

    private LocalDateTime offerExpiresAt;

    // ── Audit ────────────────────────────────────────────────────────────────

    @Column(length = 100)
    private String lastUpdatedBy;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}