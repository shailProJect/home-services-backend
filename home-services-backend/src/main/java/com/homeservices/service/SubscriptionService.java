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

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

  private final ProviderRepository providerRepository;
  private final ProviderSubscriptionRepository subscriptionRepository;
  private final ProviderServiceRepository providerServiceRepository;
  private final BookingRepository bookingRepository;
  private final PlatformSettingsService platformSettingsService;

  @Value("${cashfree.app-id}")
  private String cashfreeAppId;

  @Value("${cashfree.secret-key}")
  private String cashfreeSecretKey;

  @Value("${cashfree.api-url:https://api.cashfree.com/pg}")
  private String cashfreeApiUrl;

  @Value("${app.frontend-url:http://localhost:3000}")
  private String frontendUrl;

  // ─────────────────────────────────────────────────────────────────────────
  // PUBLIC API
  // ─────────────────────────────────────────────────────────────────────────

  @Transactional
  public SubscriptionResponse getStatus(UUID userId) {
    Provider provider = getProvider(userId);
    int servicesUsed = (int) bookingRepository.countByProviderService_Provider_Id(provider.getId());
    int freeLimit = platformSettingsService.getFreeLimitLive();

    Optional<ProviderSubscription> activeSub =
        subscriptionRepository.findActiveByProviderId(provider.getId(), LocalDateTime.now());

    if (activeSub.isPresent()) {
      ensureProviderActive(provider, "Subscription active");
      return toResponse(activeSub.get(), servicesUsed, freeLimit);
    }

    if (servicesUsed >= freeLimit) {
      autoDeactivateProvider(provider,
          "Free tier limit of " + freeLimit + " bookings reached. Subscribe to reactivate.");
    }

    return SubscriptionResponse.builder().freeServiceLimit(freeLimit).servicesUsed(servicesUsed)
        .subscriptionRequired(servicesUsed >= freeLimit).paymentStatus("NONE").active(false)
        .build();
  }

  @Transactional
  public void checkAndEnforceFreeTierAfterBooking(UUID providerId) {
    Provider provider = providerRepository.findById(providerId).orElse(null);
    if (provider == null)
      return;

    int bookingsCount = (int) bookingRepository.countByProviderService_Provider_Id(providerId);
    int freeLimit = platformSettingsService.getFreeLimitLive();

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

  public List<SubscriptionResponse> getHistory(UUID userId) {
    Provider provider = getProvider(userId);
    int servicesUsed = (int) bookingRepository.countByProviderService_Provider_Id(provider.getId());
    int freeLimit = platformSettingsService.getFreeLimitLive();

    return subscriptionRepository.findByProviderIdOrderByCreatedAtDesc(provider.getId()).stream()
        .map(s -> toResponse(s, servicesUsed, freeLimit)).toList();
  }

  /**
   * Creates a Cashfree order.
   *
   * KEY CHANGE: price is now read from PlatformSettings (admin-configured) instead of the hardcoded
   * value in the SubscriptionPlan enum. The enum price is kept as a fallback default only.
   */
  @Transactional
  public SubscriptionResponse createOrder(UUID userId, SubscriptionOrderRequest req) {
    Provider provider = getProvider(userId);
    SubscriptionPlan plan = req.getPlan();

    // ── Read the LIVE admin-configured price ──────────────────────────────
    BigDecimal livePrice = platformSettingsService.getLivePriceForPlan(plan);

    String orderId =
        "SUB-" + provider.getId().toString().replace("-", "").substring(0, 12).toUpperCase() + "-"
            + System.currentTimeMillis();

    // ── Build Cashfree order payload ──────────────────────────────────────
    Map<String, Object> orderPayload = new LinkedHashMap<>();
    orderPayload.put("order_id", orderId);
    orderPayload.put("order_amount", livePrice.doubleValue()); // ← live price
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

    // ── Persist subscription record with the LIVE price ───────────────────
    LocalDateTime now = LocalDateTime.now();
    ProviderSubscription sub =
        ProviderSubscription.builder().provider(provider).plan(plan).amountPaid(livePrice) // ← live
                                                                                           // price
                                                                                           // recorded
                                                                                           // at
                                                                                           // purchase
                                                                                           // time
            .cashfreeOrderId(orderId).paymentSessionId(paymentSessionId).paymentStatus("CREATED")
            .startsAt(now).expiresAt(plan.expiryFromNow()).build();

    subscriptionRepository.save(sub);

    int servicesUsed = (int) bookingRepository.countByProviderService_Provider_Id(provider.getId());
    int freeLimit = platformSettingsService.getFreeLimitLive();

    return toResponse(sub, servicesUsed, freeLimit);
  }

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
      return toResponse(sub, used, platformSettingsService.getFreeLimitLive());
    }

    try {
      Map<String, Object> cfOrder =
          callCashfree("/orders/" + cashfreeOrderId, HttpMethod.GET, null);
      String cfStatus = (String) cfOrder.get("order_status");

      if ("PAID".equalsIgnoreCase(cfStatus)) {
        activateSubAndProvider(sub);
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
    return toResponse(sub, used, freeLimit);
  }

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
          activateSubAndProvider(sub);
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

  private void activateSubAndProvider(ProviderSubscription sub) {
    if ("PAID".equals(sub.getPaymentStatus()))
      return;

    sub.setPaymentStatus("PAID");
    sub.setStartsAt(LocalDateTime.now());
    sub.setExpiresAt(sub.getPlan().expiryFromNow());
    subscriptionRepository.save(sub);

    Provider provider = sub.getProvider();
    if (!provider.isActive()) {
      provider.setActive(true);
      providerRepository.save(provider);
      log.info("Provider {} reactivated after payment confirmed (plan={} expires={})",
          provider.getId(), sub.getPlan(), sub.getExpiresAt());
    }
  }

  private void autoDeactivateProvider(Provider provider, String reason) {
    if (!provider.isActive())
      return;
    provider.setActive(false);
    providerRepository.save(provider);
    log.info("Provider {} auto-deactivated. Reason: {}", provider.getId(), reason);
  }

  private void ensureProviderActive(Provider provider, String reason) {
    if (provider.isActive())
      return;
    provider.setActive(true);
    providerRepository.save(provider);
    log.info("Provider {} reactivated. Reason: {}", provider.getId(), reason);
  }

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

  private Provider getProvider(UUID userId) {
    return providerRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found"));
  }

  private SubscriptionResponse toResponse(ProviderSubscription s, int servicesUsed, int freeLimit) {
    return SubscriptionResponse.builder().id(s.getId()).plan(s.getPlan().name())
        .planLabel(s.getPlan().getLabel()).amountPaid(s.getAmountPaid())
        .paymentStatus(s.getPaymentStatus()).active(s.isActive()).startsAt(s.getStartsAt())
        .expiresAt(s.getExpiresAt()).createdAt(s.getCreatedAt())
        .cashfreeOrderId(s.getCashfreeOrderId()).paymentSessionId(s.getPaymentSessionId())
        .freeServiceLimit(freeLimit).servicesUsed(servicesUsed)
        .subscriptionRequired(!s.isActive() && servicesUsed >= freeLimit).build();
  }
}
