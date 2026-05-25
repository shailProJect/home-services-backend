package com.homeservices.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PlatformSettingsRequest {

  @Min(value = 0, message = "Free limit must be ≥ 0")
  @Max(value = 10000, message = "Free limit seems unreasonably high")
  private int freeServiceLimit;

  /** Nullable — send null or empty string to clear the banner */
  private String offerBannerText;

  @Min(0)
  @Max(100)
  private int offerDiscountPercent;

  /** ISO-8601 string or null. E.g. "2025-12-31T23:59:59" */
  private String offerExpiresAt;
}
