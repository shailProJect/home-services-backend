package com.homeservices.controller;

import com.homeservices.dto.request.CategoryRequest;
import com.homeservices.dto.response.ApiResponse;
import com.homeservices.dto.response.ServiceCategoryResponse;
import com.homeservices.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * UPDATED CategoryController
 *
 * Public endpoints (no auth required): GET /categories — list all active categories (with their
 * fields) GET /categories/{id} — get a single category with fields
 *
 * Admin-only endpoints (mapped under /admin/categories via SecurityConfig): POST /admin/categories
 * — create a new category + fields PUT /admin/categories/{id} — update category + replace all
 * fields DELETE /admin/categories/{id} — delete category (soft: set active=false) PUT
 * /admin/categories/{id}/toggle-active — toggle visibility GET /admin/categories — list ALL
 * categories (incl. inactive)
 */
@RestController
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;

  // ── Public ──────────────────────────────────────────────────────────────

  /** Returns all ACTIVE categories with their dynamic fields. */
  @GetMapping("/categories")
  public ResponseEntity<ApiResponse<List<ServiceCategoryResponse>>> getActiveCategories() {
    return ResponseEntity.ok(ApiResponse.success(categoryService.getActiveCategories()));
  }

  /** Returns a single category (public, for provider registration form). */
  @GetMapping("/categories/{id}")
  public ResponseEntity<ApiResponse<ServiceCategoryResponse>> getCategory(@PathVariable UUID id) {
    return ResponseEntity.ok(ApiResponse.success(categoryService.getCategory(id)));
  }

  // ── Admin ────────────────────────────────────────────────────────────────

  /** Admin: list ALL categories including inactive ones. */
  @GetMapping("/admin/categories")
  public ResponseEntity<ApiResponse<List<ServiceCategoryResponse>>> getAllCategories() {
    return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories()));
  }

  /** Admin: create a new service category with optional dynamic fields. */
  @PostMapping("/admin/categories")
  public ResponseEntity<ApiResponse<ServiceCategoryResponse>> createCategory(
      @Valid @RequestBody CategoryRequest request) {
    return ResponseEntity.ok(ApiResponse.success(categoryService.createCategory(request),
        "Category created successfully"));
  }

  /** Admin: update category details and replace all its fields. */
  @PutMapping("/admin/categories/{id}")
  public ResponseEntity<ApiResponse<ServiceCategoryResponse>> updateCategory(@PathVariable UUID id,
      @Valid @RequestBody CategoryRequest request) {
    return ResponseEntity.ok(ApiResponse.success(categoryService.updateCategory(id, request),
        "Category updated successfully"));
  }

  /** Admin: soft-delete (deactivate) a category. */
  @DeleteMapping("/admin/categories/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
    categoryService.deleteCategory(id);
    return ResponseEntity.ok(ApiResponse.success(null, "Category deleted successfully"));
  }

  /** Admin: toggle active/inactive visibility. */
  @PutMapping("/admin/categories/{id}/toggle-active")
  public ResponseEntity<ApiResponse<ServiceCategoryResponse>> toggleActive(@PathVariable UUID id,
      @RequestParam boolean active) {
    return ResponseEntity.ok(ApiResponse.success(categoryService.toggleActive(id, active),
        active ? "Category activated" : "Category deactivated"));
  }
}
