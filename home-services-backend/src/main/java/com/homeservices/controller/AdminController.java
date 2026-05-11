package com.homeservices.controller;

import com.homeservices.dto.response.ApiResponse;
import com.homeservices.dto.response.BookingResponse;
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

  /**
   * GET /admin/providers List all registered providers (verified and unverified).
   */
  @GetMapping("/providers")
  public ResponseEntity<ApiResponse<List<ProviderResponse>>> getAllProviders() {
    return ResponseEntity.ok(ApiResponse.success(adminService.getAllProviders()));
  }

  /**
   * PUT /admin/providers/{id}/approve Verify and approve a provider.
   */
  @PutMapping("/providers/{id}/approve")
  public ResponseEntity<ApiResponse<ProviderResponse>> approveProvider(@PathVariable UUID id) {
    ProviderResponse response = adminService.approveProvider(id);
    return ResponseEntity.ok(ApiResponse.success(response, "Provider approved successfully"));
  }

  /**
   * PUT /admin/providers/{id}/toggle-active Enable or disable a provider (active/inactive).
   */
  @PutMapping("/providers/{id}/toggle-active")
  public ResponseEntity<ApiResponse<ProviderResponse>> toggleProviderActive(@PathVariable UUID id,
      @RequestParam boolean active) {
    ProviderResponse response = adminService.toggleProviderActive(id, active);
    String msg = active ? "Provider activated successfully" : "Provider deactivated successfully";
    return ResponseEntity.ok(ApiResponse.success(response, msg));
  }

  /**
   * GET /admin/users List all registered users.
   */
  @GetMapping("/users")
  public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
    return ResponseEntity.ok(ApiResponse.success(adminService.getAllUsers()));
  }

  /**
   * GET /admin/bookings Monitor all bookings in the system.
   */
  @GetMapping("/bookings")
  public ResponseEntity<ApiResponse<List<BookingResponse>>> getAllBookings() {
    return ResponseEntity.ok(ApiResponse.success(adminService.getAllBookings()));
  }
}
