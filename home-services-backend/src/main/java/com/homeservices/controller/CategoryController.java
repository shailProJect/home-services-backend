package com.homeservices.controller;

import com.homeservices.dto.response.ApiResponse;
import com.homeservices.dto.response.ServiceCategoryResponse;
import com.homeservices.entity.ServiceCategory;
import com.homeservices.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GET /categories — public endpoint (no auth required)
 * Returns all seeded service categories with their UUIDs so the
 * frontend can populate the category dropdown and send a valid categoryId.
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ServiceCategoryRepository serviceCategoryRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceCategoryResponse>>> getAllCategories() {
        List<ServiceCategoryResponse> categories = serviceCategoryRepository.findAll()
                .stream()
                .map(c -> ServiceCategoryResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
}