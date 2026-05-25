package com.homeservices.dto.response;

import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusySlotResponse {

  private LocalTime startTime;
  private LocalTime endTime;
}
