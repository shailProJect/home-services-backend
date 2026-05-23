package com.homeservices.controller;

import com.homeservices.dto.request.ForgotPasswordRequest;
import com.homeservices.dto.request.LoginRequest;
import com.homeservices.dto.request.PhoneLoginRequest;
import com.homeservices.dto.request.RefreshTokenRequest;
import com.homeservices.dto.request.RegisterRequest;
import com.homeservices.dto.request.ResendOtpRequest;
import com.homeservices.dto.request.ResetPasswordRequest;
import com.homeservices.dto.response.ApiResponse;
import com.homeservices.dto.response.AuthResponse;
import com.homeservices.service.AuthService;
import com.resend.core.exception.ResendException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.homeservices.dto.request.VerifyEmailRequest;
import com.homeservices.dto.request.VerifyProviderOtpRequest;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<AuthResponse>> register(
      @Valid @RequestBody RegisterRequest request) {
    try {
      AuthResponse response = authService.register(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(
          ApiResponse.success(response, "Registration successful. Please verify your email."));
    } catch (ResendException e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to send verification email"));
    }
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<AuthResponse>> refresh(
      @Valid @RequestBody RefreshTokenRequest request) {
    AuthResponse response = authService.refresh(request);
    return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
  }

  @PostMapping("/verify-email")
  public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestBody VerifyEmailRequest request) {

    authService.verifyEmail(request.getEmail(), request.getOtp());

    return ResponseEntity.ok(ApiResponse.success("SUCCESS", "Email verified successfully"));
  }

  @PostMapping("/resend-otp")
  public ResponseEntity<ApiResponse<String>> resendOtp(@RequestBody ResendOtpRequest request)
      throws ResendException {

    authService.resendOtp(request.getEmail());

    return ResponseEntity.ok(ApiResponse.success("SUCCESS", "OTP resent successfully"));
  }

  /**
   * POST /auth/login-phone
   *
   * Mobile-only endpoint. After the user completes Firebase Phone Authentication
   * (signInWithPhoneNumber), the mobile app sends the resulting Firebase ID token here. The backend
   * verifies it with Firebase Admin SDK, looks up the matching account by phone number, and returns
   * the app's own JWT access + refresh tokens.
   *
   * Request body: { "firebaseToken": "<Firebase ID token>" } Response body: standard AuthResponse
   * with accessToken, refreshToken, userId, role
   */
  @PostMapping("/login-phone")
  public ResponseEntity<ApiResponse<AuthResponse>> loginWithPhone(
      @Valid @RequestBody PhoneLoginRequest request) {
    AuthResponse response = authService.loginWithPhone(request.getFirebaseToken());
    return ResponseEntity.ok(ApiResponse.success(response, "Phone login successful"));
  }

  /**
   * POST /auth/forgot-password Send a password-reset OTP to the user's registered email. Body: {
   * "email": "user@example.com" }
   */
  @PostMapping("/forgot-password")
  public ResponseEntity<ApiResponse<String>> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request) {
    try {
      authService.forgotPassword(request.getEmail());
      return ResponseEntity
          .ok(ApiResponse.success("OTP_SENT", "Password reset OTP sent to your email."));
    } catch (com.resend.core.exception.ResendException e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to send reset email. Please try again."));
    }
  }

  /**
   * POST /auth/reset-password Verify OTP and set new password. Body: { "email": "...", "otp":
   * "123456", "newPassword": "newpass123" }
   */
  @PostMapping("/reset-password")
  public ResponseEntity<ApiResponse<String>> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
    return ResponseEntity
        .ok(ApiResponse.success("SUCCESS", "Password reset successfully. You can now login."));
  }

  @PostMapping("/verify-provider-phone")
  public ResponseEntity<ApiResponse<String>> verifyProviderPhone(
      @RequestBody VerifyProviderOtpRequest request) {

    authService.verifyProviderPhone(request.getPhone(), request.getOtp());

    return ResponseEntity.ok(ApiResponse.success("SUCCESS", "Phone verified successfully"));
  }
}
