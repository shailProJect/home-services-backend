package com.homeservices.service;

import com.homeservices.dto.request.BookingRequest;
import com.homeservices.dto.request.ReviewRequest;
import com.homeservices.dto.request.UpdateProfileRequest;
import com.homeservices.dto.request.VerifyPhoneRequest;
import com.homeservices.dto.response.BookingResponse;
import com.homeservices.dto.response.ProviderResponse;
import com.homeservices.dto.response.ProviderServiceResponse;
import com.homeservices.dto.response.ReviewResponse;
import com.homeservices.dto.response.UserResponse;
import com.homeservices.entity.*;
import com.homeservices.exception.BadRequestException;
import com.homeservices.exception.ResourceNotFoundException;
import com.homeservices.mapper.BookingMapper;
import com.homeservices.mapper.ProviderMapper;
import com.homeservices.mapper.ReviewMapper;
import com.homeservices.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class UserService {

  private final ProviderRepository providerRepository;
  private final ProviderServiceRepository providerServiceRepository;
  private final BookingRepository bookingRepository;
  private final ReviewRepository reviewRepository;
  private final UserRepository userRepository;
  private final ProviderMapper providerMapper;
  private final BookingMapper bookingMapper;
  private final ReviewMapper reviewMapper;

  // ── Profile ────────────────────────────────────────────────────────────────

  public UserResponse getProfile(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    return toUserResponse(user);
  }

  @Transactional
  public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    if (request.getName() != null && !request.getName().isBlank()) {
      user.setName(request.getName());
    }
    if (request.getAddress() != null) {
      user.setAddress(request.getAddress());
    }
    userRepository.save(user);
    return toUserResponse(user);
  }

  // ── Phone OTP ──────────────────────────────────────────────────────────────

  @Transactional
  public String sendPhoneOtp(UUID userId, String phone) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    // Generate 6-digit OTP
    String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999));
    user.setPhone(phone);
    user.setPhoneOtp(otp);
    user.setPhoneOtpExpiry(LocalDateTime.now().plusMinutes(10));
    userRepository.save(user);

    // In production, integrate with SMS provider (Twilio, MSG91, etc.)
    // For now, return OTP directly so frontend can display it (dev mode)
    return otp;
  }

  @Transactional
  public UserResponse verifyPhone(UUID userId, VerifyPhoneRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    if (user.getPhoneOtp() == null || !user.getPhoneOtp().equals(request.getOtp())) {
      throw new BadRequestException("Invalid OTP");
    }
    if (user.getPhoneOtpExpiry() == null || LocalDateTime.now().isAfter(user.getPhoneOtpExpiry())) {
      throw new BadRequestException("OTP has expired");
    }

    user.setPhoneVerified(true);
    user.setPhoneOtp(null);
    user.setPhoneOtpExpiry(null);
    if (request.getPhone() != null && !request.getPhone().isBlank()) {
      user.setPhone(request.getPhone());
    }
    userRepository.save(user);
    return toUserResponse(user);
  }

  // ── Search ─────────────────────────────────────────────────────────────────

  public List<ProviderServiceResponse> searchByCategory(String category) {
    return providerServiceRepository.findByCategoryName(category).stream()
        .map(providerMapper::toProviderServiceResponse).toList();
  }

  public List<ProviderResponse> findNearbyProviders(double lat, double lng, double radius) {
    return providerRepository.findNearbyProviders(lat, lng, radius).stream()
        .map(providerMapper::toProviderResponse).toList();
  }

  // ── Bookings ───────────────────────────────────────────────────────────────

  @Transactional
  public BookingResponse createBooking(UUID userId, BookingRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    // Enforce phone verification before booking
    if (!user.isPhoneVerified()) {
      throw new BadRequestException("Please verify your phone number before making a booking");
    }

    ProviderService providerService =
        providerServiceRepository.findById(request.getProviderServiceId())
            .orElseThrow(() -> new ResourceNotFoundException("Provider service not found"));

    // Enforce provider active status
    if (!providerService.getProvider().isActive()) {
      throw new BadRequestException("This provider is currently not accepting bookings");
    }

    Booking booking = Booking.builder().user(user).providerService(providerService)
        .bookingDate(request.getBookingDate()).startTime(request.getStartTime())
        .endTime(request.getEndTime()).address(request.getAddress()).build();

    bookingRepository.save(booking);
    return bookingMapper.toBookingResponse(booking);
  }

  public List<BookingResponse> getMyBookings(UUID userId) {
    return bookingRepository.findByUserId(userId).stream().map(bookingMapper::toBookingResponse)
        .toList();
  }

  // ── Reviews ────────────────────────────────────────────────────────────────

  @Transactional
  public ReviewResponse addReview(UUID userId, ReviewRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    Provider provider = providerRepository.findById(request.getProviderId())
        .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

    Review review = Review.builder().user(user).provider(provider).rating(request.getRating())
        .comment(request.getComment()).build();

    reviewRepository.save(review);

    // Update provider average rating
    Double avg = reviewRepository.findAverageRatingByProviderId(provider.getId());
    provider.setRating(avg != null ? avg : 0.0);
    providerRepository.save(provider);

    return reviewMapper.toReviewResponse(review);
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private UserResponse toUserResponse(User user) {
    return UserResponse.builder().id(user.getId()).name(user.getName()).email(user.getEmail())
        .phone(user.getPhone()).role(user.getRole()).enabled(user.isEnabled())
        .createdAt(user.getCreatedAt()).phoneVerified(user.isPhoneVerified())
        .emailVerified(user.isEmailVerified()).address(user.getAddress()).build();
  }
}
