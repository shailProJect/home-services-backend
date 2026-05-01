package com.homeservices.service;

import com.homeservices.dto.request.BookingRequest;
import com.homeservices.dto.request.ReviewRequest;
import com.homeservices.dto.response.BookingResponse;
import com.homeservices.dto.response.ProviderResponse;
import com.homeservices.dto.response.ProviderServiceResponse;
import com.homeservices.dto.response.ReviewResponse;
import com.homeservices.entity.*;
import com.homeservices.exception.ResourceNotFoundException;
import com.homeservices.mapper.BookingMapper;
import com.homeservices.mapper.ProviderMapper;
import com.homeservices.mapper.ReviewMapper;
import com.homeservices.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final ProviderRepository providerRepository;
    private final ProviderServiceRepository providerServiceRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProviderMapper providerMapper;
    private final BookingMapper bookingMapper;
    private final ReviewMapper reviewMapper;

    public List<ProviderServiceResponse> searchByCategory(String category) {
        return providerServiceRepository.findByCategoryName(category)
                .stream()
                .map(providerMapper::toProviderServiceResponse)
                .toList();
    }

    public List<ProviderResponse> findNearbyProviders(double lat, double lng, double radius) {
        return providerRepository.findNearbyProviders(lat, lng, radius)
                .stream()
                .map(providerMapper::toProviderResponse)
                .toList();
    }

    @Transactional
    public BookingResponse createBooking(UUID userId, BookingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ProviderService providerService = providerServiceRepository.findById(request.getProviderServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider service not found"));

        Booking booking = Booking.builder()
                .user(user)
                .providerService(providerService)
                .bookingDate(request.getBookingDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .address(request.getAddress())
                .build();

        bookingRepository.save(booking);
        return bookingMapper.toBookingResponse(booking);
    }

    public List<BookingResponse> getMyBookings(UUID userId) {
        return bookingRepository.findByUserId(userId)
                .stream()
                .map(bookingMapper::toBookingResponse)
                .toList();
    }

    @Transactional
    public ReviewResponse addReview(UUID userId, ReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Provider provider = providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

        Review review = Review.builder()
                .user(user)
                .provider(provider)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        reviewRepository.save(review);

        // Update provider average rating
        Double avg = reviewRepository.findAverageRatingByProviderId(provider.getId());
        provider.setRating(avg != null ? avg : 0.0);
        providerRepository.save(provider);

        return reviewMapper.toReviewResponse(review);
    }
}
