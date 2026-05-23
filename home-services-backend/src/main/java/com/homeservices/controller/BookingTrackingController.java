package com.homeservices.controller;

import com.homeservices.dto.request.BookingRequest;
import com.homeservices.dto.request.ProviderLocationUpdateRequest;
import com.homeservices.dto.response.ApiResponse;
import com.homeservices.dto.response.BookingResponse;
import com.homeservices.service.BookingTrackingService;
import com.homeservices.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingTrackingController {

    private final BookingTrackingService bookingTrackingService;
    private final SecurityUtil securityUtil;

    /**
     * POST /bookings/urgent
     * Creates an urgent booking. Provider must arrive within 30 minutes.
     * ₹199 surcharge is applied automatically.
     */
    @PostMapping("/urgent")
    public ResponseEntity<ApiResponse<BookingResponse>> createUrgentBooking(
            @Valid @RequestBody BookingRequest request) {

        request.setUrgent(true);
        BookingResponse response = bookingTrackingService.createBooking(
                securityUtil.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Urgent booking created! Provider will arrive within 30 minutes."));
    }

    /**
     * GET /bookings/{id}/track
     * Returns live provider location + ETA for a booking.
     * Polling every 10 seconds from the frontend is recommended.
     */
    @GetMapping("/{id}/track")
    public ResponseEntity<ApiResponse<BookingResponse>> trackBooking(
            @PathVariable UUID id) {

        BookingResponse response = bookingTrackingService.getTrackingInfo(
                securityUtil.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PUT /bookings/{id}/provider-location  [PROVIDER ONLY]
     * Provider app calls this to update their live GPS location.
     * Updates providerLatitude, providerLongitude, estimatedArrivalMinutes.
     */
    @PutMapping("/{id}/provider-location")
    public ResponseEntity<ApiResponse<String>> updateProviderLocation(
            @PathVariable UUID id,
            @Valid @RequestBody ProviderLocationUpdateRequest request) {

        request.setBookingId(id);
        bookingTrackingService.updateProviderLocation(securityUtil.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Location updated"));
    }

    /**
     * PUT /bookings/{id}/arrived  [PROVIDER ONLY]
     * Provider marks themselves as arrived. Status → IN_PROGRESS.
     */
    @PutMapping("/{id}/arrived")
    public ResponseEntity<ApiResponse<BookingResponse>> markArrived(
            @PathVariable UUID id) {

        BookingResponse response = bookingTrackingService.markArrived(
                securityUtil.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(response, "Marked as arrived"));
    }
}
