package com.homeservices.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class BookingRequest {

  @NotNull(message = "Provider service ID is required")
  private UUID providerServiceId;

  @NotNull(message = "Booking date is required")
  @FutureOrPresent(message = "Booking date must be today or in the future")
  private LocalDate bookingDate;

  @NotNull(message = "Start time is required")
  private LocalTime startTime;

  @NotNull(message = "End time is required")
  private LocalTime endTime;

  @NotBlank(message = "Address is required")
  private String address;

  private String notes;

  /**
   * Set to true for urgent booking. Provider is required to arrive within 30 minutes. A surcharge
   * of ₹199 is applied automatically.
   */
  private boolean urgent = false;

  /**
   * Optional: Cloudinary URL of a problem image already uploaded by the user. If provided, AI
   * diagnosis will be performed and attached to the booking.
   */
  private String problemImageUrl;
}
