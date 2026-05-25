package com.homeservices.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformSettingsResponse {
  private int freeServiceLimit;
  private String offerBannerText;
  private int offerDiscountPercent;
  private String offerExpiresAt; // ISO string, nullable
  private String lastUpdatedBy;
  private String updatedAt; // ISO string
  private boolean offerActive; // convenience flag for frontend
}
