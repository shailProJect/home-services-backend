package com.homeservices.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.homeservices.dto.request.ForgotPasswordRequest;
import com.homeservices.dto.request.ResetPasswordRequest;
import com.homeservices.dto.request.LoginRequest;
import com.homeservices.dto.request.RefreshTokenRequest;
import com.homeservices.dto.request.RegisterRequest;
import com.homeservices.dto.response.AuthResponse;
import com.homeservices.entity.Provider;
import com.homeservices.entity.ServiceCategory;
import com.homeservices.entity.User;
import com.homeservices.entity.enums.Role;
import com.homeservices.exception.BadRequestException;
import com.homeservices.exception.ResourceNotFoundException;
import com.homeservices.repository.ProviderRepository;
import com.homeservices.repository.ServiceCategoryRepository;
import com.homeservices.repository.UserRepository;
import com.homeservices.security.JwtService;
import com.homeservices.service.FirebaseService.FirebasePhoneAuthResult;
import com.resend.core.exception.ResendException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;

  private final ProviderRepository providerRepository;

  private final PasswordEncoder passwordEncoder;

  private final JwtService jwtService;

  private final AuthenticationManager authenticationManager;

  private final UserDetailsService userDetailsService;

  private final EmailService emailService;

  private final ServiceCategoryRepository serviceCategoryRepository;

  private final FirebaseService firebaseService;

  // ─────────────────────────────────────────
  // REGISTER
  // ─────────────────────────────────────────
  @Transactional
  public AuthResponse register(RegisterRequest request) throws ResendException {

    if (userRepository.existsByEmail(request.getEmail())) {

      throw new BadRequestException("Email already in use");
    }

    User user = User.builder()

        .name(request.getName())

        .email(request.getEmail())

        .phone(request.getPhone())

        .password(passwordEncoder.encode(request.getPassword()))

        .role(request.getRole())

        .enabled(true)

        .build();

    // GENERATE OTP
    String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));

    LocalDateTime now = LocalDateTime.now();

    user.setEmailOtp(otp);

    user.setOtpExpiry(now.plusMinutes(10));

    user.setLastOtpSentAt(now);

    user.setOtpRequestCount(1);

    user.setOtpCountResetAt(now.plusHours(1));

    userRepository.save(user);

    // AUTO CREATE PROVIDER
    if (request.getRole() == Role.PROVIDER) {

      ServiceCategory category = serviceCategoryRepository.findByName(request.getServiceCategory())
          .orElseThrow(() -> new ResourceNotFoundException("Service category not found"));

      Provider provider = Provider.builder()

          .user(user)

          .experienceYears(request.getYearsOfExperience())

          .serviceArea(request.getServiceArea())

          .latitude(request.getLatitude())

          .longitude(request.getLongitude())

          .category(category)

          .verified(false)

          .active(true)

          .rating(0.0)

          .build();

      providerRepository.save(provider);
    }

    // SEND EMAIL
    emailService.sendOtpEmail(user.getEmail(), otp);

    return AuthResponse.builder()

        .userId(user.getId())

        .role(user.getRole())

        .build();
  }

  // ─────────────────────────────────────────
  // LOGIN
  // ─────────────────────────────────────────
  public AuthResponse login(LoginRequest request) {

    authenticationManager.authenticate(

        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

    User user = userRepository

        .findByEmail(request.getEmail())

        .orElseThrow(() -> new BadRequestException("User not found"));

    if (!user.isEmailVerified()) {

      throw new BadRequestException("Please verify your email first");
    }

    String accessToken = jwtService.generateToken(user.getEmail());

    String refreshToken = jwtService.generateRefreshToken(user.getEmail());

    return AuthResponse.builder()

        .accessToken(accessToken)

        .refreshToken(refreshToken)

        .userId(user.getId())

        .role(user.getRole())

        .build();
  }

  // ─────────────────────────────────────────
  // REFRESH TOKEN
  // ─────────────────────────────────────────
  public AuthResponse refresh(RefreshTokenRequest request) {

    String email = jwtService.extractUsername(request.getRefreshToken());

    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

    if (!jwtService.isTokenValid(request.getRefreshToken(), userDetails)) {

      throw new BadRequestException("Invalid or expired refresh token");
    }

    User user = userRepository

        .findByEmail(email)

        .orElseThrow(() -> new BadRequestException("User not found"));

    String newAccessToken = jwtService.generateToken(email);

    String newRefreshToken = jwtService.generateRefreshToken(email);

    return AuthResponse.builder()

        .accessToken(newAccessToken)

        .refreshToken(newRefreshToken)

        .userId(user.getId())

        .role(user.getRole())

        .build();
  }

  // ─────────────────────────────────────────
  // VERIFY EMAIL
  // ─────────────────────────────────────────
  @Transactional
  public void verifyEmail(String email, String otp) {

    User user = userRepository

        .findByEmail(email)

        .orElseThrow(() -> new BadRequestException("User not found"));

    // INVALID OTP
    if (user.getEmailOtp() == null ||

        !user.getEmailOtp().equals(otp)) {

      throw new BadRequestException("Invalid OTP");
    }

    // EXPIRED OTP
    if (user.getOtpExpiry() == null ||

        user.getOtpExpiry().isBefore(LocalDateTime.now())) {

      throw new BadRequestException("OTP expired");
    }

    // VERIFIED
    user.setEmailVerified(true);

    // CLEAR OTP DATA
    user.setEmailOtp(null);

    user.setOtpExpiry(null);

    user.setLastOtpSentAt(null);

    user.setOtpRequestCount(0);

    user.setOtpCountResetAt(null);

    userRepository.save(user);
  }

  // ─────────────────────────────────────────
  // RESEND OTP
  // ─────────────────────────────────────────
  @Transactional
  public void resendOtp(String email) throws ResendException {

    User user = userRepository

        .findByEmail(email)

        .orElseThrow(() -> new BadRequestException("User not found"));

    // ALREADY VERIFIED
    if (user.isEmailVerified()) {

      throw new BadRequestException("Email already verified");
    }

    LocalDateTime now = LocalDateTime.now();

    // RESET HOURLY LIMIT
    if (user.getOtpCountResetAt() == null ||

        now.isAfter(user.getOtpCountResetAt())) {

      user.setOtpRequestCount(0);

      user.setOtpCountResetAt(now.plusHours(1));
    }

    // MAX 5 OTP / HOUR
    if (user.getOtpRequestCount() >= 5) {

      throw new BadRequestException("Maximum OTP requests reached. Try again later.");
    }

    // 30 SECOND COOLDOWN
    if (user.getLastOtpSentAt() != null &&

        now.isBefore(user.getLastOtpSentAt().plusSeconds(30))) {

      long secondsLeft = Duration.between(now, user.getLastOtpSentAt().plusSeconds(30)).toSeconds();

      throw new BadRequestException(

          "Please wait " +

              secondsLeft +

              " seconds before requesting another OTP.");
    }

    // GENERATE NEW OTP
    String otp = String.valueOf(

        ThreadLocalRandom.current()

            .nextInt(100000, 999999));

    user.setEmailOtp(otp);

    user.setOtpExpiry(now.plusMinutes(10));

    user.setLastOtpSentAt(now);

    user.setOtpRequestCount(user.getOtpRequestCount() + 1);

    userRepository.save(user);

    // SEND EMAIL
    emailService.sendOtpEmail(user.getEmail(), otp);
  }

  // ─────────────────────────────────────────
  // LOGIN WITH FIREBASE PHONE TOKEN
  // ─────────────────────────────────────────

  /**
   * Authenticates a user using a Firebase Phone Auth ID token.
   *
   * Flow: 1. Verify the Firebase token → get (uid, phoneNumber). 2. Look up the user by
   * firebaseUid, then fall back to phoneNumber. 3. If no user exists yet, throw 404 (require prior
   * registration). 4. Link the firebaseUid to the user if not already linked. 5. Mark phone as
   * verified and issue the app's own JWT pair.
   */
  @Transactional
  public AuthResponse loginWithPhone(String firebaseIdToken) {

    // ── 1. Verify Firebase token ──────────────────────────────────────────
    FirebasePhoneAuthResult result;
    try {
      result = firebaseService.verifyPhoneToken(firebaseIdToken);
    } catch (Exception e) {
      throw new BadRequestException("Invalid or expired Firebase token: " + e.getMessage());
    }

    String uid = result.uid();
    String phoneNumber = result.phoneNumber();

    // ── 2. Find user: prefer firebaseUid lookup, fall back to phone ───────
    User user =
        userRepository.findByFirebaseUid(uid).or(() -> userRepository.findByPhone(phoneNumber))
            .orElseThrow(() -> new BadRequestException(
                "No account found for this phone number. Please register first."));

    // ── 3. Link Firebase UID if not yet stored ────────────────────────────
    if (user.getFirebaseUid() == null) {
      user.setFirebaseUid(uid);
    }

    // ── 4. Mark phone verified ────────────────────────────────────────────
    if (!user.isPhoneVerified()) {
      user.setPhoneVerified(true);
    }

    userRepository.save(user);

    // ── 5. Issue JWT pair ─────────────────────────────────────────────────
    String accessToken = jwtService.generateToken(user.getEmail());
    String refreshToken = jwtService.generateRefreshToken(user.getEmail());

    return AuthResponse.builder().accessToken(accessToken).refreshToken(refreshToken)
        .userId(user.getId()).role(user.getRole()).build();
  }

  // ─────────────────────────────────────────
  // FORGOT PASSWORD — send OTP
  // ─────────────────────────────────────────
  @Transactional
  public void forgotPassword(String email) throws ResendException {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("No account found with this email."));

    LocalDateTime now = LocalDateTime.now();

    // Rate-limit: max 3 OTPs per hour
    if (user.getOtpCountResetAt() != null && now.isBefore(user.getOtpCountResetAt())) {
      if (user.getOtpRequestCount() != null && user.getOtpRequestCount() >= 3) {
        throw new BadRequestException("Too many OTP requests. Please wait before trying again.");
      }
    } else {
      user.setOtpRequestCount(0);
      user.setOtpCountResetAt(now.plusHours(1));
    }

    String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
    user.setEmailOtp(otp);
    user.setOtpExpiry(now.plusMinutes(10));
    user.setLastOtpSentAt(now);
    user.setOtpRequestCount(user.getOtpRequestCount() + 1);
    userRepository.save(user);

    emailService.sendPasswordResetEmail(email, otp);
  }

  // ─────────────────────────────────────────
  // RESET PASSWORD — verify OTP + set new password
  // ─────────────────────────────────────────
  @Transactional
  public void resetPassword(String email, String otp, String newPassword) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("No account found with this email."));

    if (user.getEmailOtp() == null || !user.getEmailOtp().equals(otp)) {
      throw new BadRequestException("Invalid OTP.");
    }

    if (user.getOtpExpiry() == null || LocalDateTime.now().isAfter(user.getOtpExpiry())) {
      throw new BadRequestException("OTP has expired. Please request a new one.");
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    // Invalidate the OTP after use
    user.setEmailOtp(null);
    user.setOtpExpiry(null);
    userRepository.save(user);
  }
}
