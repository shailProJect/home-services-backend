package com.homeservices.controller;

import com.homeservices.dto.request.AdminNoteRequest;
import com.homeservices.dto.response.ApiResponse;
import com.homeservices.dto.response.BookingResponse;
import com.homeservices.dto.response.ProviderDetailResponse;
import com.homeservices.dto.response.ProviderResponse;
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

  // ── Provider list ─────────────────────────────────────────────────────────

  /**
   * GET /admin/providers
   * List all registered providers (verified and unverified) — summary view.
   */
  @GetMapping("/providers")
  public ResponseEntity<ApiResponse<List<ProviderResponse>>> getAllProviders() {
    return ResponseEntity.ok(ApiResponse.success(adminService.getAllProviders()));
  }

  // ── Provider detail (for review before verify) ────────────────────────────

  /**
   * GET /admin/providers/{id}
   * Full provider profile: personal info, shop address, all uploaded documents,
   * and current verification status. Use this before approving or rejecting.
   */
  @GetMapping("/providers/{id}")
  public ResponseEntity<ApiResponse<ProviderDetailResponse>> getProviderDetail(
      @PathVariable UUID id) {
    ProviderDetailResponse response = adminService.getProviderDetail(id);
    return ResponseEntity.ok(ApiResponse.success(response, "Provider detail fetched successfully"));
  }

  // ── Approve ───────────────────────────────────────────────────────────────

  /**
   * PUT /admin/providers/{id}/approve
   * Verify and approve a provider after reviewing their documents.
   * Returns the updated full detail.
   */
  @PutMapping("/providers/{id}/approve")
  public ResponseEntity<ApiResponse<ProviderDetailResponse>> approveProvider(
      @PathVariable UUID id) {
    ProviderDetailResponse response = adminService.approveProvider(id);
    return ResponseEntity.ok(ApiResponse.success(response, "Provider approved successfully"));
  }

  // ── Reject ────────────────────────────────────────────────────────────────

  /**
   * PUT /admin/providers/{id}/reject
   * Reject a provider with an optional reason/note.
   * The provider is set to verified=false, active=false, and the note is stored.
   *
   * Request body (optional): { "notes": "Document unclear, please resubmit" }
   */
  @PutMapping("/providers/{id}/reject")
  public ResponseEntity<ApiResponse<ProviderDetailResponse>> rejectProvider(
      @PathVariable UUID id,
      @RequestBody(required = false) AdminNoteRequest noteRequest) {
    String notes = (noteRequest != null) ? noteRequest.getNotes() : null;
    ProviderDetailResponse response = adminService.rejectProvider(id, notes);
    return ResponseEntity.ok(ApiResponse.success(response, "Provider rejected"));
  }

  // ── Toggle active ─────────────────────────────────────────────────────────

  /**
   * PUT /admin/providers/{id}/toggle-active?active=true|false
   * Enable or disable a provider (active/inactive).
   */
  @PutMapping("/providers/{id}/toggle-active")
  public ResponseEntity<ApiResponse<ProviderResponse>> toggleProviderActive(
      @PathVariable UUID id, @RequestParam boolean active) {
    ProviderResponse response = adminService.toggleProviderActive(id, active);
    String msg = active ? "Provider activated successfully" : "Provider deactivated successfully";
    return ResponseEntity.ok(ApiResponse.success(response, msg));
  }

  // ── Users ─────────────────────────────────────────────────────────────────

  /**
   * GET /admin/users
   * List all registered users.
   */
  @GetMapping("/users")
  public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
    return ResponseEntity.ok(ApiResponse.success(adminService.getAllUsers()));
  }

  // ── Bookings ──────────────────────────────────────────────────────────────

  /**
   * GET /admin/bookings
   * Monitor all bookings in the system.
   */
  @GetMapping("/bookings")
  public ResponseEntity<ApiResponse<List<BookingResponse>>> getAllBookings() {
    return ResponseEntity.ok(ApiResponse.success(adminService.getAllBookings()));
  }
}
