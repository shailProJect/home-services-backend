package com.homeservices.repository;

import com.homeservices.entity.ProviderService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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
}
