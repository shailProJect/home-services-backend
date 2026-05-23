package com.homeservices.repository;

import com.homeservices.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

  List<Review> findByProviderId(UUID providerId);

  boolean existsByUserIdAndProviderId(UUID userId, UUID providerId);

  List<Review> findByUserId(UUID userId);

  @Query("SELECT AVG(r.rating) FROM Review r WHERE r.provider.id = :providerId")
  Double findAverageRatingByProviderId(@Param("providerId") UUID providerId);
}
