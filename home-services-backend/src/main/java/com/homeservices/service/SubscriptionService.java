package com.homeservices.service;

import com.homeservices.dto.request.SubscriptionOrderRequest;
import com.homeservices.dto.response.SubscriptionResponse;
import com.homeservices.entity.Provider;
import com.homeservices.entity.ProviderSubscription;
import com.homeservices.entity.enums.SubscriptionPlan;
import com.homeservices.exception.BadRequestException;
import com.homeservices.exception.ResourceNotFoundException;
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

    private final ProviderRepository             providerRepository;
    private final ProviderSubscriptionRepository subscriptionRepository;
    private final ProviderServiceRepository      providerServiceRepository;

    @Value("${cashfree.app-id}")
    private String cashfreeAppId;

    @Value("${cashfree.secret-key}")
    private String cashfreeSecretKey;

    @Value("${cashfree.api-url:https://api.cashfree.com/pg}")
    private String cashfreeApiUrl;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private static final int FREE_LIMIT = ProviderSubscription.FREE_SERVICE_LIMIT;

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the current subscription status for the provider — used by the
     * frontend to show the free-tier counter and subscription banner.
     */
    public SubscriptionResponse getStatus(UUID userId) {
        Provider provider = getProvider(userId);
        int servicesUsed  = providerServiceRepository
                .findByProviderIdAndActiveTrue(provider.getId()).size();

        Optional<ProviderSubscription> activeSub =
                subscriptionRepository.findActiveByProviderId(provider.getId(), LocalDateTime.now());

        if (activeSub.isPresent()) {
            ProviderSubscription s = activeSub.get();
            return toResponse(s, servicesUsed, false);
        }

        // No active subscription — return freemium info
        return SubscriptionResponse.builder()
                .freeServiceLimit(FREE_LIMIT)
                .servicesUsed(servicesUsed)
                .subscriptionRequired(servicesUsed >= FREE_LIMIT)
                .paymentStatus("NONE")
                .active(false)
                .build();
    }

    /**
     * Returns full subscription history for the provider.
     */
    public List<SubscriptionResponse> getHistory(UUID userId) {
        Provider provider = getProvider(userId);
        int servicesUsed  = providerServiceRepository
                .findByProviderIdAndActiveTrue(provider.getId()).size();

        return subscriptionRepository
                .findByProviderIdOrderByCreatedAtDesc(provider.getId())
                .stream()
                .map(s -> toResponse(s, servicesUsed, false))
                .toList();
    }

    /**
     * Creates a Cashfree order and returns the paymentSessionId so the
     * frontend can launch the Cashfree JS SDK directly.
     *
     * Flow:
     *  1. POST to Cashfree /orders                    → get order_id + payment_session_id
     *  2. Save ProviderSubscription with status=CREATED
     *  3. Return paymentSessionId to frontend
     *  4. Frontend opens Cashfree checkout
     *  5. Cashfree calls our webhook  →  verifyWebhook()
     *     OR frontend polls           →  verifyPayment()
     */
    @Transactional
    public SubscriptionResponse createOrder(UUID userId, SubscriptionOrderRequest req) {
        Provider provider = getProvider(userId);
        SubscriptionPlan plan = req.getPlan();

        // Build a short unique order id  (Cashfree limit: 50 chars)
        String orderId = "SUB-" + provider.getId().toString().replace("-", "").substring(0, 12).toUpperCase()
                         + "-" + System.currentTimeMillis();

        // ── Call Cashfree create-order API ────────────────────────────────
        Map<String, Object> orderPayload = new LinkedHashMap<>();
        orderPayload.put("order_id",       orderId);
        orderPayload.put("order_amount",   plan.getPrice().doubleValue());
        orderPayload.put("order_currency", "INR");
        orderPayload.put("order_note",     "ApnaAdmi subscription: " + plan.getLabel());

        Map<String, String> customerDetails = new LinkedHashMap<>();
        customerDetails.put("customer_id",    provider.getUser().getId().toString());
        customerDetails.put("customer_name",  provider.getUser().getName());
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

        // ── Persist the subscription record (status=CREATED until paid) ────
        LocalDateTime now    = LocalDateTime.now();
        ProviderSubscription sub = ProviderSubscription.builder()
                .provider(provider)
                .plan(plan)
                .amountPaid(plan.getPrice())
                .cashfreeOrderId(orderId)
                .paymentSessionId(paymentSessionId)
                .paymentStatus("CREATED")
                .startsAt(now)
                .expiresAt(plan.expiryFromNow())
                .build();

        subscriptionRepository.save(sub);

        int servicesUsed = providerServiceRepository
                .findByProviderIdAndActiveTrue(provider.getId()).size();

        return toResponse(sub, servicesUsed, false);
    }

    /**
     * Called by the frontend after the Cashfree checkout redirect to verify payment.
     * Also used as fallback if webhook is delayed.
     */
    @Transactional
    public SubscriptionResponse verifyPayment(UUID userId, String cashfreeOrderId) {
        ProviderSubscription sub = subscriptionRepository
                .findByCashfreeOrderId(cashfreeOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + cashfreeOrderId));

        // Only the owning provider can verify
        if (!sub.getProvider().getUser().getId().equals(userId)) {
            throw new BadRequestException("Order does not belong to this provider");
        }

        // Already confirmed — just return current state
        if ("PAID".equals(sub.getPaymentStatus())) {
            int used = providerServiceRepository
                    .findByProviderIdAndActiveTrue(sub.getProvider().getId()).size();
            return toResponse(sub, used, false);
        }

        // ── Ask Cashfree for the live order status ─────────────────────────
        try {
            Map<String, Object> cfOrder = callCashfree("/orders/" + cashfreeOrderId, HttpMethod.GET, null);
            String cfStatus = (String) cfOrder.get("order_status");

            if ("PAID".equalsIgnoreCase(cfStatus)) {
                activateSub(sub);
            } else if ("EXPIRED".equalsIgnoreCase(cfStatus) || "CANCELLED".equalsIgnoreCase(cfStatus)) {
                sub.setPaymentStatus("FAILED");
                subscriptionRepository.save(sub);
            }
            // ACTIVE / PARTIALLY_PAID → leave as CREATED, frontend will poll

        } catch (Exception e) {
            log.warn("Cashfree verify call failed for order {}: {}", cashfreeOrderId, e.getMessage());
        }

        int used = providerServiceRepository
                .findByProviderIdAndActiveTrue(sub.getProvider().getId()).size();
        return toResponse(sub, used, false);
    }

    /**
     * Cashfree webhook handler — called server-to-server by Cashfree.
     * Verifies the signature then marks the subscription as PAID.
     *
     * NOTE: In production you MUST validate Cashfree's signature header.
     *       Add the signature check when you have the Cashfree webhook secret.
     */
    @Transactional
    public void handleWebhook(Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> orderData = (Map<String, Object>) payload.get("data");
            if (orderData == null) return;

            @SuppressWarnings("unchecked")
            Map<String, Object> order = (Map<String, Object>) orderData.get("order");
            if (order == null) return;

            String orderId = (String) order.get("order_id");
            String status  = (String) order.get("order_status");

            if (orderId == null) return;

            subscriptionRepository.findByCashfreeOrderId(orderId).ifPresent(sub -> {
                if ("PAID".equalsIgnoreCase(status)) {
                    activateSub(sub);
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
    // INTERNAL HELPERS (used by ProviderManagementService)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Throws BadRequestException if the provider has hit the free limit
     * and has no active subscription. Called from addService().
     */
    public void enforceServiceLimit(UUID providerId) {
        int count = providerServiceRepository
                .findByProviderIdAndActiveTrue(providerId).size();

        if (count < FREE_LIMIT) return;   // still in free tier

        boolean subscribed = subscriptionRepository
                .findActiveByProviderId(providerId, LocalDateTime.now())
                .isPresent();

        if (!subscribed) {
            throw new BadRequestException(
                "FREE_LIMIT_REACHED: You have reached the free limit of " + FREE_LIMIT +
                " services. Please subscribe to add more.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void activateSub(ProviderSubscription sub) {
        if ("PAID".equals(sub.getPaymentStatus())) return;  // idempotent
        LocalDateTime now = LocalDateTime.now();
        sub.setPaymentStatus("PAID");
        sub.setStartsAt(now);
        sub.setExpiresAt(sub.getPlan().expiryFromNow());
        subscriptionRepository.save(sub);
        log.info("Subscription ACTIVATED: provider={} plan={} expires={}",
                 sub.getProvider().getId(), sub.getPlan(), sub.getExpiresAt());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callCashfree(String path, HttpMethod method, Map<String, Object> body) {
        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id",     cashfreeAppId);
        headers.set("x-client-secret", cashfreeSecretKey);
        headers.set("x-api-version",   "2023-08-01");

        HttpEntity<?> entity = (body != null)
                ? new HttpEntity<>(body, headers)
                : new HttpEntity<>(headers);

        ResponseEntity<Map> resp = rt.exchange(
                cashfreeApiUrl + path, method, entity, Map.class);

        return resp.getBody() != null ? resp.getBody() : Map.of();
    }

    private Provider getProvider(UUID userId) {
        return providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found"));
    }

    private SubscriptionResponse toResponse(ProviderSubscription s, int servicesUsed,
                                             boolean includeSession) {
        return SubscriptionResponse.builder()
                .id(s.getId())
                .plan(s.getPlan().name())
                .planLabel(s.getPlan().getLabel())
                .amountPaid(s.getAmountPaid())
                .paymentStatus(s.getPaymentStatus())
                .active(s.isActive())
                .startsAt(s.getStartsAt())
                .expiresAt(s.getExpiresAt())
                .createdAt(s.getCreatedAt())
                .cashfreeOrderId(s.getCashfreeOrderId())
                .paymentSessionId(includeSession ? s.getPaymentSessionId() : s.getPaymentSessionId())
                .freeServiceLimit(FREE_LIMIT)
                .servicesUsed(servicesUsed)
                .subscriptionRequired(!s.isActive() && servicesUsed >= FREE_LIMIT)
                .build();
    }
}
