package com.homeservices.dto.response;

import com.homeservices.entity.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Response for a combined multi-service (cart) booking. Contains a summary plus the list of
 * individual service bookings created.
 */
@Data
@Builder
public class CartBookingResponse {

  /** Unique group ID tying all individual bookings in this cart together */
  private UUID cartId;

  private UUID providerId;
  private String providerName;
  private UUID userId;
  private String userName;

  private LocalDate bookingDate;
  private LocalTime startTime;
  private LocalTime endTime;
  private String address;
  private String notes;

  private BookingStatus overallStatus;

  /** Total estimated cost across all services in this cart */
  private BigDecimal totalAmount;

  /** Individual booking entries — one per service */
  private List<BookingResponse> bookings;

  /** Number of services in this cart */
  private int serviceCount;

  private LocalDateTime createdAt;

  // ── Contact (revealed only after acceptance) ─────────────────────────────
  private String providerPhone;
  private String userPhone;
}
