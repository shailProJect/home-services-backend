package com.homeservices.service;

import com.homeservices.repository.PushSubscriptionRepository;
import com.homeservices.service.WebPushService;
import com.homeservices.dto.request.AvailabilityRequest;
import com.homeservices.dto.request.BookingStatusRequest;
import com.homeservices.dto.request.ProviderProfileRequest;
import com.homeservices.dto.request.ProviderServiceRequest;
import com.homeservices.dto.response.BookingResponse;
import com.homeservices.dto.response.ProviderAvailabilityResponse;
import com.homeservices.dto.response.ProviderDetailResponse;
import com.homeservices.dto.response.ProviderResponse;
import com.homeservices.dto.response.ProviderServiceResponse;
import com.homeservices.entity.*;
import com.homeservices.entity.enums.BookingStatus;
import com.homeservices.exception.BadRequestException;
import com.homeservices.exception.ResourceNotFoundException;
import com.homeservices.mapper.BookingMapper;
import com.homeservices.mapper.ProviderMapper;
import com.homeservices.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProviderManagementService {

  private final ProviderRepository providerRepository;
  private final ProviderServiceRepository providerServiceRepository;
  private final ServiceCategoryRepository serviceCategoryRepository;
  private final BookingRepository bookingRepository;
  private final ProviderAvailabilityRepository availabilityRepository;
  private final ProviderMapper providerMapper;
  private final BookingMapper bookingMapper;
  private final CloudinaryService cloudinaryService;
  private final PushSubscriptionRepository pushSubscriptionRepository;
  private final WebPushService webPushService;

  // ── Services ──────────────────────────────────────────────────────────────

  @Transactional
  public ProviderServiceResponse addService(UUID userId, ProviderServiceRequest request) {
    Provider provider = getProviderByUserId(userId);

//    if (!provider.getUser().isPhoneVerified())
//      throw new BadRequestException("Please verify your phone number before adding a service");

    ServiceCategory category =
        serviceCategoryRepository.findById(request.getCategoryId()).orElseThrow(
            () -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

    // FIX: prevent duplicate active service names for same provider
    boolean nameExists =
        providerServiceRepository.findByProviderIdAndActiveTrue(provider.getId()).stream()
            .anyMatch(s -> s.getServiceName().equalsIgnoreCase(request.getServiceName().trim()));
    if (nameExists)
      throw new BadRequestException(
          "You already have an active service named \"" + request.getServiceName().trim() + "\"");

    ProviderService ps = ProviderService.builder().provider(provider).category(category)
        .serviceName(request.getServiceName().trim())
        .description(request.getDescription())
        .price(request.getPrice())
        .durationMinutes(request.getDurationMinutes())
        .perDayRate(request.getPerDayRate())
        .perDayAllowed(request.isPerDayAllowed())
        .build();

    return providerMapper.toProviderServiceResponse(providerServiceRepository.save(ps));
  }

  @Transactional
  public ProviderServiceResponse updateService(UUID userId, UUID serviceId,
      ProviderServiceRequest request) {
    Provider provider = getProviderByUserId(userId);
    ProviderService ps = providerServiceRepository.findById(serviceId)
        .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

    if (!ps.getProvider().getId().equals(provider.getId()))
      throw new BadRequestException("You do not have permission to edit this service");

    ServiceCategory category =
        serviceCategoryRepository.findById(request.getCategoryId()).orElseThrow(
            () -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

    ps.setCategory(category);
    ps.setServiceName(request.getServiceName().trim());
    ps.setDescription(request.getDescription());
    ps.setPrice(request.getPrice());
    ps.setDurationMinutes(request.getDurationMinutes());
    ps.setPerDayRate(request.getPerDayRate());
    ps.setPerDayAllowed(request.isPerDayAllowed());
    return providerMapper.toProviderServiceResponse(providerServiceRepository.save(ps));
  }

  /**
   * Soft-delete a service (sets active=false). Existing bookings are untouched.
   */
  @Transactional
  public void deleteService(UUID userId, UUID serviceId) {
    Provider provider = getProviderByUserId(userId);
    ProviderService ps = providerServiceRepository.findById(serviceId)
        .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
    if (!ps.getProvider().getId().equals(provider.getId()))
      throw new BadRequestException("You do not have permission to delete this service");
    ps.setActive(false);
    providerServiceRepository.save(ps);
  }

  public List<ProviderServiceResponse> getMyServices(UUID userId) {
    Provider provider = getProviderByUserId(userId);
    return providerServiceRepository.findByProviderIdAndActiveTrue(provider.getId()).stream()
        .map(providerMapper::toProviderServiceResponse).toList();
  }

  // ── Profile ───────────────────────────────────────────────────────────────

  @Transactional
  public ProviderResponse toggleActive(UUID userId, boolean active) {
    Provider provider = getProviderByUserId(userId);
    provider.setActive(active);
    return providerMapper.toProviderResponse(providerRepository.save(provider));
  }

  public ProviderResponse getProviderProfile(UUID userId) {
    return providerMapper.toProviderResponse(getProviderByUserId(userId));
  }

  @Transactional
  public ProviderResponse updateProviderProfile(UUID userId, ProviderProfileRequest request) {
    Provider provider = getProviderByUserId(userId);
    if (request.getExperienceYears() != null)
      provider.setExperienceYears(request.getExperienceYears());
    if (request.getServiceArea() != null && !request.getServiceArea().isBlank())
      provider.setServiceArea(request.getServiceArea());
    if (request.getLatitude() != null)
      provider.setLatitude(request.getLatitude());
    if (request.getLongitude() != null)
      provider.setLongitude(request.getLongitude());
    if (request.getShopName() != null)
      provider.setShopName(request.getShopName());
    if (request.getShopAddress() != null)
      provider.setShopAddress(request.getShopAddress());
    return providerMapper.toProviderResponse(providerRepository.save(provider));
  }

  // ── Availability ──────────────────────────────────────────────────────────

  @Transactional
  public ProviderAvailabilityResponse addAvailability(UUID userId, AvailabilityRequest request) {
    Provider provider = getProviderByUserId(userId);

    // FIX: prevent overlapping slots for same date
    boolean overlap = availabilityRepository.findByProviderId(provider.getId()).stream()
        .filter(a -> a.getAvailableDate().equals(request.getAvailableDate()))
        .anyMatch(a -> request.getStartTime().isBefore(a.getEndTime())
            && request.getEndTime().isAfter(a.getStartTime()));
    if (overlap)
      throw new BadRequestException("This time slot overlaps with an existing availability.");

    ProviderAvailability availability =
        ProviderAvailability.builder().provider(provider).availableDate(request.getAvailableDate())
            .startTime(request.getStartTime()).endTime(request.getEndTime()).build();
    availabilityRepository.save(availability);
    return toAvailabilityResponse(availability);
  }

  public List<ProviderAvailabilityResponse> getMyAvailability(UUID userId) {
    Provider provider = getProviderByUserId(userId);
    return availabilityRepository.findByProviderId(provider.getId()).stream()
        .map(this::toAvailabilityResponse).toList();
  }

  @Transactional
  public void deleteAvailability(UUID userId, UUID slotId) {
    Provider provider = getProviderByUserId(userId);
    ProviderAvailability slot = availabilityRepository.findById(slotId)
        .orElseThrow(() -> new ResourceNotFoundException("Availability slot not found"));
    if (!slot.getProvider().getId().equals(provider.getId()))
      throw new BadRequestException("You do not have permission to delete this slot");
    availabilityRepository.delete(slot);
  }

  // ── Bookings ──────────────────────────────────────────────────────────────

  public List<BookingResponse> getMyBookings(UUID userId) {
    Provider provider = getProviderByUserId(userId);
    return bookingRepository.findByProviderId(provider.getId()).stream()
        .map(bookingMapper::toBookingResponse).toList();
  }

  @Transactional
  public BookingResponse updateBookingStatus(UUID userId, UUID bookingId,
      BookingStatusRequest request) {
    Provider provider = getProviderByUserId(userId);
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

    if (!booking.getProviderService().getProvider().getId().equals(provider.getId()))
      throw new BadRequestException("You do not have permission to update this booking");

    BookingStatus newStatus = request.getStatus();
    if (newStatus != BookingStatus.CONFIRMED && newStatus != BookingStatus.REJECTED
        && newStatus != BookingStatus.COMPLETED)
      throw new BadRequestException(
          "Provider can only set status to CONFIRMED, REJECTED, or COMPLETED");

    // FIX: guard invalid transitions
    BookingStatus current = booking.getStatus();
    if (current == BookingStatus.COMPLETED || current == BookingStatus.REJECTED)
      throw new BadRequestException("Cannot update a booking that is already " + current);
    if (current == BookingStatus.CONFIRMED && newStatus == BookingStatus.CONFIRMED)
      throw new BadRequestException("Booking is already CONFIRMED");

    booking.setStatus(newStatus);
    if (newStatus == BookingStatus.CONFIRMED) {
      booking.setAcceptedAt(java.time.LocalDateTime.now());
    }
    Booking saved = bookingRepository.save(booking);

    // Real-time push notification to user on status change
    try {
      var userSubs = pushSubscriptionRepository.findByUserId(booking.getUser().getId());
      String title, body;
      if (newStatus == BookingStatus.CONFIRMED) {
        title = "Booking Confirmed ✅";
        body = "Your booking for " + booking.getProviderService().getServiceName()
            + " has been confirmed by " + booking.getProviderService().getProvider().getUser().getName() + "!";
      } else if (newStatus == BookingStatus.REJECTED) {
        title = "Booking Rejected ❌";
        body = "Unfortunately, your booking for " + booking.getProviderService().getServiceName() + " was rejected.";
      } else {
        title = "Booking Completed 🎉";
        body = "Your " + booking.getProviderService().getServiceName() + " service is complete!";
      }
      for (var sub : userSubs) {
        webPushService.sendNotification(sub, title, body);
      }
    } catch (Exception e) {
      // Non-blocking
    }

    return bookingMapper.toBookingResponse(saved);
  }

  // ── Documents ─────────────────────────────────────────────────────────────

  @Transactional
  public ProviderDetailResponse uploadDocuments(UUID userId, MultipartFile govtId,
      MultipartFile businessCertificate, MultipartFile addressProof) {
    Provider provider = getProviderByUserId(userId);
    try {
      if (govtId != null && !govtId.isEmpty())
        provider.setGovtIdDocumentUrl(
            cloudinaryService.uploadSecureDocument(govtId, "provider-documents/govt-id"));
      if (businessCertificate != null && !businessCertificate.isEmpty())
        provider.setBusinessCertificateUrl(cloudinaryService
            .uploadSecureDocument(businessCertificate, "provider-documents/business"));
      if (addressProof != null && !addressProof.isEmpty())
        provider.setAddressProofUrl(
            cloudinaryService.uploadSecureDocument(addressProof, "provider-documents/address"));
    } catch (Exception e) {
      throw new BadRequestException("Document upload failed: " + e.getMessage());
    }
    providerRepository.save(provider);
    User u = provider.getUser();
    return ProviderDetailResponse.builder().providerId(provider.getId()).userId(u.getId())
        .name(u.getName()).email(u.getEmail()).phone(u.getPhone())
        .govtIdDocumentUrl(provider.getGovtIdDocumentUrl())
        .businessCertificateUrl(provider.getBusinessCertificateUrl())
        .addressProofUrl(provider.getAddressProofUrl()).verified(provider.isVerified())
        .active(provider.isActive()).build();
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private Provider getProviderByUserId(UUID userId) {
    return providerRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found"));
  }

  private ProviderAvailabilityResponse toAvailabilityResponse(ProviderAvailability a) {
    return ProviderAvailabilityResponse.builder().id(a.getId()).availableDate(a.getAvailableDate())
        .startTime(a.getStartTime()).endTime(a.getEndTime()).build();
  }
}