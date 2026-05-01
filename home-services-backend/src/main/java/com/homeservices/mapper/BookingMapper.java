package com.homeservices.mapper;

import com.homeservices.dto.response.BookingResponse;
import com.homeservices.entity.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse toBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUser().getId())
                .userName(booking.getUser().getName())
                .providerServiceId(booking.getProviderService().getId())
                .serviceName(booking.getProviderService().getServiceName())
                .providerName(booking.getProviderService().getProvider().getUser().getName())
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(booking.getStatus())
                .address(booking.getAddress())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
