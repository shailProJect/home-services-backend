package com.homeservices.controller;

import com.homeservices.dto.request.AvailabilityRequest;
import com.homeservices.dto.request.BookingStatusRequest;
import com.homeservices.dto.request.ProviderServiceRequest;
import com.homeservices.dto.response.ApiResponse;
import com.homeservices.dto.response.BookingResponse;
import com.homeservices.dto.response.ProviderResponse;
import com.homeservices.dto.response.ProviderServiceResponse;
import com.homeservices.service.ProviderManagementService;
import com.homeservices.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/provider")
@RequiredArgsConstructor
public class ProviderController {

  private final ProviderManagementService providerManagementService;
  private final SecurityUtil securityUtil;

  /** POST /provider/services — Add a new service */
  @PostMapping("/services")
  public ResponseEntity<ApiResponse<ProviderServiceResponse>> addService(
      @Valid @RequestBody ProviderServiceRequest request) {
    ProviderServiceResponse response =
        providerManagementService.addService(securityUtil.getCurrentUserId(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(response, "Service added successfully"));
  }

  /** GET /provider/services — Get all services for the authenticated provider */
  @GetMapping("/services")
  public ResponseEntity<ApiResponse<List<ProviderServiceResponse>>> getMyServices() {
    List<ProviderServiceResponse> services =
        providerManagementService.getMyServices(securityUtil.getCurrentUserId());
    return ResponseEntity.ok(ApiResponse.success(services));
  }

  /**
   * PUT /provider/services/{id} — Edit an existing service
   */
  @PutMapping("/services/{id}")
  public ResponseEntity<ApiResponse<ProviderServiceResponse>> updateService(@PathVariable UUID id,
      @Valid @RequestBody ProviderServiceRequest request) {
    ProviderServiceResponse response =
        providerManagementService.updateService(securityUtil.getCurrentUserId(), id, request);
    return ResponseEntity.ok(ApiResponse.success(response, "Service updated successfully"));
  }

  /** PUT /provider/active — Toggle provider active/inactive status */
  @PutMapping("/active")
  public ResponseEntity<ApiResponse<ProviderResponse>> toggleActive(@RequestParam boolean active) {
    ProviderResponse response =
        providerManagementService.toggleActive(securityUtil.getCurrentUserId(), active);
    String msg = active ? "You are now active and visible to users" : "You are now inactive";
    return ResponseEntity.ok(ApiResponse.success(response, msg));
  }

  /** GET /provider/profile — Get provider's own profile/status */
  @GetMapping("/profile")
  public ResponseEntity<ApiResponse<ProviderResponse>> getProfile() {
    ProviderResponse response =
        providerManagementService.getProviderProfile(securityUtil.getCurrentUserId());
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /** POST /provider/availability — Add an availability slot */
  @PostMapping("/availability")
  public ResponseEntity<ApiResponse<Void>> addAvailability(
      @Valid @RequestBody AvailabilityRequest request) {
    providerManagementService.addAvailability(securityUtil.getCurrentUserId(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(null, "Availability slot added successfully"));
  }

  /** GET /provider/bookings — Get all bookings for the authenticated provider */
  @GetMapping("/bookings")
  public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings() {
    List<BookingResponse> bookings =
        providerManagementService.getMyBookings(securityUtil.getCurrentUserId());
    return ResponseEntity.ok(ApiResponse.success(bookings));
  }

  /** PUT /provider/bookings/{id}/status — Accept, reject, or complete a booking */
  @PutMapping("/bookings/{id}/status")
  public ResponseEntity<ApiResponse<BookingResponse>> updateBookingStatus(@PathVariable UUID id,
      @Valid @RequestBody BookingStatusRequest request) {
    BookingResponse response =
        providerManagementService.updateBookingStatus(securityUtil.getCurrentUserId(), id, request);
    return ResponseEntity.ok(ApiResponse.success(response, "Booking status updated"));
  }
}
