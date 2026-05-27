package com.homeservices.repository;

import com.homeservices.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

  List<Review> findByProviderId(UUID providerId);

  boolean existsByUserIdAndProviderId(UUID userId, UUID providerId);

  List<Review> findByUserId(UUID userId);

  @Query("SELECT AVG(r.rating) FROM Review r WHERE r.provider.id = :providerId")
  Double findAverageRatingByProviderId(@Param("providerId") UUID providerId);

  /** Count total reviews for a provider */
  @Query("SELECT COUNT(r) FROM Review r WHERE r.provider.id = :providerId")
  Long countByProviderId(@Param("providerId") UUID providerId);

  /**
   * Returns the most recent high-rated (>=4) review comment for a provider. Used as highlighted
   * positive feedback on provider cards.
   */
  @Query("""
      SELECT r.comment FROM Review r
      WHERE r.provider.id = :providerId
        AND r.rating >= 4
        AND r.comment IS NOT NULL
        AND LENGTH(r.comment) > 10
      ORDER BY r.createdAt DESC
      LIMIT 1
      """)
  Optional<String> findHighlightedFeedback(@Param("providerId") UUID providerId);
}
