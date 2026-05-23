package com.homeservices.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Full provider profile returned to admin for verification review.
 * Includes personal info, shop details, location, documents, and account status.
 */
@Data
@Builder
public class ProviderDetailResponse {

  // ── Provider identity ─────────────────────────────────────────────────────
  private UUID providerId;
  private UUID userId;

  // ── User / personal info ──────────────────────────────────────────────────
  private String name;
  private String email;
  private String phone;
  private boolean emailVerified;
  private boolean phoneVerified;
  private LocalDateTime registeredAt;
  private String userAddress;

  // ── Professional info ─────────────────────────────────────────────────────
  private UUID categoryId;
  private String categoryName;
  private Integer experienceYears;
  private Double rating;

  // ── Shop / location ───────────────────────────────────────────────────────
  private String shopName;
  private String shopAddress;
  private String serviceArea;
  private Double latitude;
  private Double longitude;

  // ── Documents (file URLs) ─────────────────────────────────────────────────
  private String govtIdDocumentUrl;
  private String businessCertificateUrl;
  private String addressProofUrl;

  // ── Admin / verification status ───────────────────────────────────────────
  private String profilePhotoUrl;

  private boolean verified;
  private boolean active;
  private String adminNotes;
}
