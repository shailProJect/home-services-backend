package com.homeservices.dto.response;

import com.homeservices.entity.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class BookingResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private UUID providerServiceId;
    private String serviceName;
    private String providerName;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BookingStatus status;
    private String address;
    private LocalDateTime createdAt;
}
