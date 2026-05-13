package com.homeservices.service;

import com.homeservices.dto.request.AvailabilityRequest;
import com.homeservices.dto.request.BookingStatusRequest;
import com.homeservices.dto.request.ProviderServiceRequest;
import com.homeservices.dto.response.BookingResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

  @Transactional
  public ProviderServiceResponse addService(UUID userId, ProviderServiceRequest request) {
    Provider provider = getProviderByUserId(userId);

    // Enforce phone verification before adding a service
    if (!provider.getUser().isPhoneVerified()) {
      throw new BadRequestException("Please verify your phone number before adding a service");
    }

    ServiceCategory category = serviceCategoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Category not found with id: " + request.getCategoryId()));

    ProviderService ps = ProviderService.builder().provider(provider).category(category)
        .serviceName(request.getServiceName()).price(request.getPrice())
        .durationMinutes(request.getDurationMinutes()).build();

    providerServiceRepository.save(ps);
    return providerMapper.toProviderServiceResponse(ps);
  }

  /**
   * Update an existing service belonging to the authenticated provider. Only the owner of the
   * service can edit it.
   */
  @Transactional
  public ProviderServiceResponse updateService(UUID userId, UUID serviceId,
      ProviderServiceRequest request) {
    Provider provider = getProviderByUserId(userId);

    ProviderService ps = providerServiceRepository.findById(serviceId)
        .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

    // Ensure this service belongs to the requesting provider
    if (!ps.getProvider().getId().equals(provider.getId())) {
      throw new BadRequestException("You do not have permission to edit this service");
    }

    ServiceCategory category = serviceCategoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Category not found with id: " + request.getCategoryId()));

    ps.setCategory(category);
    ps.setServiceName(request.getServiceName());
    ps.setPrice(request.getPrice());
    ps.setDurationMinutes(request.getDurationMinutes());

    providerServiceRepository.save(ps);
    return providerMapper.toProviderServiceResponse(ps);
  }

  public List<ProviderServiceResponse> getMyServices(UUID userId) {
    Provider provider = getProviderByUserId(userId);
    return providerServiceRepository.findByProviderIdAndActiveTrue(provider.getId()).stream()
        .map(providerMapper::toProviderServiceResponse).toList();
  }

  /** Toggle provider active / inactive status */
  @Transactional
  public ProviderResponse toggleActive(UUID userId, boolean active) {
    Provider provider = getProviderByUserId(userId);
    provider.setActive(active);
    providerRepository.save(provider);
    return providerMapper.toProviderResponse(provider);
  }

  /** Get provider's own profile */
  public ProviderResponse getProviderProfile(UUID userId) {
    Provider provider = getProviderByUserId(userId);
    return providerMapper.toProviderResponse(provider);
  }

  /** Update provider profile including shop name and address */
  @Transactional
  public ProviderResponse updateProviderProfile(UUID userId,
      com.homeservices.dto.request.ProviderProfileRequest request) {
    Provider provider = getProviderByUserId(userId);
    if (request.getExperienceYears() != null) {
      provider.setExperienceYears(request.getExperienceYears());
    }
    if (request.getServiceArea() != null && !request.getServiceArea().isBlank()) {
      provider.setServiceArea(request.getServiceArea());
    }
    if (request.getLatitude() != null) {
      provider.setLatitude(request.getLatitude());
    }
    if (request.getLongitude() != null) {
      provider.setLongitude(request.getLongitude());
    }
    if (request.getShopName() != null) {
      provider.setShopName(request.getShopName());
    }
    if (request.getShopAddress() != null) {
      provider.setShopAddress(request.getShopAddress());
    }
    providerRepository.save(provider);
    return providerMapper.toProviderResponse(provider);
  }

  @Transactional
  public void addAvailability(UUID userId, AvailabilityRequest request) {
    Provider provider = getProviderByUserId(userId);

    ProviderAvailability availability =
        ProviderAvailability.builder().provider(provider).availableDate(request.getAvailableDate())
            .startTime(request.getStartTime()).endTime(request.getEndTime()).build();

    availabilityRepository.save(availability);
  }

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

    if (!booking.getProviderService().getProvider().getId().equals(provider.getId())) {
      throw new BadRequestException("You do not have permission to update this booking");
    }

    BookingStatus newStatus = request.getStatus();
    if (newStatus != BookingStatus.CONFIRMED && newStatus != BookingStatus.REJECTED
        && newStatus != BookingStatus.COMPLETED) {
      throw new BadRequestException(
          "Provider can only set status to CONFIRMED, REJECTED, or COMPLETED");
    }

    booking.setStatus(newStatus);
    bookingRepository.save(booking);
    return bookingMapper.toBookingResponse(booking);
  }

  private Provider getProviderByUserId(UUID userId) {
    return providerRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found"));
  }

  // ── Document upload ───────────────────────────────────────────────────────

  /**
   * Saves up to three verification documents to disk and stores their URL paths on the Provider
   * entity. The admin can then view these via GET /admin/providers/{id}.
   *
   * Files are stored at: {upload.dir}/provider-docs/{providerId}/{docType}_{originalFilename} The
   * URL saved to DB is the relative path which can be served as a static resource.
   */
  @Transactional
  public ProviderDetailResponse uploadDocuments(UUID userId, MultipartFile govtId,
      MultipartFile businessCertificate, MultipartFile addressProof) {

    Provider provider = getProviderByUserId(userId);

    try {

      if (govtId != null && !govtId.isEmpty()) {
        String govtIdPublicId =
            cloudinaryService.uploadSecureDocument(govtId, "provider-documents/govt-id");

        provider.setGovtIdDocumentUrl(govtIdPublicId);
      }

      if (businessCertificate != null && !businessCertificate.isEmpty()) {
        String businessDocId = cloudinaryService.uploadSecureDocument(businessCertificate,
            "provider-documents/business");

        provider.setBusinessCertificateUrl(businessDocId);
      }

      if (addressProof != null && !addressProof.isEmpty()) {
        String addressDocId =
            cloudinaryService.uploadSecureDocument(addressProof, "provider-documents/address");

        provider.setAddressProofUrl(addressDocId);
      }

    } catch (Exception e) {
      throw new BadRequestException("Secure upload failed: " + e.getMessage());
    }

    providerRepository.save(provider);

    User u = provider.getUser();

    return ProviderDetailResponse.builder().providerId(provider.getId()).userId(u.getId())
        .name(u.getName()).email(u.getEmail()).phone(u.getPhone())
        .govtIdDocumentUrl(provider.getGovtIdDocumentUrl())
        .businessCertificateUrl(provider.getBusinessCertificateUrl())
        .addressProofUrl(provider.getAddressProofUrl()).verified(provider.isVerified()).build();
  }
}
