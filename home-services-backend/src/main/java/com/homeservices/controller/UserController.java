package com.homeservices.controller;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.homeservices.dto.request.BookingRequest;
import com.homeservices.dto.request.FirebasePhoneVerifyRequest;
import com.homeservices.dto.request.ReviewRequest;
import com.homeservices.dto.request.SendPhoneOtpRequest;
import com.homeservices.dto.request.UpdateProfileRequest;
import com.homeservices.dto.request.VerifyPhoneRequest;
import com.homeservices.dto.response.ApiResponse;
import com.homeservices.dto.response.BookingResponse;
import com.homeservices.dto.response.ProviderResponse;
import com.homeservices.dto.response.ProviderServiceResponse;
import com.homeservices.dto.response.ReviewResponse;
import com.homeservices.dto.response.UserResponse;
import com.homeservices.entity.User;
import com.homeservices.repository.UserRepository;
import com.homeservices.service.FirebaseService;
import com.homeservices.service.UserService;
import com.homeservices.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final SecurityUtil securityUtil;
  private final FirebaseService firebaseService;
  private final UserRepository userRepository;

  /**
   * GET /user/profile — Get current user's profile
   */
  @GetMapping("/profile")
  public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
    UserResponse response = userService.getProfile(securityUtil.getCurrentUserId());
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * PUT /user/profile — Update current user's name and address
   */
  @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<UserResponse>> updateProfile(

      @RequestPart("data") UpdateProfileRequest request,

      @RequestPart(value = "profilePhoto", required = false) MultipartFile profilePhoto) {

    UserResponse response =
        userService.updateProfile(securityUtil.getCurrentUserId(), request, profilePhoto);

    return ResponseEntity.ok(ApiResponse.success(response, "Profile updated successfully"));
  }

  /**
   * POST /user/phone/send-otp — Send OTP to user's phone (or a new phone number). Phone is
   * optional; if omitted the existing phone on the account is used.
   */
  @PostMapping("/phone/send-otp")
  public ResponseEntity<ApiResponse<String>> sendPhoneOtp(
      @RequestBody SendPhoneOtpRequest request) {
    String otp = userService.sendPhoneOtp(securityUtil.getCurrentUserId(), request.getPhone());
    return ResponseEntity.ok(ApiResponse.success(otp, "OTP sent to phone"));
  }

  /**
   * POST /user/phone/verify — Verify phone OTP and mark phone as verified.
   */
  @PostMapping("/phone/verify")
  public ResponseEntity<ApiResponse<UserResponse>> verifyPhone(
      @RequestBody VerifyPhoneRequest request) {
    UserResponse response = userService.verifyPhone(securityUtil.getCurrentUserId(), request);
    return ResponseEntity.ok(ApiResponse.success(response, "Phone verified successfully"));
  }

  /**
   * PUT /user/phone — Update phone number (resets verification).
   */
  @PutMapping("/phone")
  public ResponseEntity<ApiResponse<UserResponse>> updatePhone(
      @RequestBody SendPhoneOtpRequest request) {
    UserResponse response =
        userService.updatePhone(securityUtil.getCurrentUserId(), request.getPhone());
    return ResponseEntity
        .ok(ApiResponse.success(response, "Phone updated. Please verify your new number."));
  }

  /**
   * GET /user/providers?category=ELECTRICIAN Search for active provider services by category.
   */
  @GetMapping("/providers")
  public ResponseEntity<ApiResponse<List<ProviderServiceResponse>>> searchByCategory(
      @RequestParam String category) {
    List<ProviderServiceResponse> result = userService.searchByCategory(category);
    return ResponseEntity.ok(ApiResponse.success(result));
  }

  /**
   * GET /user/providers/nearby?lat=&lng=&radius=5 Find nearby providers using Haversine formula
   * within given km radius.
   */
  @GetMapping("/providers/nearby")
  public ResponseEntity<ApiResponse<List<ProviderResponse>>> findNearby(@RequestParam double lat,
      @RequestParam double lng, @RequestParam(defaultValue = "5") double radius) {
    List<ProviderResponse> result = userService.findNearbyProviders(lat, lng, radius);
    return ResponseEntity.ok(ApiResponse.success(result));
  }

  /**
   * POST /user/bookings Create a new booking.
   */
  @PostMapping("/bookings")
  public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
      @Valid @RequestBody BookingRequest request) {
    BookingResponse response = userService.createBooking(securityUtil.getCurrentUserId(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(response, "Booking created successfully"));
  }

  /**
   * GET /user/bookings Get the current user's bookings.
   */
  @GetMapping("/bookings")
  public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings() {
    List<BookingResponse> bookings = userService.getMyBookings(securityUtil.getCurrentUserId());
    return ResponseEntity.ok(ApiResponse.success(bookings));
  }

  /**
   * POST /user/reviews Submit a review for a provider.
   */
  @PostMapping("/reviews")
  public ResponseEntity<ApiResponse<ReviewResponse>> addReview(
      @Valid @RequestBody ReviewRequest request) {
    ReviewResponse response = userService.addReview(securityUtil.getCurrentUserId(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(response, "Review submitted successfully"));
  }

  /**
   * GET /user/providers/{id} — Get a specific provider's public profile by ID.
   */
  @GetMapping("/providers/{id}")
  public ResponseEntity<ApiResponse<ProviderResponse>> getProviderById(@PathVariable UUID id) {
    ProviderResponse response = userService.getProviderById(id);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * GET /user/providers/{id}/reviews — Get all reviews for a specific provider.
   */
  @GetMapping("/providers/{id}/reviews")
  public ResponseEntity<ApiResponse<List<ReviewResponse>>> getProviderReviews(
      @PathVariable UUID id) {
    List<ReviewResponse> reviews = userService.getProviderReviews(id);
    return ResponseEntity.ok(ApiResponse.success(reviews));
  }

  /**
   * GET /user/reviews/me — Get current user's submitted reviews (to check if already reviewed).
   */
  @GetMapping("/reviews/me")
  public ResponseEntity<ApiResponse<List<ReviewResponse>>> getMyReviews() {
    List<ReviewResponse> reviews = userService.getMyReviews(securityUtil.getCurrentUserId());
    return ResponseEntity.ok(ApiResponse.success(reviews));
  }

  @PostMapping("/phone/firebase-verify")
  public ResponseEntity<?> verifyPhone(@RequestBody FirebasePhoneVerifyRequest request) {

    try {

      String phoneNumber = firebaseService.verifyToken(request.getFirebaseToken());

      UUID userId = securityUtil.getCurrentUserId();

      User user =
          userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

      user.setPhone(phoneNumber);

      user.setPhoneVerified(true);

      userRepository.save(user);

      return ResponseEntity.ok("Phone verified successfully");

    } catch (Exception e) {

      e.printStackTrace();

      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @PutMapping(value = "/profile/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<UserResponse>> uploadProfilePhoto(
      @RequestPart("profilePhoto") MultipartFile profilePhoto) {

    try {

      UserResponse response =
          userService.uploadProfilePhoto(securityUtil.getCurrentUserId(), profilePhoto);

      return ResponseEntity
          .ok(ApiResponse.success(response, "Profile photo uploaded successfully"));

    } catch (IOException e) {

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to upload profile photo"));

    } catch (RuntimeException e) {

      return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
  }
}
