package com.homeservices.entity;

import com.homeservices.entity.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_service_id", nullable = false)
  private ProviderService providerService;

  @Column(nullable = false)
  private LocalDate bookingDate;

  @Column(nullable = false)
  private LocalTime startTime;

  @Column(nullable = false)
  private LocalTime endTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private BookingStatus status = BookingStatus.PENDING;

  @Column(nullable = false)
  private String address;

  @Column(columnDefinition = "TEXT")
  private String notes;

  // ── Image diagnosis ──────────────────────────────────────────────────────
  /** Cloudinary URL of the problem image uploaded by user */
  private String problemImageUrl;

  /** AI-generated diagnosis from the uploaded image */
  @Column(columnDefinition = "TEXT")
  private String imageDiagnosis;

  // ── Cost estimation ──────────────────────────────────────────────────────
  /** Estimated minimum cost in INR */
  private Double estimatedCostMin;

  /** Estimated maximum cost in INR */
  private Double estimatedCostMax;

  // ── Urgent booking ───────────────────────────────────────────────────────
  /** True if this is an urgent booking (provider arrives within 30 minutes) */
  @Column(nullable = false)
  @Builder.Default
  private boolean urgent = false;

  /**
   * Deadline by which provider must arrive for urgent bookings. Set to createdAt + 30 minutes
   * automatically.
   */
  private LocalDateTime urgentArrivalDeadline;

  /** Urgent booking surcharge amount in INR */
  private Double urgentSurcharge;

  // ── Provider tracking ────────────────────────────────────────────────────
  /** Live latitude of provider (updated via tracking API) */
  private Double providerLatitude;

  /** Live longitude of provider (updated via tracking API) */
  private Double providerLongitude;

  /** When the provider location was last updated */
  private LocalDateTime providerLocationUpdatedAt;

  /** Estimated minutes until provider arrives */
  private Integer estimatedArrivalMinutes;

  /** Timestamp when provider accepted the booking */
  private LocalDateTime acceptedAt;

  /** Timestamp when provider actually arrived / started work */
  private LocalDateTime arrivedAt;

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime createdAt;
}
