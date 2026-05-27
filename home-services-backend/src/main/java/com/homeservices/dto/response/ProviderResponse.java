package com.homeservices.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ProviderResponse {
  private UUID id;
  private UUID userId;
  private String name;
  private String email;
  private String phone;
  private Integer experienceYears;
  private String serviceArea;
  private Double latitude;
  private Double longitude;
  private boolean verified;
  private boolean active;
  private Double rating;
  private UUID categoryId;
  private String categoryName;
  private String shopName;
  private String shopAddress;

  // === ENHANCED: Trust & affordability signals ===
  /** Total number of reviews submitted for this provider */
  private Long totalReviews;

  /** Starting (minimum) price across all active services — e.g. "Starting from ₹499" */
  private BigDecimal startingPrice;

  /** Most positive recent review comment to display as a highlighted testimonial */
  private String highlightedFeedback;

  /** Provider's profile photo URL */
  private String profilePhotoUrl;
}