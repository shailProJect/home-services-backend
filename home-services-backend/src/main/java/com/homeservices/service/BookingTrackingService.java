package com.homeservices.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.homeservices.dto.request.BookingRequest;
import com.homeservices.dto.request.ProviderLocationUpdateRequest;
import com.homeservices.dto.response.BookingResponse;
import com.homeservices.dto.response.BusySlotResponse;
import com.homeservices.entity.Booking;
import com.homeservices.entity.ProviderService;
import com.homeservices.entity.User;
import com.homeservices.entity.enums.BookingStatus;
import com.homeservices.exception.BadRequestException;
import com.homeservices.exception.ResourceNotFoundException;
import com.homeservices.mapper.BookingMapper;
import com.homeservices.repository.BookingRepository;
import com.homeservices.repository.ProviderRepository;
import com.homeservices.repository.ProviderServiceRepository;
import com.homeservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingTrackingService {

  private static final double URGENT_SURCHARGE_INR = 199.0;
  private static final int URGENT_ARRIVAL_MINUTES = 30;

  private final BookingRepository bookingRepository;
  private final ProviderServiceRepository providerServiceRepository;
  private final UserRepository userRepository;
  private final ProviderRepository providerRepository;
  private final BookingMapper bookingMapper;

  // ─────────────────────────────────────────────────────────────────────────
  // Create booking (normal or urgent)
  // ─────────────────────────────────────────────────────────────────────────

  @Transactional
  public BookingResponse createBooking(UUID userId, BookingRequest request) {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    ProviderService providerService =
        providerServiceRepository.findById(request.getProviderServiceId())
            .orElseThrow(() -> new ResourceNotFoundException("Provider service not found"));

    BookingStatus initialStatus =
        request.isUrgent() ? BookingStatus.URGENT_PENDING : BookingStatus.PENDING;

    LocalDateTime urgentDeadline =
        request.isUrgent() ? LocalDateTime.now().plusMinutes(URGENT_ARRIVAL_MINUTES) : null;

    Booking booking = Booking.builder().user(user).providerService(providerService)
        .bookingDate(request.getBookingDate()).startTime(request.getStartTime())
        .endTime(request.getEndTime()).address(request.getAddress()).notes(request.getNotes())
        .status(initialStatus).urgent(request.isUrgent()).urgentArrivalDeadline(urgentDeadline)
        .urgentSurcharge(request.isUrgent() ? URGENT_SURCHARGE_INR : null)
        .problemImageUrl(request.getProblemImageUrl()).acceptanceDeadline(LocalDateTime.now().plusMinutes(10)).build();

    validateProviderAvailability(providerService.getProvider().getId(), request.getBookingDate(),
        request.getStartTime(), request.getEndTime());
    bookingRepository.save(booking);
    log.info("Booking created: {} | urgent={}", booking.getId(), booking.isUrgent());

    return bookingMapper.toBookingResponse(booking);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Get tracking info for a booking
  // ─────────────────────────────────────────────────────────────────────────

  public BookingResponse getTrackingInfo(UUID userId, UUID bookingId) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

    if (!booking.getUser().getId().equals(userId)) {
      throw new BadRequestException("Access denied to this booking");
    }

    return bookingMapper.toBookingResponse(booking);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Provider updates their live location
  // ─────────────────────────────────────────────────────────────────────────

  @Transactional
  public void updateProviderLocation(UUID providerUserId, ProviderLocationUpdateRequest req) {

    Booking booking = bookingRepository.findById(req.getBookingId())
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

    // Validate caller is the provider for this booking
    UUID bookingProviderUserId = booking.getProviderService().getProvider().getUser().getId();

    if (!bookingProviderUserId.equals(providerUserId)) {
      throw new BadRequestException("Not authorised to update location for this booking");
    }

    booking.setProviderLatitude(req.getLatitude());
    booking.setProviderLongitude(req.getLongitude());
    booking.setProviderLocationUpdatedAt(LocalDateTime.now());

    // ETA: use provided value or compute from Haversine distance (~30km/h city speed)
    if (req.getEstimatedArrivalMinutes() != null) {
      booking.setEstimatedArrivalMinutes(req.getEstimatedArrivalMinutes());
    } else if (booking.getUser() != null) {
      // We'd need user lat/lng — store it in the future; skip for now
      booking.setEstimatedArrivalMinutes(null);
    }

    // Auto-upgrade status to PROVIDER_EN_ROUTE when provider starts sharing location
    if (booking.getStatus() == BookingStatus.CONFIRMED
        || booking.getStatus() == BookingStatus.URGENT_CONFIRMED) {
      booking.setStatus(BookingStatus.PROVIDER_EN_ROUTE);
    }

    bookingRepository.save(booking);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Provider marks arrival
  // ─────────────────────────────────────────────────────────────────────────

  @Transactional
  public BookingResponse markArrived(UUID providerUserId, UUID bookingId) {

    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

    UUID bookingProviderUserId = booking.getProviderService().getProvider().getUser().getId();

    if (!bookingProviderUserId.equals(providerUserId)) {
      throw new BadRequestException("Not authorised to update this booking");
    }

    // For urgent bookings — check if arrived within 30-min deadline
    if (booking.isUrgent() && booking.getUrgentArrivalDeadline() != null) {
      if (LocalDateTime.now().isAfter(booking.getUrgentArrivalDeadline())) {
        log.warn("Urgent booking {} — provider arrived LATE (after deadline)", bookingId);
        // Could trigger refund of surcharge — business logic hook here
      }
    }

    booking.setArrivedAt(LocalDateTime.now());
    booking.setStatus(BookingStatus.IN_PROGRESS);
    bookingRepository.save(booking);

    return bookingMapper.toBookingResponse(booking);
  }

  private void validateProviderAvailability(UUID providerId, LocalDate bookingDate,
      LocalTime startTime, LocalTime endTime) {

    boolean booked = bookingRepository
        .existsByProviderService_Provider_IdAndBookingDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
            providerId, bookingDate,
            List.of(BookingStatus.ACCEPTED, BookingStatus.ARRIVED, BookingStatus.IN_PROGRESS),
            endTime, startTime);

    if (booked) {
      throw new BadRequestException("Provider is already booked for this time slot");
    }
  }

  public List<BusySlotResponse> getBusySlots(UUID providerId, LocalDate bookingDate) {

    List<Booking> bookings = bookingRepository
        .findByProviderService_Provider_IdAndBookingDateAndStatusIn(providerId, bookingDate,
            List.of(BookingStatus.ACCEPTED, BookingStatus.ARRIVED, BookingStatus.IN_PROGRESS));

    return bookings.stream().map(
        b -> BusySlotResponse.builder().startTime(b.getStartTime()).endTime(b.getEndTime()).build())
        .toList();
  }
  
  @Transactional
  public void acceptBooking(UUID bookingId) {

      Booking booking = bookingRepository.findById(bookingId)
              .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

      booking.setStatus(BookingStatus.ACCEPTED);
      booking.setAcceptedAt(LocalDateTime.now());

      bookingRepository.save(booking);
  }
}
