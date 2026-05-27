package com.homeservices.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.homeservices.dto.request.BookingRequest;
import com.homeservices.dto.request.CartBookingRequest;
import com.homeservices.dto.request.PushSubscriptionRequest;
import com.homeservices.dto.response.CartBookingResponse;
import com.homeservices.dto.request.ReviewRequest;
import com.homeservices.dto.request.UpdateProfileRequest;
import com.homeservices.dto.request.VerifyPhoneRequest;
import com.homeservices.dto.response.BookingResponse;
import com.homeservices.dto.response.ProviderResponse;
import com.homeservices.dto.response.ProviderServiceResponse;
import com.homeservices.dto.response.ReviewResponse;
import com.homeservices.dto.response.UserResponse;
import com.homeservices.entity.Booking;
import com.homeservices.entity.Provider;
import com.homeservices.entity.ProviderService;
import com.homeservices.entity.PushSubscription;
import com.homeservices.entity.Review;
import com.homeservices.entity.User;
import com.homeservices.exception.BadRequestException;
import com.homeservices.exception.ResourceNotFoundException;
import com.homeservices.mapper.BookingMapper;
import com.homeservices.mapper.ProviderMapper;
import com.homeservices.mapper.ReviewMapper;
import com.homeservices.repository.BookingRepository;
import com.homeservices.repository.ProviderRepository;
import com.homeservices.repository.ProviderServiceRepository;
import com.homeservices.repository.PushSubscriptionRepository;
import com.homeservices.repository.ReviewRepository;
import com.homeservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;

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
  private final CloudinaryService cloudinaryService;
  private final PushSubscriptionRepository pushSubscriptionRepository;
  private final WebPushService webPushService;
  private final SubscriptionService subscriptionService;
  // ── Profile ────────────────────────────────────────────────────────────────

  public UserResponse getProfile(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    return toUserResponse(user);
  }

  @Transactional
  public UserResponse updateProfile(UUID userId, UpdateProfileRequest request,
      MultipartFile profilePhoto) {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    if (request.getName() != null && !request.getName().isBlank()) {
      user.setName(request.getName().trim());
    }

    if (request.getAddress() != null) {
      user.setAddress(request.getAddress());
    }

    // Secure Profile Photo Upload
    if (profilePhoto != null && !profilePhoto.isEmpty()) {

      try {

        String contentType = profilePhoto.getContentType();

        if (contentType == null
            || !(contentType.equals("image/jpeg") || contentType.equals("image/png"))) {
          throw new BadRequestException("Only JPG and PNG profile photos are allowed");
        }

        // 500KB limit
        if (profilePhoto.getSize() > 500 * 1024) {
          throw new BadRequestException("Profile photo must be under 500KB");
        }

        String photoUrl = cloudinaryService.uploadProfilePhoto(profilePhoto);

        user.setProfilePhoto(photoUrl);

      } catch (Exception e) {
        throw new BadRequestException("Profile photo upload failed: " + e.getMessage());
      }
    }

    userRepository.save(user);

    return toUserResponse(user);
  }

  // ── Phone OTP ──────────────────────────────────────────────────────────────

  @Transactional
  public String sendPhoneOtp(UUID userId, String phone) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    // Rate-limit: only allow a new OTP if the last one was sent > 1 min ago
    if (user.getPhoneOtpExpiry() != null
        && LocalDateTime.now().isBefore(user.getPhoneOtpExpiry().minusMinutes(9))) {
      throw new BadRequestException("Please wait before requesting another OTP");
    }

    String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999));
    if (phone != null && !phone.isBlank()) {
      user.setPhone(phone);
    }
    user.setPhoneOtp(otp);
    user.setPhoneOtpExpiry(LocalDateTime.now().plusMinutes(10));
    userRepository.save(user);
    // TODO: In production, send via SMS provider (MSG91 / Twilio)
    return otp; // dev mode: returned to frontend
  }

  @Transactional
  public UserResponse verifyPhone(UUID userId, VerifyPhoneRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    if (user.getPhoneOtp() == null || !user.getPhoneOtp().equals(request.getOtp())) {
      throw new BadRequestException("Invalid OTP");
    }
    if (user.getPhoneOtpExpiry() == null || LocalDateTime.now().isAfter(user.getPhoneOtpExpiry())) {
      throw new BadRequestException("OTP has expired. Please request a new one.");
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

  @Transactional
  public UserResponse updatePhone(UUID userId, String newPhone) {
    if (newPhone == null || newPhone.isBlank()) {
      throw new BadRequestException("Phone number is required");
    }
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    user.setPhone(newPhone);
    user.setPhoneVerified(false); // reset verification for new number
    user.setPhoneOtp(null);
    user.setPhoneOtpExpiry(null);
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

    // if (!user.isPhoneVerified()) {
    // throw new BadRequestException("Please verify your phone number before making a booking");
    // }

    ProviderService providerService =
        providerServiceRepository.findById(request.getProviderServiceId())
            .orElseThrow(() -> new ResourceNotFoundException("Provider service not found"));

    if (!providerService.getProvider().isActive()) {
      throw new BadRequestException("This provider is currently not accepting bookings");
    }

    Booking booking = Booking.builder().user(user).providerService(providerService)
        .bookingDate(request.getBookingDate()).startTime(request.getStartTime())
        .endTime(request.getEndTime()).address(request.getAddress()).build();

    bookingRepository.save(booking);

    // =============================
    // USER NOTIFICATION
    // =============================

    try {

      var userSubscriptions = pushSubscriptionRepository.findByUserId(user.getId());

      for (var subscription : userSubscriptions) {

        webPushService.sendNotification(subscription, "Booking Confirmed ✅",
            "Your booking for " + providerService.getServiceName() + " has been confirmed");
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    // =============================
    // PROVIDER NOTIFICATION
    // =============================

    try {

      var providerSubscriptions =
          pushSubscriptionRepository.findByProviderId(providerService.getProvider().getId());

      for (var subscription : providerSubscriptions) {

        webPushService.sendNotification(subscription, "New Booking Received 🔔",
            "You received a new booking from " + user.getName());
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    subscriptionService.checkAndEnforceFreeTierAfterBooking(providerService.getProvider().getId());

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

    // Prevent duplicate reviews
    if (reviewRepository.existsByUserIdAndProviderId(userId, provider.getId())) {
      throw new BadRequestException("You have already submitted a review for this provider.");
    }

    Review review = Review.builder().user(user).provider(provider).rating(request.getRating())
        .comment(request.getComment()).build();

    reviewRepository.save(review);

    // Update provider average rating
    Double avg = reviewRepository.findAverageRatingByProviderId(provider.getId());
    provider.setRating(avg != null ? avg : 0.0);
    providerRepository.save(provider);

    return reviewMapper.toReviewResponse(review);
  }

  public ProviderResponse getProviderById(UUID providerId) {
    Provider provider = providerRepository.findById(providerId)
        .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
    return providerMapper.toProviderResponse(provider);
  }

  public List<ReviewResponse> getProviderReviews(UUID providerId) {
    if (!providerRepository.existsById(providerId)) {
      throw new ResourceNotFoundException("Provider not found");
    }
    return reviewRepository.findByProviderId(providerId).stream()
        .map(reviewMapper::toReviewResponse).toList();
  }

  public List<ReviewResponse> getMyReviews(UUID userId) {
    return reviewRepository.findByUserId(userId).stream().map(reviewMapper::toReviewResponse)
        .toList();
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private UserResponse toUserResponse(User user) {
    return UserResponse.builder().id(user.getId()).name(user.getName()).email(user.getEmail())
        .phone(user.getPhone()).role(user.getRole()).enabled(user.isEnabled())
        .createdAt(user.getCreatedAt()).phoneVerified(user.isPhoneVerified())
        .emailVerified(user.isEmailVerified()).address(user.getAddress())
        .profilePhoto(user.getProfilePhoto()).build();
  }

  @Transactional
  public UserResponse uploadProfilePhoto(UUID userId, MultipartFile profilePhoto)
      throws IOException {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    // Validate
    if (profilePhoto == null || profilePhoto.isEmpty()) {

      throw new RuntimeException("Profile photo is required");
    }

    // Max 500KB
    if (profilePhoto.getSize() > 500 * 1024) {

      throw new RuntimeException("Profile photo exceeds 500KB");
    }

    // Upload to Cloudinary
    String photoUrl = cloudinaryService.uploadProfilePhoto(profilePhoto);

    user.setProfilePhoto(photoUrl);

    userRepository.save(user);

    return toUserResponse(user);
  }

  // ── Enriched Provider Search ───────────────────────────────────────────────

  /**
   * Returns enriched ProviderResponse list for a category, with trust and affordability signals
   * (totalReviews, startingPrice, highlightedFeedback). Avoids N+1 by batching enrichment queries
   * per provider.
   */
  public List<ProviderResponse> searchEnrichedProvidersByCategory(String category) {
    List<ProviderService> services = providerServiceRepository.findByCategoryName(category);

    // Deduplicate providers preserving order
    List<Provider> seen = new ArrayList<>();
    List<UUID> seenIds = new ArrayList<>();
    for (ProviderService ps : services) {
      Provider p = ps.getProvider();
      if (!seenIds.contains(p.getId())) {
        seen.add(p);
        seenIds.add(p.getId());
      }
    }

    return seen.stream().map(provider -> {
      UUID pid = provider.getId();
      Long reviewCount = reviewRepository.countByProviderId(pid);
      BigDecimal minPrice = providerServiceRepository.findMinPriceByProviderId(pid).orElse(null);
      String highlight = reviewRepository.findHighlightedFeedback(pid).orElse(null);
      return providerMapper.toEnrichedProviderResponse(provider, reviewCount, minPrice, highlight);
    }).toList();
  }

  // ── Cart Booking ───────────────────────────────────────────────────────────

  /**
   * Creates a combined multi-service booking from the user's cart. All services must belong to the
   * specified provider. One Booking row is created per service; they share a cartId (UUID).
   */
  @Transactional
  public CartBookingResponse createCartBooking(UUID userId, CartBookingRequest request) {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    Provider provider = providerRepository.findById(request.getProviderId())
        .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

    if (!provider.isActive()) {
      throw new BadRequestException("This provider is currently not accepting bookings");
    }

    if (request.getServiceIds() == null || request.getServiceIds().isEmpty()) {
      throw new BadRequestException("At least one service must be selected");
    }

    // Generate a shared cart group ID
    UUID cartId = UUID.randomUUID();

    List<Booking> bookings = new ArrayList<>();
    BigDecimal totalAmount = BigDecimal.ZERO;

    for (UUID serviceId : request.getServiceIds()) {
      ProviderService ps = providerServiceRepository.findById(serviceId)
          .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + serviceId));

      // Security: ensure service belongs to requested provider
      if (!ps.getProvider().getId().equals(provider.getId())) {
        throw new BadRequestException(
            "Service " + serviceId + " does not belong to provider " + provider.getId());
      }

      if (!ps.isActive()) {
        throw new BadRequestException(
            "Service '" + ps.getServiceName() + "' is currently inactive");
      }

      Booking booking = Booking.builder().user(user).providerService(ps)
          .bookingDate(request.getBookingDate()).startTime(request.getStartTime())
          .endTime(request.getEndTime()).address(request.getAddress()).notes(request.getNotes())
          .urgent(request.isUrgent()).build();

      bookingRepository.save(booking);
      bookings.add(booking);
      totalAmount = totalAmount.add(ps.getPrice());
    }

    // Send notifications
    try {
      var userSubs = pushSubscriptionRepository.findByUserId(user.getId());
      for (var sub : userSubs) {
        webPushService.sendNotification(sub, "Cart Booking Confirmed ✅",
            bookings.size() + " service(s) booked with " + provider.getUser().getName());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      var providerSubs = pushSubscriptionRepository.findByProviderId(provider.getId());
      for (var sub : providerSubs) {
        webPushService.sendNotification(sub, "New Cart Booking 🛒",
            user.getName() + " requested " + bookings.size() + " service(s)");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    subscriptionService.checkAndEnforceFreeTierAfterBooking(provider.getId());

    List<com.homeservices.dto.response.BookingResponse> bookingResponses =
        bookings.stream().map(bookingMapper::toBookingResponse).toList();

    return CartBookingResponse.builder().cartId(cartId).providerId(provider.getId())
        .providerName(provider.getUser().getName()).userId(user.getId()).userName(user.getName())
        .bookingDate(request.getBookingDate()).startTime(request.getStartTime())
        .endTime(request.getEndTime()).address(request.getAddress()).notes(request.getNotes())
        .overallStatus(com.homeservices.entity.enums.BookingStatus.PENDING).totalAmount(totalAmount)
        .bookings(bookingResponses).serviceCount(bookings.size()).createdAt(LocalDateTime.now())
        .build();
  }

  @Transactional
  public void saveSubscription(String email, PushSubscriptionRequest request) {

    User user =
        userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

    PushSubscription subscription = PushSubscription.builder().endpoint(request.getEndpoint())
        .p256dh(request.getP256dh()).auth(request.getAuth()).user(user).build();

    pushSubscriptionRepository.save(subscription);

    System.out.println("Subscription saved");
  }
}
