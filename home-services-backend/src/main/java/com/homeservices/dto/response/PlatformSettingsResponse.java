package com.homeservices.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlatformSettingsResponse {

    // ── Free tier ─────────────────────────────────────────────────────────────
    private int freeServiceLimit;

    // ── Plan prices ───────────────────────────────────────────────────────────
    private BigDecimal priceDayPlan;
    private BigDecimal priceWeekPlan;
    private BigDecimal priceMonthPlan;
    private BigDecimal priceYearPlan;

    // ── Offer ─────────────────────────────────────────────────────────────────
    private String  offerBannerText;
    private int     offerDiscountPercent;
    private String  offerExpiresAt;      // ISO string, nullable
    private boolean offerActive;         // convenience flag for frontend

    // ── Audit ─────────────────────────────────────────────────────────────────
    private String  lastUpdatedBy;
    private String  updatedAt;           // ISO string
}