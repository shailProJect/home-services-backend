package com.homeservices.repository;

import com.homeservices.entity.ProviderSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderSubscriptionRepository extends JpaRepository<ProviderSubscription, UUID> {

    /** Returns the single active (PAID + not-expired) subscription for this provider, if any. */
    @Query("""
            SELECT s FROM ProviderSubscription s
            WHERE s.provider.id = :providerId
              AND s.paymentStatus = 'PAID'
              AND s.expiresAt > :now
            ORDER BY s.expiresAt DESC
            """)
    Optional<ProviderSubscription> findActiveByProviderId(
            @Param("providerId") UUID providerId,
            @Param("now") LocalDateTime now);

    /** Find by Cashfree order id (used in webhook + verify). */
    Optional<ProviderSubscription> findByCashfreeOrderId(String cashfreeOrderId);

    /** All subscriptions for a provider, newest first. */
    List<ProviderSubscription> findByProviderIdOrderByCreatedAtDesc(UUID providerId);
}
