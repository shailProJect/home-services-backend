package com.homeservices.service;

import com.homeservices.dto.request.PlatformSettingsRequest;
import com.homeservices.dto.response.PlatformSettingsResponse;
import com.homeservices.entity.PlatformSettings;
import com.homeservices.repository.PlatformSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PlatformSettingsService {

  private static final long SETTINGS_ID = 1L;
  private final PlatformSettingsRepository repo;

  // ─── public API ──────────────────────────────────────────────────────────

  /** Returns current settings, auto-creating the singleton row if it doesn't exist yet. */
  public PlatformSettingsResponse getSettings() {
    return toResponse(getOrCreate());
  }

  /**
   * Admin updates platform settings.
   * 
   * @param updatedBy email / name of the admin making the change (from SecurityContext)
   */
  @Transactional
  public PlatformSettingsResponse updateSettings(PlatformSettingsRequest req, String updatedBy) {
    PlatformSettings s = getOrCreate();
    s.setFreeServiceLimit(req.getFreeServiceLimit());
    s.setOfferBannerText(blankToNull(req.getOfferBannerText()));
    s.setOfferDiscountPercent(req.getOfferDiscountPercent());
    s.setOfferExpiresAt(parseDate(req.getOfferExpiresAt()));
    s.setLastUpdatedBy(updatedBy);
    return toResponse(repo.save(s));
  }

  /**
   * Convenience method used by SubscriptionService to get the live free limit without going through
   * the full DTO mapping.
   */
  public int getFreeLimitLive() {
    return getOrCreate().getFreeServiceLimit();
  }

  // ─── helpers ─────────────────────────────────────────────────────────────

  private PlatformSettings getOrCreate() {
    return repo.findById(SETTINGS_ID).orElseGet(() -> {
      PlatformSettings defaults = PlatformSettings.builder().id(SETTINGS_ID).freeServiceLimit(10)
          .offerDiscountPercent(0).build();
      return repo.save(defaults);
    });
  }

  private PlatformSettingsResponse toResponse(PlatformSettings s) {
    boolean offerActive = s.getOfferBannerText() != null && !s.getOfferBannerText().isBlank()
        && (s.getOfferExpiresAt() == null || s.getOfferExpiresAt().isAfter(LocalDateTime.now()));

    return PlatformSettingsResponse.builder().freeServiceLimit(s.getFreeServiceLimit())
        .offerBannerText(s.getOfferBannerText()).offerDiscountPercent(s.getOfferDiscountPercent())
        .offerExpiresAt(s.getOfferExpiresAt() != null ? s.getOfferExpiresAt().toString() : null)
        .lastUpdatedBy(s.getLastUpdatedBy())
        .updatedAt(s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null)
        .offerActive(offerActive).build();
  }

  private static String blankToNull(String s) {
    return (s == null || s.isBlank()) ? null : s.trim();
  }

  private static LocalDateTime parseDate(String iso) {
    if (iso == null || iso.isBlank())
      return null;
    try {
      return LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    } catch (Exception e) {
      return null; // ignore bad dates from frontend
    }
  }
}
