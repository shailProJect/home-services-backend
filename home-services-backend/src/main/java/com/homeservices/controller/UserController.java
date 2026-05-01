package com.homeservices.controller;

import com.homeservices.dto.request.BookingRequest;
import com.homeservices.dto.request.ReviewRequest;
import com.homeservices.dto.response.*;
import com.homeservices.service.UserService;
import com.homeservices.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SecurityUtil securityUtil;

    /**
     * GET /user/providers?category=ELECTRICIAN
     * Search for active provider services by category.
     */
    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<List<ProviderServiceResponse>>> searchByCategory(
            @RequestParam String category) {
        List<ProviderServiceResponse> result = userService.searchByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /user/providers/nearby?lat=&lng=&radius=5
     * Find nearby providers using Haversine formula within given km radius.
     */
    @GetMapping("/providers/nearby")
    public ResponseEntity<ApiResponse<List<ProviderResponse>>> findNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") double radius) {
        List<ProviderResponse> result = userService.findNearbyProviders(lat, lng, radius);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * POST /user/bookings
     * Create a new booking.
     */
    @PostMapping("/bookings")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request) {
        BookingResponse response = userService.createBooking(securityUtil.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Booking created successfully"));
    }

    /**
     * GET /user/bookings
     * Get the current user's bookings.
     */
    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings() {
        List<BookingResponse> bookings = userService.getMyBookings(securityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    /**
     * POST /user/reviews
     * Submit a review for a provider.
     */
    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> addReview(
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = userService.addReview(securityUtil.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Review submitted successfully"));
    }
}
