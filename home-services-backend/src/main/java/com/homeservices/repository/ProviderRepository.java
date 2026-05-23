package com.homeservices.repository;

import com.homeservices.entity.Provider;
import com.homeservices.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, UUID> {

    Optional<Provider> findByUser(User user);

    Optional<Provider> findByUserId(UUID userId);

    List<Provider> findByVerifiedTrueAndActiveTrue();

    /**
     * Finds providers within a given radius (km) using the Haversine formula.
     * 6371 is Earth's radius in km.
     */
    @Query(value = """
            SELECT p.* FROM providers p
            WHERE p.verified = true AND p.active = true
              AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL
              AND (
                6371 * acos(
                  cos(radians(:lat)) * cos(radians(p.latitude))
                  * cos(radians(p.longitude) - radians(:lng))
                  + sin(radians(:lat)) * sin(radians(p.latitude))
                )
              ) <= :radius
            """, nativeQuery = true)
    List<Provider> findNearbyProviders(@Param("lat") double lat,
                                       @Param("lng") double lng,
                                       @Param("radius") double radius);
    
    List<Provider> findTop5ByVerifiedTrueAndActiveTrue();
    
}
