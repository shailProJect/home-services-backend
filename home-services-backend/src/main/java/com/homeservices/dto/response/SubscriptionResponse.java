package com.homeservices.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionResponse {
    private UUID   id;
    private String plan;           // DAY / WEEK / MONTH / YEAR
    private String planLabel;      // "1 Day", "1 Week" …
    private BigDecimal amountPaid;
    private String paymentStatus;  // CREATED / PAID / FAILED
    private boolean active;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    // Returned only after order creation — used by frontend Cashfree SDK
    private String cashfreeOrderId;
    private String paymentSessionId;

    // Subscription status info for the provider dashboard
    private int  freeServiceLimit;    // always 10
    private int  servicesUsed;        // how many active services exist
    private boolean subscriptionRequired; // true when servicesUsed >= limit AND no active sub
}
