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
  private UUID providerId;
  private LocalDate bookingDate;
  private LocalTime startTime;
  private LocalTime endTime;
  private BookingStatus status;
  private String address;
  private String notes;
  private LocalDateTime createdAt;

  // ── Image & diagnosis ────────────────────────────────────────────────────
  private String problemImageUrl;
  private String imageDiagnosis;

  // ── Cost ────────────────────────────────────────────────────────────────
  private Double estimatedCostMin;
  private Double estimatedCostMax;

  // ── Urgent ──────────────────────────────────────────────────────────────
  private boolean urgent;
  private LocalDateTime urgentArrivalDeadline;
  private Double urgentSurcharge;

  // ── Live tracking ────────────────────────────────────────────────────────
  private Double providerLatitude;
  private Double providerLongitude;
  private LocalDateTime providerLocationUpdatedAt;
  private Integer estimatedArrivalMinutes;
  private LocalDateTime acceptedAt;
  private LocalDateTime arrivedAt;
  
  private String providerPhone;
  private String userPhone;
}
