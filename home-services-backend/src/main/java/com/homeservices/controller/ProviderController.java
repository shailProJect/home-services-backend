package com.homeservices.controller;

import com.homeservices.dto.request.AvailabilityRequest;
import com.homeservices.dto.request.BookingStatusRequest;
import com.homeservices.dto.request.ProviderProfileRequest;
import com.homeservices.dto.request.ProviderServiceRequest;
import com.homeservices.dto.response.ApiResponse;
import com.homeservices.dto.response.BookingResponse;
import com.homeservices.dto.response.ProviderAvailabilityResponse;
import com.homeservices.dto.response.ProviderDetailResponse;
import com.homeservices.dto.response.ProviderResponse;
import com.homeservices.dto.response.ProviderServiceResponse;
import com.homeservices.service.ProviderManagementService;
import com.homeservices.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/provider")
@RequiredArgsConstructor
public class ProviderController {

  private final ProviderManagementService providerManagementService;
  private final SecurityUtil securityUtil;

  // ── Services ──────────────────────────────────────────────────────────────

  @PostMapping("/services")
  public ResponseEntity<ApiResponse<ProviderServiceResponse>> addService(
      @Valid @RequestBody ProviderServiceRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            providerManagementService.addService(securityUtil.getCurrentUserId(), request),
            "Service added successfully"));
  }

  @GetMapping("/services")
  public ResponseEntity<ApiResponse<List<ProviderServiceResponse>>> getMyServices() {
    return ResponseEntity.ok(ApiResponse
        .success(providerManagementService.getMyServices(securityUtil.getCurrentUserId())));
  }

  @PutMapping("/services/{id}")
  public ResponseEntity<ApiResponse<ProviderServiceResponse>> updateService(@PathVariable UUID id,
      @Valid @RequestBody ProviderServiceRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
        providerManagementService.updateService(securityUtil.getCurrentUserId(), id, request),
        "Service updated successfully"));
  }

  /** DELETE /provider/services/{id} — soft-deletes the service */
  @DeleteMapping("/services/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable UUID id) {
    providerManagementService.deleteService(securityUtil.getCurrentUserId(), id);
    return ResponseEntity.ok(ApiResponse.success(null, "Service deleted successfully"));
  }

  // ── Profile ───────────────────────────────────────────────────────────────

  @GetMapping("/profile")
  public ResponseEntity<ApiResponse<ProviderResponse>> getProfile() {
    return ResponseEntity.ok(ApiResponse
        .success(providerManagementService.getProviderProfile(securityUtil.getCurrentUserId())));
  }

  @PutMapping("/profile")
  public ResponseEntity<ApiResponse<ProviderResponse>> updateProfile(
      @RequestBody ProviderProfileRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
        providerManagementService.updateProviderProfile(securityUtil.getCurrentUserId(), request),
        "Profile updated successfully"));
  }

  @PutMapping("/active")
  public ResponseEntity<ApiResponse<ProviderResponse>> toggleActive(@RequestParam boolean active) {
    ProviderResponse res =
        providerManagementService.toggleActive(securityUtil.getCurrentUserId(), active);
    return ResponseEntity.ok(ApiResponse.success(res,
        active ? "You are now online and visible to customers" : "You are now offline"));
  }

  // ── Availability ──────────────────────────────────────────────────────────

  @PostMapping("/availability")
  public ResponseEntity<ApiResponse<ProviderAvailabilityResponse>> addAvailability(
      @Valid @RequestBody AvailabilityRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            providerManagementService.addAvailability(securityUtil.getCurrentUserId(), request),
            "Availability slot added successfully"));
  }

  /** GET /provider/availability — list all slots for this provider */
  @GetMapping("/availability")
  public ResponseEntity<ApiResponse<List<ProviderAvailabilityResponse>>> getMyAvailability() {
    return ResponseEntity.ok(ApiResponse
        .success(providerManagementService.getMyAvailability(securityUtil.getCurrentUserId())));
  }

  /** DELETE /provider/availability/{id} — remove a specific slot */
  @DeleteMapping("/availability/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteAvailability(@PathVariable UUID id) {
    providerManagementService.deleteAvailability(securityUtil.getCurrentUserId(), id);
    return ResponseEntity.ok(ApiResponse.success(null, "Slot removed"));
  }

  // ── Bookings ──────────────────────────────────────────────────────────────

  @GetMapping("/bookings")
  public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings() {
    return ResponseEntity.ok(ApiResponse
        .success(providerManagementService.getMyBookings(securityUtil.getCurrentUserId())));
  }

  @PutMapping("/bookings/{id}/status")
  public ResponseEntity<ApiResponse<BookingResponse>> updateBookingStatus(@PathVariable UUID id,
      @Valid @RequestBody BookingStatusRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
        providerManagementService.updateBookingStatus(securityUtil.getCurrentUserId(), id, request),
        "Booking status updated"));
  }

  // ── Documents ─────────────────────────────────────────────────────────────

  @PostMapping(value = "/documents", consumes = "multipart/form-data")
  public ResponseEntity<ApiResponse<ProviderDetailResponse>> uploadDocuments(
      @RequestPart(value = "govtId", required = false) MultipartFile govtId,
      @RequestPart(value = "businessCertificate",
          required = false) MultipartFile businessCertificate,
      @RequestPart(value = "addressProof", required = false) MultipartFile addressProof) {
    return ResponseEntity.ok(ApiResponse
        .success(providerManagementService.uploadDocuments(securityUtil.getCurrentUserId(), govtId,
            businessCertificate, addressProof), "Documents uploaded successfully"));
  }
}
