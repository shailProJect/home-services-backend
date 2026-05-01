package com.homeservices.repository;

import com.homeservices.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

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
}
