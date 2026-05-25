package com.homeservices.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlatformSettingsRequest {

    // ── Free tier ─────────────────────────────────────────────────────────────

    @Min(value = 0, message = "Free limit must be ≥ 0")
    @Max(value = 10000, message = "Free limit seems unreasonably high")
    private int freeServiceLimit;

    // ── Plan prices ───────────────────────────────────────────────────────────

    @NotNull(message = "Day plan price is required")
    @DecimalMin(value = "1", message = "Price must be at least ₹1")
    @DecimalMax(value = "99999", message = "Price seems unreasonably high")
    @Digits(integer = 7, fraction = 2)
    private BigDecimal priceDayPlan;

    @NotNull(message = "Week plan price is required")
    @DecimalMin(value = "1")
    @DecimalMax(value = "99999")
    @Digits(integer = 7, fraction = 2)
    private BigDecimal priceWeekPlan;

    @NotNull(message = "Month plan price is required")
    @DecimalMin(value = "1")
    @DecimalMax(value = "99999")
    @Digits(integer = 7, fraction = 2)
    private BigDecimal priceMonthPlan;

    @NotNull(message = "Year plan price is required")
    @DecimalMin(value = "1")
    @DecimalMax(value = "99999")
    @Digits(integer = 7, fraction = 2)
    private BigDecimal priceYearPlan;

    // ── Offer ─────────────────────────────────────────────────────────────────

    /** Nullable — send null or empty string to clear the banner */
    private String offerBannerText;

    @Min(0) @Max(100)
    private int offerDiscountPercent;

    /** ISO-8601 string or null. E.g. "2025-12-31T23:59:59" */
    private String offerExpiresAt;
}