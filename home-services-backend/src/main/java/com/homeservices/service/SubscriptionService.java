package com.homeservices.service;

import com.homeservices.dto.request.SubscriptionOrderRequest;
import com.homeservices.dto.response.SubscriptionResponse;
import com.homeservices.entity.Provider;
import com.homeservices.entity.ProviderSubscription;
import com.homeservices.entity.enums.SubscriptionPlan;
import com.homeservices.exception.BadRequestException;
import com.homeservices.exception.ResourceNotFoundException;
import com.homeservices.repository.BookingRepository;
import com.homeservices.repository.ProviderRepository;
import com.homeservices.repository.ProviderServiceRepository;
import com.homeservices.repository.ProviderSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

// TODO: Auto-generated Javadoc
/**
 * The Class SubscriptionService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

  /** The provider repository. */
  private final ProviderRepository providerRepository;
  
  /** The subscription repository. */
  private final ProviderSubscriptionRepository subscriptionRepository;
  
  /** The provider service repository. */
  private final ProviderServiceRepository providerServiceRepository;
  
  /** The booking repository. */
  private final BookingRepository bookingRepository;
  
  /** The platform settings service. */
  private final PlatformSettingsService platformSettingsService; // ← NEW

  /** The cashfree app id. */
  @Value("${cashfree.app-id}")
  private String cashfreeAppId;

  /** The cashfree secret key. */
  @Value("${cashfree.secret-key}")
  private String cashfreeSecretKey;

  /** The cashfree api url. */
  @Value("${cashfree.api-url:https://api.cashfree.com/pg}")
  private String cashfreeApiUrl;

  /** The frontend url. */
  @Value("${app.frontend-url:http://localhost:3000}")
  private String frontendUrl;

  // ─────────────────────────────────────────────────────────────────────────
  // PUBLIC API
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Returns the current subscription status for the provider. Also auto-deactivates the provider if
   * the free limit has been hit and they have no active subscription (idempotent — safe to call
   * repeatedly).
   *
   * @param userId the user id
   * @return the status
   */
  @Transactional
  public SubscriptionResponse getStatus(UUID userId) {
    Provider provider = getProvider(userId);
    int servicesUsed = (int) bookingRepository.countByProviderService_Provider_Id(provider.getId());
    int freeLimit = platformSettingsService.getFreeLimitLive();

    Optional<ProviderSubscription> activeSub =
        subscriptionRepository.findActiveByProviderId(provider.getId(), LocalDateTime.now());

    if (activeSub.isPresent()) {
      // Has a valid paid subscription → ensure provider is active
      ensureProviderActive(provider, "Subscription active");
      return toResponse(activeSub.get(), servicesUsed, freeLimit, false);
    }

    // No active subscription — check if free tier is exhausted
    if (servicesUsed >= freeLimit) {
      autoDeactivateProvider(provider,
          "Free tier limit of " + freeLimit + " bookings reached. Subscribe to reactivate.");
    }

    return SubscriptionResponse.builder().freeServiceLimit(freeLimit).servicesUsed(servicesUsed)
        .subscriptionRequired(servicesUsed >= freeLimit).paymentStatus("NONE").active(false)
        .build();
  }

  /**
   * Called after every new booking is recorded — checks if this booking pushed the provider over
   * the free limit and deactivates them if so. Call this from UserService#createBooking after
   * saving the booking.
   *
   * @param providerId the provider id
   */
  @Transactional
  public void checkAndEnforceFreeTierAfterBooking(UUID providerId) {
    Provider provider = providerRepository.findById(providerId).orElse(null);
    if (provider == null)
      return;

    int bookingsCount = (int) bookingRepository.countByProviderService_Provider_Id(providerId);
    int freeLimit = platformSettingsService.getFreeLimitLive();

    // Already has an active subscription — no action needed
    boolean subscribed =
        subscriptionRepository.findActiveByProviderId(providerId, LocalDateTime.now()).isPresent();
    if (subscribed)
      return;

    if (bookingsCount >= freeLimit) {
      autoDeactivateProvider(provider, "Free tier limit of " + freeLimit + " bookings reached.");
      log.info("Provider {} auto-deactivated after reaching free tier limit ({} bookings)",
          providerId, bookingsCount);
    }
  }

  /**
   * Returns full subscription history for the provider.
   *
   * @param userId the user id
   * @return the history
   */
  public List<SubscriptionResponse> getHistory(UUID userId) {
    Provider provider = getProvider(userId);
    int servicesUsed = (int) bookingRepository.countByProviderService_Provider_Id(provider.getId());
    int freeLimit = platformSettingsService.getFreeLimitLive();

    return subscriptionRepository.findByProviderIdOrderByCreatedAtDesc(provider.getId()).stream()
        .map(s -> toResponse(s, servicesUsed, freeLimit, false)).toList();
  }

  /**
   * Creates a Cashfree order and returns the paymentSessionId.
   *
   * @param userId the user id
   * @param req the req
   * @return the subscription response
   */
  @Transactional
  public SubscriptionResponse createOrder(UUID userId, SubscriptionOrderRequest req) {
    Provider provider = getProvider(userId);
    SubscriptionPlan plan = req.getPlan();

    String orderId =
        "SUB-" + provider.getId().toString().replace("-", "").substring(0, 12).toUpperCase() + "-"
            + System.currentTimeMillis();

    Map<String, Object> orderPayload = new LinkedHashMap<>();
    orderPayload.put("order_id", orderId);
    orderPayload.put("order_amount", plan.getPrice().doubleValue());
    orderPayload.put("order_currency", "INR");
    orderPayload.put("order_note", "ApnaAdmi subscription: " + plan.getLabel());

    Map<String, String> customerDetails = new LinkedHashMap<>();
    customerDetails.put("customer_id", provider.getUser().getId().toString());
    customerDetails.put("customer_name", provider.getUser().getName());
    customerDetails.put("customer_email", provider.getUser().getEmail());
    customerDetails.put("customer_phone", provider.getUser().getPhone());
    orderPayload.put("customer_details", customerDetails);

    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("return_url", frontendUrl + "/provider/subscription?order_id=" + orderId);
    meta.put("notify_url", frontendUrl.replace("localhost:3000", "your-backend.com")
        + "/provider/subscription/webhook");
    orderPayload.put("order_meta", meta);

    Map<String, Object> cfResponse = callCashfree("/orders", HttpMethod.POST, orderPayload);

    String paymentSessionId = (String) cfResponse.get("payment_session_id");
    if (paymentSessionId == null) {
      log.error("Cashfree order creation failed: {}", cfResponse);
      throw new BadRequestException("Payment gateway error. Please try again.");
    }

    LocalDateTime now = LocalDateTime.now();
    ProviderSubscription sub = ProviderSubscription.builder().provider(provider).plan(plan)
        .amountPaid(plan.getPrice()).cashfreeOrderId(orderId).paymentSessionId(paymentSessionId)
        .paymentStatus("CREATED").startsAt(now).expiresAt(plan.expiryFromNow()).build();

    subscriptionRepository.save(sub);

    int servicesUsed = (int) bookingRepository.countByProviderService_Provider_Id(provider.getId());
    int freeLimit = platformSettingsService.getFreeLimitLive();

    return toResponse(sub, servicesUsed, freeLimit, false);
  }

  /**
   * Called by the frontend after Cashfree checkout redirect to verify payment. On PAID →
   * auto-reactivates the provider.
   *
   * @param userId the user id
   * @param cashfreeOrderId the cashfree order id
   * @return the subscription response
   */
  @Transactional
  public SubscriptionResponse verifyPayment(UUID userId, String cashfreeOrderId) {
    ProviderSubscription sub = subscriptionRepository.findByCashfreeOrderId(cashfreeOrderId)
        .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + cashfreeOrderId));

    if (!sub.getProvider().getUser().getId().equals(userId)) {
      throw new BadRequestException("Order does not belong to this provider");
    }

    if ("PAID".equals(sub.getPaymentStatus())) {
      int used =
          (int) bookingRepository.countByProviderService_Provider_Id(sub.getProvider().getId());
      return toResponse(sub, used, platformSettingsService.getFreeLimitLive(), false);
    }

    try {
      Map<String, Object> cfOrder =
          callCashfree("/orders/" + cashfreeOrderId, HttpMethod.GET, null);
      String cfStatus = (String) cfOrder.get("order_status");

      if ("PAID".equalsIgnoreCase(cfStatus)) {
        activateSubAndProvider(sub); // ← reactivates provider too
      } else if ("EXPIRED".equalsIgnoreCase(cfStatus) || "CANCELLED".equalsIgnoreCase(cfStatus)) {
        sub.setPaymentStatus("FAILED");
        subscriptionRepository.save(sub);
      }
    } catch (Exception e) {
      log.warn("Cashfree verify call failed for order {}: {}", cashfreeOrderId, e.getMessage());
    }

    int used =
        (int) bookingRepository.countByProviderService_Provider_Id(sub.getProvider().getId());
    int freeLimit = platformSettingsService.getFreeLimitLive();
    return toResponse(sub, used, freeLimit, false);
  }

  /**
   * Cashfree webhook handler — called server-to-server. On PAID → marks subscription active AND
   * reactivates provider.
   *
   * @param payload the payload
   */
  @Transactional
  public void handleWebhook(Map<String, Object> payload) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> orderData = (Map<String, Object>) payload.get("data");
      if (orderData == null)
        return;

      @SuppressWarnings("unchecked")
      Map<String, Object> order = (Map<String, Object>) orderData.get("order");
      if (order == null)
        return;

      String orderId = (String) order.get("order_id");
      String status = (String) order.get("order_status");
      if (orderId == null)
        return;

      subscriptionRepository.findByCashfreeOrderId(orderId).ifPresent(sub -> {
        if ("PAID".equalsIgnoreCase(status)) {
          activateSubAndProvider(sub); // ← reactivates provider
        } else if ("EXPIRED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
          sub.setPaymentStatus("FAILED");
          subscriptionRepository.save(sub);
        }
      });

    } catch (Exception e) {
      log.error("Webhook processing error: {}", e.getMessage(), e);
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // PRIVATE HELPERS
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Marks subscription as PAID and re-activates the provider. Idempotent — safe to call multiple
   * times.
   *
   * @param sub the sub
   */
  private void activateSubAndProvider(ProviderSubscription sub) {
    if ("PAID".equals(sub.getPaymentStatus()))
      return;

    LocalDateTime now = LocalDateTime.now();
    sub.setPaymentStatus("PAID");
    sub.setStartsAt(now);
    sub.setExpiresAt(sub.getPlan().expiryFromNow());
    subscriptionRepository.save(sub);

    // ── Auto-reactivate the provider ──────────────────────────────────
    Provider provider = sub.getProvider();
    if (!provider.isActive()) {
      provider.setActive(true);
      providerRepository.save(provider);
      log.info("Provider {} reactivated after payment confirmed (plan={} expires={})",
          provider.getId(), sub.getPlan(), sub.getExpiresAt());
    }
  }

  /**
   * Deactivates provider if they have hit the free limit and have no subscription. Idempotent —
   * only writes to DB if provider is currently active.
   *
   * @param provider the provider
   * @param reason the reason
   */
  private void autoDeactivateProvider(Provider provider, String reason) {
    if (!provider.isActive())
      return; // already inactive — nothing to do
    provider.setActive(false);
    providerRepository.save(provider);
    log.info("Provider {} auto-deactivated. Reason: {}", provider.getId(), reason);
  }

  /**
   * Ensures provider is active (called when subscription is valid). Repairs state in case provider
   * was deactivated then manually resubscribed.
   *
   * @param provider the provider
   * @param reason the reason
   */
  private void ensureProviderActive(Provider provider, String reason) {
    if (provider.isActive())
      return;
    provider.setActive(true);
    providerRepository.save(provider);
    log.info("Provider {} reactivated. Reason: {}", provider.getId(), reason);
  }

  /**
   * Call cashfree.
   *
   * @param path the path
   * @param method the method
   * @param body the body
   * @return the map
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> callCashfree(String path, HttpMethod method,
      Map<String, Object> body) {
    RestTemplate rt = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("x-client-id", cashfreeAppId);
    headers.set("x-client-secret", cashfreeSecretKey);
    headers.set("x-api-version", "2023-08-01");

    HttpEntity<?> entity =
        (body != null) ? new HttpEntity<>(body, headers) : new HttpEntity<>(headers);

    ResponseEntity<Map> resp = rt.exchange(cashfreeApiUrl + path, method, entity, Map.class);

    return resp.getBody() != null ? resp.getBody() : Map.of();
  }

  /**
   * Gets the provider.
   *
   * @param userId the user id
   * @return the provider
   */
  private Provider getProvider(UUID userId) {
    return providerRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found"));
  }

  /**
   * To response.
   *
   * @param s the s
   * @param servicesUsed the services used
   * @param freeLimit the free limit
   * @param includeSession the include session
   * @return the subscription response
   */
  private SubscriptionResponse toResponse(ProviderSubscription s, int servicesUsed, int freeLimit,
      boolean includeSession) {
    return SubscriptionResponse.builder().id(s.getId()).plan(s.getPlan().name())
        .planLabel(s.getPlan().getLabel()).amountPaid(s.getAmountPaid())
        .paymentStatus(s.getPaymentStatus()).active(s.isActive()).startsAt(s.getStartsAt())
        .expiresAt(s.getExpiresAt()).createdAt(s.getCreatedAt())
        .cashfreeOrderId(s.getCashfreeOrderId()).paymentSessionId(s.getPaymentSessionId())
        .freeServiceLimit(freeLimit).servicesUsed(servicesUsed)
        .subscriptionRequired(!s.isActive() && servicesUsed >= freeLimit).build();
  }
}
