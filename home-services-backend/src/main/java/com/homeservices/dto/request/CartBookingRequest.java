package com.homeservices.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Request to create a combined booking for multiple services from the same provider. This is the
 * new "cart checkout" flow — user adds multiple services from one provider, then submits a single
 * booking request covering all of them.
 */
@Data
public class CartBookingRequest {

  /**
   * The provider whose services are being booked. All serviceIds must belong to this provider
   * (validated server-side).
   */
  @NotNull(message = "Provider ID is required")
  private UUID providerId;

  /**
   * List of service IDs selected from the provider's catalogue. Must contain at least one service.
   */
  @NotEmpty(message = "At least one service must be selected")
  private List<UUID> serviceIds;

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

  /** Set to true for urgent booking (₹199 surcharge, provider arrives within 30 min). */
  private boolean urgent = false;

  /** Optional: Cloudinary URL of a problem image for AI diagnosis. */
  private String problemImageUrl;
}
