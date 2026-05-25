package com.homeservices.controller;

import com.homeservices.dto.request.SubscriptionOrderRequest;
import com.homeservices.dto.response.ApiResponse;
import com.homeservices.dto.response.SubscriptionResponse;
import com.homeservices.service.SubscriptionService;
import com.homeservices.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * All endpoints are under /provider/** which SecurityConfig already locks
 * to PROVIDER role — no extra security annotations needed.
 *
 * Webhook is public (Cashfree calls it server-to-server without a JWT).
 */
@RestController
@RequestMapping("/provider/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SecurityUtil        securityUtil;

    /** GET /provider/subscription/status — current freemium / subscription state */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getStatus() {
        SubscriptionResponse resp = subscriptionService.getStatus(securityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    /** GET /provider/subscription/history — all past subscriptions */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getHistory() {
        List<SubscriptionResponse> list = subscriptionService.getHistory(securityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * POST /provider/subscription/create-order
     * Body: { "plan": "MONTH" }
     * Returns: cashfreeOrderId + paymentSessionId for JS SDK
     */
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createOrder(
            @Valid @RequestBody SubscriptionOrderRequest request) {
        SubscriptionResponse resp =
                subscriptionService.createOrder(securityUtil.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(resp, "Order created. Complete payment to activate."));
    }

    /**
     * GET /provider/subscription/verify?order_id=SUB-XXXX
     * Called by the frontend after Cashfree redirects back.
     */
    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> verify(
            @RequestParam("order_id") String orderId) {
        SubscriptionResponse resp =
                subscriptionService.verifyPayment(securityUtil.getCurrentUserId(), orderId);
        String msg = "PAID".equals(resp.getPaymentStatus())
                ? "Payment confirmed! Subscription is now active."
                : "Payment pending. It may take a moment to confirm.";
        return ResponseEntity.ok(ApiResponse.success(resp, msg));
    }

    /**
     * POST /provider/subscription/webhook
     * Cashfree calls this server-to-server. Must be PUBLIC (no JWT).
     * We handle security via Cashfree's webhook signature (see SubscriptionService).
     *
     * Add this URL to your SecurityConfig permitAll() list:
     *   .requestMatchers("/provider/subscription/webhook").permitAll()
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody Map<String, Object> payload) {
        subscriptionService.handleWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
