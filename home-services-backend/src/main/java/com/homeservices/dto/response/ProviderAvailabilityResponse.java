package com.homeservices.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class ProviderAvailabilityResponse {
  private UUID id;
  private LocalDate availableDate;
  private LocalTime startTime;
  private LocalTime endTime;
}
