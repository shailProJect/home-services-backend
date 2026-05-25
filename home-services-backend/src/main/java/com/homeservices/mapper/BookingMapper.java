package com.homeservices.mapper;

import com.homeservices.dto.response.BookingResponse;
import com.homeservices.entity.Booking;
import com.homeservices.entity.enums.BookingStatus;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

  public BookingResponse toBookingResponse(Booking booking) {

    String providerPhone = null;
    String userPhone = null;

    // Show phone numbers only after acceptance
    if (booking.getStatus() == BookingStatus.ACCEPTED
        || booking.getStatus() == BookingStatus.IN_PROGRESS
        || booking.getStatus() == BookingStatus.COMPLETED) {

      providerPhone = booking.getProviderService().getProvider().getUser().getPhone();

      userPhone = booking.getUser().getPhone();
    }

    return BookingResponse.builder().id(booking.getId())

        .userId(booking.getUser().getId()).userName(booking.getUser().getName())

        .providerServiceId(booking.getProviderService().getId())

        .serviceName(booking.getProviderService().getServiceName())

        .providerName(booking.getProviderService().getProvider().getUser().getName())

        .providerId(booking.getProviderService().getProvider().getId())

        .bookingDate(booking.getBookingDate()).startTime(booking.getStartTime())
        .endTime(booking.getEndTime())

        .status(booking.getStatus())

        .address(booking.getAddress()).notes(booking.getNotes())

        .createdAt(booking.getCreatedAt())

        // Image & diagnosis
        .problemImageUrl(booking.getProblemImageUrl()).imageDiagnosis(booking.getImageDiagnosis())

        // Cost
        .estimatedCostMin(booking.getEstimatedCostMin())
        .estimatedCostMax(booking.getEstimatedCostMax())

        // Urgent
        .urgent(booking.isUrgent()).urgentArrivalDeadline(booking.getUrgentArrivalDeadline())
        .urgentSurcharge(booking.getUrgentSurcharge())

        // Live tracking
        .providerLatitude(booking.getProviderLatitude())
        .providerLongitude(booking.getProviderLongitude())
        .providerLocationUpdatedAt(booking.getProviderLocationUpdatedAt())
        .estimatedArrivalMinutes(booking.getEstimatedArrivalMinutes())

        .acceptedAt(booking.getAcceptedAt()).arrivedAt(booking.getArrivedAt())

        // ADD THESE
        .providerPhone(providerPhone).userPhone(userPhone)

        .build();
  }
}
