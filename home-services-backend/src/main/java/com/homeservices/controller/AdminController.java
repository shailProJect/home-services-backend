package com.homeservices.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.homeservices.dto.request.AdminNoteRequest;
import com.homeservices.dto.request.PlatformSettingsRequest;
import com.homeservices.dto.response.ApiResponse;
import com.homeservices.dto.response.BookingResponse;
import com.homeservices.dto.response.PlatformSettingsResponse;
import com.homeservices.dto.response.ProviderDetailResponse;
import com.homeservices.dto.response.ProviderResponse;
import com.homeservices.dto.response.ReviewResponse;
import com.homeservices.dto.response.UserResponse;
import com.homeservices.service.AdminService;
import com.homeservices.service.PlatformSettingsService;
import com.homeservices.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// TODO: Auto-generated Javadoc
/**
 * The Class AdminController.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

  /** The admin service. */
  private final AdminService adminService;
  private final PlatformSettingsService platformSettingsService;
  private final SecurityUtil securityUtil;
  // ── Providers ─────────────────────────────────────────────────────────────

  /**
   * Gets the all providers.
   *
   * @return the all providers
   */
  @GetMapping("/providers")
  public ResponseEntity<ApiResponse<List<ProviderResponse>>> getAllProviders() {
    return ResponseEntity.ok(ApiResponse.success(adminService.getAllProviders()));
  }

  /**
   * Gets the provider detail.
   *
   * @param id the id
   * @return the provider detail
   */
  @GetMapping("/providers/{id}")
  public ResponseEntity<ApiResponse<ProviderDetailResponse>> getProviderDetail(
      @PathVariable UUID id) {
    return ResponseEntity
        .ok(ApiResponse.success(adminService.getProviderDetail(id), "Provider detail fetched"));
  }

  /**
   * Approve provider.
   *
   * @param id the id
   * @return the response entity
   */
  @PutMapping("/providers/{id}/approve")
  public ResponseEntity<ApiResponse<ProviderDetailResponse>> approveProvider(
      @PathVariable UUID id) {
    return ResponseEntity.ok(
        ApiResponse.success(adminService.approveProvider(id), "Provider approved successfully"));
  }

  /**
   * Reject provider.
   *
   * @param id the id
   * @param noteRequest the note request
   * @return the response entity
   */
  @PutMapping("/providers/{id}/reject")
  public ResponseEntity<ApiResponse<ProviderDetailResponse>> rejectProvider(@PathVariable UUID id,
      @RequestBody(required = false) AdminNoteRequest noteRequest) {
    return ResponseEntity.ok(ApiResponse.success(
        adminService.rejectProvider(id, noteRequest != null ? noteRequest.getNotes() : null),
        "Provider rejected"));
  }

  /**
   * Toggle provider active.
   *
   * @param id the id
   * @param active the active
   * @return the response entity
   */
  @PutMapping("/providers/{id}/toggle-active")
  public ResponseEntity<ApiResponse<ProviderResponse>> toggleProviderActive(@PathVariable UUID id,
      @RequestParam boolean active) {
    String msg = active ? "Provider activated" : "Provider deactivated";
    return ResponseEntity
        .ok(ApiResponse.success(adminService.toggleProviderActive(id, active), msg));
  }

  // ── Users ─────────────────────────────────────────────────────────────────

  /**
   * Gets the all users.
   *
   * @return the all users
   */
  @GetMapping("/users")
  public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
    return ResponseEntity.ok(ApiResponse.success(adminService.getAllUsers()));
  }

  // ── Bookings ──────────────────────────────────────────────────────────────

  /**
   * Gets the all bookings.
   *
   * @return the all bookings
   */
  @GetMapping("/bookings")
  public ResponseEntity<ApiResponse<List<BookingResponse>>> getAllBookings() {
    return ResponseEntity.ok(ApiResponse.success(adminService.getAllBookings()));
  }

  // ── Reviews (FIX: these were all commented out) ───────────────────────────

  /**
   * GET /admin/reviews — All platform reviews.
   *
   * @return the all reviews
   */
  @GetMapping("/reviews")
  public ResponseEntity<ApiResponse<List<ReviewResponse>>> getAllReviews() {
    return ResponseEntity.ok(ApiResponse.success(adminService.getAllReviews()));
  }

  /**
   * GET /admin/providers/{id}/reviews — Reviews for a specific provider.
   *
   * @param id the id
   * @return the provider reviews
   */
  @GetMapping("/providers/{id}/reviews")
  public ResponseEntity<ApiResponse<List<ReviewResponse>>> getProviderReviews(
      @PathVariable UUID id) {
    return ResponseEntity.ok(ApiResponse.success(adminService.getReviewsByProvider(id)));
  }

  /**
   * DELETE /admin/reviews/{id} — Remove inappropriate review (also recalculates provider rating).
   *
   * @param id the id
   * @return the response entity
   */
  @DeleteMapping("/reviews/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable UUID id) {
    adminService.deleteReview(id);
    return ResponseEntity.ok(ApiResponse.success(null, "Review deleted"));
  }

  /**
   * Gets the settings.
   *
   * @return the settings
   */
  @GetMapping("/settings")
  public ResponseEntity<ApiResponse<PlatformSettingsResponse>> getSettings() {
    return ResponseEntity.ok(ApiResponse.success(platformSettingsService.getSettings()));
  }

  /**
   * PUT /admin/settings Update free tier booking limit and/or promotional offer.
   * 
   * Request body example: { "freeServiceLimit": 15, "offerBannerText": "🎉 Festival Offer — 20% off
   * all plans!", "offerDiscountPercent": 20, "offerExpiresAt": "2025-10-31T23:59:59" }
   *
   * @param request the request
   * @return the response entity
   */
  @PutMapping("/settings")
  public ResponseEntity<ApiResponse<PlatformSettingsResponse>> updateSettings(
      @Valid @RequestBody PlatformSettingsRequest request) {
    String adminEmail = securityUtil.getCurrentUserEmail(); // add this helper if not present
    return ResponseEntity
        .ok(ApiResponse.success(platformSettingsService.updateSettings(request, adminEmail),
            "Platform settings updated successfully"));
  }
}
