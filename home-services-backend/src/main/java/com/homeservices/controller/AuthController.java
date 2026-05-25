package com.homeservices.controller;

import com.homeservices.dto.request.ForgotPasswordRequest;
import com.homeservices.dto.request.LoginRequest;
import com.homeservices.dto.request.RefreshTokenRequest;
import com.homeservices.dto.request.RegisterRequest;
import com.homeservices.dto.request.ResendOtpRequest;
import com.homeservices.dto.request.ResetPasswordRequest;
import com.homeservices.dto.request.VerifyEmailRequest;
import com.homeservices.dto.response.ApiResponse;
import com.homeservices.dto.response.AuthResponse;
import com.homeservices.service.AuthService;
import com.resend.core.exception.ResendException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Auth endpoints — email/password only. Phone / Firebase endpoints have been fully removed.
 */
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
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to send verification email"));
    }
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(ApiResponse.success(authService.login(request), "Login successful"));
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<AuthResponse>> refresh(
      @Valid @RequestBody RefreshTokenRequest request) {
    return ResponseEntity.ok(ApiResponse.success(authService.refresh(request), "Token refreshed"));
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

  @PostMapping("/forgot-password")
  public ResponseEntity<ApiResponse<String>> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request) {
    try {
      authService.forgotPassword(request.getEmail());
      return ResponseEntity
          .ok(ApiResponse.success("OTP_SENT", "Password reset OTP sent to your email."));
    } catch (ResendException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to send reset email. Please try again."));
    }
  }
}