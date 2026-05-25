package com.homeservices.service;

import com.homeservices.dto.request.PlatformSettingsRequest;
import com.homeservices.dto.response.PlatformSettingsResponse;
import com.homeservices.entity.PlatformSettings;
import com.homeservices.entity.enums.SubscriptionPlan;
import com.homeservices.repository.PlatformSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PlatformSettingsService {

    private static final long SETTINGS_ID = 1L;
    private final PlatformSettingsRepository repo;

    // ─── Public API ───────────────────────────────────────────────────────────

    /** Full settings DTO — used by GET /admin/settings and provider subscription page. */
    public PlatformSettingsResponse getSettings() {
        return toResponse(getOrCreate());
    }

    /** Admin saves new settings (all fields at once). */
    @Transactional
    public PlatformSettingsResponse updateSettings(PlatformSettingsRequest req, String updatedBy) {
        PlatformSettings s = getOrCreate();

        // Free tier
        s.setFreeServiceLimit(req.getFreeServiceLimit());

        // Plan prices
        s.setPriceDayPlan(req.getPriceDayPlan());
        s.setPriceWeekPlan(req.getPriceWeekPlan());
        s.setPriceMonthPlan(req.getPriceMonthPlan());
        s.setPriceYearPlan(req.getPriceYearPlan());

        // Offer
        s.setOfferBannerText(blankToNull(req.getOfferBannerText()));
        s.setOfferDiscountPercent(req.getOfferDiscountPercent());
        s.setOfferExpiresAt(parseDate(req.getOfferExpiresAt()));

        s.setLastUpdatedBy(updatedBy);
        return toResponse(repo.save(s));
    }

    // ─── Convenience helpers used by SubscriptionService ─────────────────────

    /** Live free-tier limit (replaces hardcoded FREE_LIMIT constant). */
    public int getFreeLimitLive() {
        return getOrCreate().getFreeServiceLimit();
    }

    /**
     * Returns the admin-configured price for a given plan.
     * Falls back to the enum's compiled default if no DB row exists yet.
     *
     * Used in SubscriptionService.createOrder() so the checkout amount
     * always reflects the price the admin last set.
     */
    public BigDecimal getLivePriceForPlan(SubscriptionPlan plan) {
        PlatformSettings s = getOrCreate();
        return switch (plan) {
            case DAY   -> s.getPriceDayPlan();
            case WEEK  -> s.getPriceWeekPlan();
            case MONTH -> s.getPriceMonthPlan();
            case YEAR  -> s.getPriceYearPlan();
        };
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private PlatformSettings getOrCreate() {
        return repo.findById(SETTINGS_ID).orElseGet(() -> {
            PlatformSettings defaults = PlatformSettings.builder()
                    .id(SETTINGS_ID)
                    .freeServiceLimit(10)
                    .priceDayPlan(BigDecimal.valueOf(99))
                    .priceWeekPlan(BigDecimal.valueOf(299))
                    .priceMonthPlan(BigDecimal.valueOf(999))
                    .priceYearPlan(BigDecimal.valueOf(9999))
                    .offerDiscountPercent(0)
                    .build();
            return repo.save(defaults);
        });
    }

    private PlatformSettingsResponse toResponse(PlatformSettings s) {
        boolean offerActive = s.getOfferBannerText() != null
                && !s.getOfferBannerText().isBlank()
                && (s.getOfferExpiresAt() == null || s.getOfferExpiresAt().isAfter(LocalDateTime.now()));

        return PlatformSettingsResponse.builder()
                .freeServiceLimit(s.getFreeServiceLimit())
                .priceDayPlan(s.getPriceDayPlan())
                .priceWeekPlan(s.getPriceWeekPlan())
                .priceMonthPlan(s.getPriceMonthPlan())
                .priceYearPlan(s.getPriceYearPlan())
                .offerBannerText(s.getOfferBannerText())
                .offerDiscountPercent(s.getOfferDiscountPercent())
                .offerExpiresAt(s.getOfferExpiresAt() != null ? s.getOfferExpiresAt().toString() : null)
                .lastUpdatedBy(s.getLastUpdatedBy())
                .updatedAt(s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null)
                .offerActive(offerActive)
                .build();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static LocalDateTime parseDate(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }
}