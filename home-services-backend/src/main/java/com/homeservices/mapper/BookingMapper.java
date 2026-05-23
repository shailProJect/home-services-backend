package com.homeservices.mapper;

import com.homeservices.dto.response.BookingResponse;
import com.homeservices.entity.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

  public BookingResponse toBookingResponse(Booking booking) {
    return BookingResponse.builder().id(booking.getId()).userId(booking.getUser().getId())
        .userName(booking.getUser().getName())
        .providerServiceId(booking.getProviderService().getId())
        .serviceName(booking.getProviderService().getServiceName())
        .providerName(booking.getProviderService().getProvider().getUser().getName())
        .providerId(booking.getProviderService().getProvider().getId())
        .bookingDate(booking.getBookingDate()).startTime(booking.getStartTime())
        .endTime(booking.getEndTime()).status(booking.getStatus()).address(booking.getAddress())
        .notes(booking.getNotes()).createdAt(booking.getCreatedAt())
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
        .acceptedAt(booking.getAcceptedAt()).arrivedAt(booking.getArrivedAt()).build();
  }
}
