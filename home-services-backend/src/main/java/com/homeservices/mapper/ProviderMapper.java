package com.homeservices.mapper;

import com.homeservices.dto.response.ProviderResponse;
import com.homeservices.dto.response.ProviderServiceResponse;
import com.homeservices.entity.Provider;
import com.homeservices.entity.ProviderService;
import org.springframework.stereotype.Component;

@Component
public class ProviderMapper {

  /**
   * Basic provider mapping without enrichment queries. Use enrichedProviderResponse() when you need
   * totalReviews, startingPrice, highlightedFeedback.
   */
  public ProviderResponse toProviderResponse(Provider provider) {
    return ProviderResponse.builder().id(provider.getId()).userId(provider.getUser().getId())
        .name(provider.getUser().getName()).email(provider.getUser().getEmail())
        .phone(provider.getUser().getPhone()).experienceYears(provider.getExperienceYears())
        .serviceArea(provider.getServiceArea()).latitude(provider.getLatitude())
        .longitude(provider.getLongitude()).verified(provider.isVerified())
        .active(provider.isActive()).rating(provider.getRating())
        .categoryId(provider.getCategory() != null ? provider.getCategory().getId() : null)
        .categoryName(provider.getCategory() != null ? provider.getCategory().getName() : null)
        .shopName(provider.getShopName()).shopAddress(provider.getShopAddress())
        .profilePhotoUrl(provider.getUser().getProfilePhoto()).build();
  }

  /**
   * Enriched provider mapping with trust and affordability signals. Receives pre-fetched enrichment
   * data to avoid N+1 queries.
   */
  public ProviderResponse toEnrichedProviderResponse(Provider provider, Long totalReviews,
      java.math.BigDecimal startingPrice, String highlightedFeedback) {

    return ProviderResponse.builder().id(provider.getId()).userId(provider.getUser().getId())
        .name(provider.getUser().getName()).email(provider.getUser().getEmail())
        .phone(provider.getUser().getPhone()).experienceYears(provider.getExperienceYears())
        .serviceArea(provider.getServiceArea()).latitude(provider.getLatitude())
        .longitude(provider.getLongitude()).verified(provider.isVerified())
        .active(provider.isActive()).rating(provider.getRating())
        .categoryId(provider.getCategory() != null ? provider.getCategory().getId() : null)
        .categoryName(provider.getCategory() != null ? provider.getCategory().getName() : null)
        .shopName(provider.getShopName()).shopAddress(provider.getShopAddress())
        .profilePhotoUrl(provider.getUser().getProfilePhoto())
        // Enriched fields
        .totalReviews(totalReviews != null ? totalReviews : 0L).startingPrice(startingPrice)
        .highlightedFeedback(highlightedFeedback).build();
  }

  public ProviderServiceResponse toProviderServiceResponse(ProviderService ps) {
    return ProviderServiceResponse.builder().id(ps.getId()).providerId(ps.getProvider().getId())
        .providerName(ps.getProvider().getUser().getName()).categoryId(ps.getCategory().getId())
        .categoryName(ps.getCategory().getName()).serviceName(ps.getServiceName())
        .price(ps.getPrice()).durationMinutes(ps.getDurationMinutes()).active(ps.isActive())
        .description(ps.getDescription()).perDayAllowed(ps.isPerDayAllowed())
        .perDayRate(ps.getPerDayRate()).build();
  }
}
