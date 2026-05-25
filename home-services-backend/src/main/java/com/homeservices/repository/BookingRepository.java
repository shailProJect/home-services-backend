package com.homeservices.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.homeservices.entity.Booking;
import com.homeservices.entity.enums.BookingStatus;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

  List<Booking> findByUserId(UUID userId);

  @Query("""
      SELECT b FROM Booking b
      JOIN b.providerService ps
      JOIN ps.provider p
      WHERE p.id = :providerId
      ORDER BY b.createdAt DESC
      """)
  List<Booking> findByProviderId(@Param("providerId") UUID providerId);

  long countByProviderService_Provider_Id(UUID providerId);

  boolean existsByProviderService_Provider_IdAndBookingDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
      UUID providerId, LocalDate bookingDate, List<BookingStatus> statuses, LocalTime endTime,
      LocalTime startTime);

  List<Booking> findByProviderService_Provider_IdAndBookingDateAndStatusIn(UUID providerId,
      LocalDate bookingDate, List<BookingStatus> statuses);
}
