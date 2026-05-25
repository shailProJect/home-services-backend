package com.homeservices.entity;

import com.homeservices.entity.enums.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks each paid subscription for a provider.
 * A provider is considered "subscribed" when:
 *   - at least one row exists for them
 *   - AND that row's expiresAt > now()
 *   - AND paymentStatus = 'PAID'
 *
 * Free tier: provider may add up to FREE_SERVICE_LIMIT services without a subscription.
 */
@Entity
@Table(name = "provider_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderSubscription {

    public static final int FREE_SERVICE_LIMIT = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan plan;

    /** Amount paid in INR (copied from plan at purchase time) */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;

    /** Cashfree order_id returned when order is created */
    @Column(unique = true)
    private String cashfreeOrderId;

    /** Cashfree payment_session_id used to launch the JS checkout SDK */
    @Column(columnDefinition = "TEXT")
    private String paymentSessionId;

    /**
     * CREATED  – order created, awaiting payment
     * PAID     – webhook confirmed / verify API confirmed payment
     * FAILED   – payment failed
     * EXPIRED  – plan duration elapsed (cron job may flip this, or we just check expiresAt)
     */
    @Column(nullable = false)
    @Builder.Default
    private String paymentStatus = "CREATED";

    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ── helpers ──────────────────────────────────────────────────────────────

    public boolean isActive() {
        return "PAID".equals(paymentStatus) && expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
    }
}
