package com.homeservices.repository;

import com.homeservices.entity.ProviderService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderServiceRepository extends JpaRepository<ProviderService, UUID> {

  List<ProviderService> findByProviderIdAndActiveTrue(UUID providerId);

  @Query("""
      SELECT ps FROM ProviderService ps
      JOIN ps.provider p
      JOIN ps.category c
      WHERE ps.active = true
        AND p.verified = true
        AND p.active = true
        AND LOWER(c.name) = LOWER(:categoryName)
      """)
  List<ProviderService> findByCategoryName(@Param("categoryName") String categoryName);

  /**
   * Returns the minimum (starting) price across all active services for a provider. Used to display
   * "Starting from ₹X" on provider cards.
   */
  @Query("""
      SELECT MIN(ps.price) FROM ProviderService ps
      WHERE ps.provider.id = :providerId AND ps.active = true
      """)
  Optional<BigDecimal> findMinPriceByProviderId(@Param("providerId") UUID providerId);
}
