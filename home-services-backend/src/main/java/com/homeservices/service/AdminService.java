package com.homeservices.service;

import com.homeservices.dto.response.BookingResponse;
import com.homeservices.dto.response.ProviderDetailResponse;
import com.homeservices.dto.response.ProviderResponse;
import com.homeservices.dto.response.UserResponse;
import com.homeservices.entity.Provider;
import com.homeservices.entity.User;
import com.homeservices.exception.ResourceNotFoundException;
import com.homeservices.mapper.BookingMapper;
import com.homeservices.mapper.ProviderMapper;
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
  private final ProviderMapper providerMapper;
  private final BookingMapper bookingMapper;
  private final ReviewRepository reviewRepository;
  private final com.homeservices.mapper.ReviewMapper reviewMapper;

  // ── List all providers ────────────────────────────────────────────────────

  public List<ProviderResponse> getAllProviders() {
    return providerRepository.findAll().stream().map(providerMapper::toProviderResponse).toList();
  }

  // ── Full provider detail (for admin verification review) ──────────────────

  /**
   * Returns the complete profile of a single provider including shop details,
   * uploaded documents, account status, and personal information.
   * Intended for the admin "review before verify" screen.
   */
  public ProviderDetailResponse getProviderDetail(UUID providerId) {
    Provider provider = providerRepository.findById(providerId)
        .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
    return toProviderDetailResponse(provider);
  }

  // ── Approve provider ──────────────────────────────────────────────────────

  @Transactional
  public ProviderDetailResponse approveProvider(UUID providerId) {
    Provider provider = providerRepository.findById(providerId)
        .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
    provider.setVerified(true);
    provider.setAdminNotes(null); // clear any previous rejection note
    providerRepository.save(provider);
    return toProviderDetailResponse(provider);
  }

  // ── Reject provider (with optional reason) ────────────────────────────────

  @Transactional
  public ProviderDetailResponse rejectProvider(UUID providerId, String notes) {
    Provider provider = providerRepository.findById(providerId)
        .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
    provider.setVerified(false);
    provider.setActive(false);
    if (notes != null && !notes.isBlank()) {
      provider.setAdminNotes(notes);
    }
    providerRepository.save(provider);
    return toProviderDetailResponse(provider);
  }

  // ── Toggle active ─────────────────────────────────────────────────────────

  @Transactional
  public ProviderResponse toggleProviderActive(UUID providerId, boolean active) {
    Provider provider = providerRepository.findById(providerId)
        .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
    provider.setActive(active);
    providerRepository.save(provider);
    return providerMapper.toProviderResponse(provider);
  }

  // ── Users ─────────────────────────────────────────────────────────────────

  public List<UserResponse> getAllUsers() {
    return userRepository.findAll().stream().map(this::toUserResponse).toList();
  }

  // ── Bookings ──────────────────────────────────────────────────────────────

  public List<BookingResponse> getAllBookings() {
    return bookingRepository.findAll().stream().map(bookingMapper::toBookingResponse).toList();
  }

  // ── Private mappers ───────────────────────────────────────────────────────

  private ProviderDetailResponse toProviderDetailResponse(Provider p) {
    User u = p.getUser();
    return ProviderDetailResponse.builder()
        .providerId(p.getId())
        .userId(u.getId())
        // user
        .name(u.getName())
        .email(u.getEmail())
        .phone(u.getPhone())
        .emailVerified(u.isEmailVerified())
        .phoneVerified(u.isPhoneVerified())
        .registeredAt(u.getCreatedAt())
        .userAddress(u.getAddress())
        // professional
        .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
        .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
        .experienceYears(p.getExperienceYears())
        .rating(p.getRating())
        // shop / location
        .shopName(p.getShopName())
        .shopAddress(p.getShopAddress())
        .serviceArea(p.getServiceArea())
        .latitude(p.getLatitude())
        .longitude(p.getLongitude())
        // documents
        .govtIdDocumentUrl(p.getGovtIdDocumentUrl())
        .businessCertificateUrl(p.getBusinessCertificateUrl())
        .addressProofUrl(p.getAddressProofUrl())
        // status
        .profilePhotoUrl(p.getUser().getProfilePhoto())
        .verified(p.isVerified())
        .active(p.isActive())
        .adminNotes(p.getAdminNotes())
        .build();
  }

  private UserResponse toUserResponse(User user) {
    return UserResponse.builder().id(user.getId()).name(user.getName()).email(user.getEmail())
        .phone(user.getPhone()).role(user.getRole()).enabled(user.isEnabled())
        .createdAt(user.getCreatedAt()).phoneVerified(user.isPhoneVerified())
        .emailVerified(user.isEmailVerified()).address(user.getAddress()).build();
  }
}
