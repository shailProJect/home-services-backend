package com.homeservices.controller;

import com.homeservices.dto.request.AdminNoteRequest;
import com.homeservices.dto.response.ApiResponse;
import com.homeservices.dto.response.BookingResponse;
import com.homeservices.dto.response.ProviderDetailResponse;
import com.homeservices.dto.response.ProviderResponse;
import com.homeservices.dto.response.ReviewResponse;
import com.homeservices.dto.response.UserResponse;
import com.homeservices.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

  private final AdminService adminService;

  // ── Providers ─────────────────────────────────────────────────────────────

  @GetMapping("/providers")
  public ResponseEntity<ApiResponse<List<ProviderResponse>>> getAllProviders() {
    return ResponseEntity.ok(ApiResponse.success(adminService.getAllProviders()));
  }

  @GetMapping("/providers/{id}")
  public ResponseEntity<ApiResponse<ProviderDetailResponse>> getProviderDetail(
      @PathVariable UUID id) {
    return ResponseEntity
        .ok(ApiResponse.success(adminService.getProviderDetail(id), "Provider detail fetched"));
  }

  @PutMapping("/providers/{id}/approve")
  public ResponseEntity<ApiResponse<ProviderDetailResponse>> approveProvider(
      @PathVariable UUID id) {
    return ResponseEntity.ok(
        ApiResponse.success(adminService.approveProvider(id), "Provider approved successfully"));
  }

  @PutMapping("/providers/{id}/reject")
  public ResponseEntity<ApiResponse<ProviderDetailResponse>> rejectProvider(@PathVariable UUID id,
      @RequestBody(required = false) AdminNoteRequest noteRequest) {
    return ResponseEntity.ok(ApiResponse.success(
        adminService.rejectProvider(id, noteRequest != null ? noteRequest.getNotes() : null),
        "Provider rejected"));
  }

  @PutMapping("/providers/{id}/toggle-active")
  public ResponseEntity<ApiResponse<ProviderResponse>> toggleProviderActive(@PathVariable UUID id,
      @RequestParam boolean active) {
    String msg = active ? "Provider activated" : "Provider deactivated";
    return ResponseEntity
        .ok(ApiResponse.success(adminService.toggleProviderActive(id, active), msg));
  }

  // ── Users ─────────────────────────────────────────────────────────────────

  @GetMapping("/users")
  public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
    return ResponseEntity.ok(ApiResponse.success(adminService.getAllUsers()));
  }

  // ── Bookings ──────────────────────────────────────────────────────────────

  @GetMapping("/bookings")
  public ResponseEntity<ApiResponse<List<BookingResponse>>> getAllBookings() {
    return ResponseEntity.ok(ApiResponse.success(adminService.getAllBookings()));
  }

  // ── Reviews (FIX: these were all commented out) ───────────────────────────

  /** GET /admin/reviews — All platform reviews */
  @GetMapping("/reviews")
  public ResponseEntity<ApiResponse<List<ReviewResponse>>> getAllReviews() {
    return ResponseEntity.ok(ApiResponse.success(adminService.getAllReviews()));
  }

  /** GET /admin/providers/{id}/reviews — Reviews for a specific provider */
  @GetMapping("/providers/{id}/reviews")
  public ResponseEntity<ApiResponse<List<ReviewResponse>>> getProviderReviews(
      @PathVariable UUID id) {
    return ResponseEntity.ok(ApiResponse.success(adminService.getReviewsByProvider(id)));
  }

  /**
   * DELETE /admin/reviews/{id} — Remove inappropriate review (also recalculates provider rating)
   */
  @DeleteMapping("/reviews/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable UUID id) {
    adminService.deleteReview(id);
    return ResponseEntity.ok(ApiResponse.success(null, "Review deleted"));
  }
}
