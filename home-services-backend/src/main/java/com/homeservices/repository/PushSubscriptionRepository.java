package com.homeservices.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.homeservices.entity.PushSubscription;

public interface PushSubscriptionRepository
extends JpaRepository<PushSubscription, UUID> {

List<PushSubscription> findByUserId(UUID userId);

List<PushSubscription> findByProviderId(UUID providerId);
}
