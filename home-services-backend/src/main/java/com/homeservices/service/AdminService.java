package com.homeservices.service;

import com.homeservices.dto.response.BookingResponse;
import com.homeservices.dto.response.ProviderDetailResponse;
import com.homeservices.dto.response.ProviderResponse;
import com.homeservices.dto.response.ReviewResponse;
import com.homeservices.dto.response.UserResponse;
import com.homeservices.entity.Provider;
import com.homeservices.entity.User;
import com.homeservices.exception.ResourceNotFoundException;
import com.homeservices.mapper.BookingMapper;
import com.homeservices.mapper.ProviderMapper;
import com.homeservices.mapper.ReviewMapper;
import com.homeservices.repository.BookingRepository;
import com.homeservices.repository.ProviderRepository;
import com.homeservices.repository.ReviewRepository;
import com.homeservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

  private final ProviderRepository providerRepository;
  private final UserRepository userRepository;
  private final BookingRepository bookingRepository;
  private final ReviewRepository reviewRepository;
  private final ProviderMapper providerMapper;
  private final BookingMapper bookingMapper;
  private final ReviewMapper reviewMapper;

  // ── Providers ─────────────────────────────────────────────────────────────

  public List<ProviderResponse> getAllProviders() {
    return providerRepository.findAll().stream().map(providerMapper::toProviderResponse).toList();
  }

  public ProviderDetailResponse getProviderDetail(UUID providerId) {
    return toProviderDetailResponse(providerRepository.findById(providerId)
        .orElseThrow(() -> new ResourceNotFoundException("Provider not found")));
  }

  @Transactional
  public ProviderDetailResponse approveProvider(UUID providerId) {
    Provider p = providerRepository.findById(providerId)
        .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
    p.setVerified(true);
    p.setActive(true);
    p.setAdminNotes(null);
    return toProviderDetailResponse(providerRepository.save(p));
  }

  @Transactional
  public ProviderDetailResponse rejectProvider(UUID providerId, String notes) {
    Provider p = providerRepository.findById(providerId)
        .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
    p.setVerified(false);
    p.setActive(false);
    if (notes != null && !notes.isBlank())
      p.setAdminNotes(notes);
    return toProviderDetailResponse(providerRepository.save(p));
  }

  @Transactional
  public ProviderResponse toggleProviderActive(UUID providerId, boolean active) {
    Provider p = providerRepository.findById(providerId)
        .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
    p.setActive(active);
    return providerMapper.toProviderResponse(providerRepository.save(p));
  }

  // ── Users ─────────────────────────────────────────────────────────────────

  public List<UserResponse> getAllUsers() {
    return userRepository.findAll().stream().map(this::toUserResponse).toList();
  }

  // ── Bookings ──────────────────────────────────────────────────────────────

  public List<BookingResponse> getAllBookings() {
    return bookingRepository.findAll().stream().map(bookingMapper::toBookingResponse).toList();
  }

  // ── Reviews (FIX: these were commented out / missing) ─────────────────────

  public List<ReviewResponse> getAllReviews() {
    return reviewRepository.findAll().stream().map(reviewMapper::toReviewResponse).toList();
  }

  public List<ReviewResponse> getReviewsByProvider(UUID providerId) {
    if (!providerRepository.existsById(providerId))
      throw new ResourceNotFoundException("Provider not found");
    return reviewRepository.findByProviderId(providerId).stream()
        .map(reviewMapper::toReviewResponse).toList();
  }

  @Transactional
  public void deleteReview(UUID reviewId) {
    if (!reviewRepository.existsById(reviewId))
      throw new ResourceNotFoundException("Review not found");

    // Recalculate provider rating after delete
    var review = reviewRepository.findById(reviewId).get();
    UUID providerId = review.getProvider().getId();
    reviewRepository.deleteById(reviewId);

    Double avg = reviewRepository.findAverageRatingByProviderId(providerId);
    Provider p = providerRepository.findById(providerId).orElse(null);
    if (p != null) {
      p.setRating(avg != null ? avg : 0.0);
      providerRepository.save(p);
    }
  }

  // ── Mappers ───────────────────────────────────────────────────────────────

  private ProviderDetailResponse toProviderDetailResponse(Provider p) {
    User u = p.getUser();
    return ProviderDetailResponse.builder().providerId(p.getId()).userId(u.getId())
        .name(u.getName()).email(u.getEmail()).phone(u.getPhone())
        .emailVerified(u.isEmailVerified()).phoneVerified(u.isPhoneVerified())
        .registeredAt(u.getCreatedAt()).userAddress(u.getAddress())
        .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
        .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
        .experienceYears(p.getExperienceYears()).rating(p.getRating()).shopName(p.getShopName())
        .shopAddress(p.getShopAddress()).serviceArea(p.getServiceArea()).latitude(p.getLatitude())
        .longitude(p.getLongitude()).govtIdDocumentUrl(p.getGovtIdDocumentUrl())
        .businessCertificateUrl(p.getBusinessCertificateUrl())
        .addressProofUrl(p.getAddressProofUrl()).profilePhotoUrl(u.getProfilePhoto())
        .verified(p.isVerified()).active(p.isActive()).adminNotes(p.getAdminNotes()).build();
  }

  private UserResponse toUserResponse(User u) {
    return UserResponse.builder().id(u.getId()).name(u.getName()).email(u.getEmail())
        .phone(u.getPhone()).role(u.getRole()).enabled(u.isEnabled()).createdAt(u.getCreatedAt())
        .phoneVerified(u.isPhoneVerified()).emailVerified(u.isEmailVerified())
        .address(u.getAddress()).profilePhoto(u.getProfilePhoto()).build();
  }
}
