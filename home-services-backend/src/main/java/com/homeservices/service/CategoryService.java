package com.homeservices.service;

import com.homeservices.dto.request.CategoryFieldRequest;
import com.homeservices.dto.request.CategoryRequest;
import com.homeservices.dto.response.CategoryFieldResponse;
import com.homeservices.dto.response.ServiceCategoryResponse;
import com.homeservices.entity.CategoryField;
import com.homeservices.entity.ServiceCategory;
import com.homeservices.exception.ResourceNotFoundException;
import com.homeservices.repository.CategoryFieldRepository;
import com.homeservices.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class CategoryService {

  private final ServiceCategoryRepository categoryRepository;
  private final CategoryFieldRepository fieldRepository;

  // ── Read ─────────────────────────────────────────────────────────────────

  public List<ServiceCategoryResponse> getActiveCategories() {
    return categoryRepository.findAllByActiveTrueOrderByDisplayNameAsc().stream()
        .map(this::toResponse).toList();
  }

  public List<ServiceCategoryResponse> getAllCategories() {
    return categoryRepository.findAllByOrderByDisplayNameAsc().stream().map(this::toResponse)
        .toList();
  }

  public ServiceCategoryResponse getCategory(UUID id) {
    return toResponse(findById(id));
  }

  // ── Create ───────────────────────────────────────────────────────────────

  @Transactional
  public ServiceCategoryResponse createCategory(CategoryRequest req) {
    String name = req.getName().toUpperCase().replace(" ", "_");
    if (categoryRepository.existsByName(name)) {
      throw new IllegalArgumentException("Category '" + name + "' already exists.");
    }

    ServiceCategory category =
        ServiceCategory.builder().name(name).displayName(req.getDisplayName())
            .description(req.getDescription()).icon(req.getIcon()).active(req.isActive()).build();

    ServiceCategory saved = categoryRepository.save(category);
    attachFields(saved, req.getFields());
    return toResponse(categoryRepository.save(saved));
  }

  // ── Update ───────────────────────────────────────────────────────────────

  @Transactional
  public ServiceCategoryResponse updateCategory(UUID id, CategoryRequest req) {
    ServiceCategory category = findById(id);

    String name = req.getName().toUpperCase().replace(" ", "_");
    // Allow rename only if the new name doesn't clash with another category
    if (!category.getName().equals(name) && categoryRepository.existsByName(name)) {
      throw new IllegalArgumentException("Category '" + name + "' already exists.");
    }

    category.setName(name);
    category.setDisplayName(req.getDisplayName());
    category.setDescription(req.getDescription());
    category.setIcon(req.getIcon());
    category.setActive(req.isActive());

    // Replace all fields: remove existing, add new ones
    fieldRepository.deleteAllByCategoryId(id);
    category.getFields().clear();
    attachFields(category, req.getFields());

    return toResponse(categoryRepository.save(category));
  }

  // ── Delete / Toggle ──────────────────────────────────────────────────────

  @Transactional
  public void deleteCategory(UUID id) {
    ServiceCategory category = findById(id);
    category.setActive(false); // soft delete
    categoryRepository.save(category);
  }

  @Transactional
  public ServiceCategoryResponse toggleActive(UUID id, boolean active) {
    ServiceCategory category = findById(id);
    category.setActive(active);
    return toResponse(categoryRepository.save(category));
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private ServiceCategory findById(UUID id) {
    return categoryRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
  }

  private void attachFields(ServiceCategory category, List<CategoryFieldRequest> fieldRequests) {
    if (fieldRequests == null || fieldRequests.isEmpty())
      return;

    IntStream.range(0, fieldRequests.size()).forEach(i -> {
      CategoryFieldRequest fr = fieldRequests.get(i);
      CategoryField field = CategoryField.builder().category(category)
          .fieldLabel(fr.getFieldLabel()).fieldKey(fr.getFieldKey().toLowerCase().replace(" ", "_"))
          .fieldType(fr.getFieldType()).options(fr.getOptions()).placeholder(fr.getPlaceholder())
          .required(fr.isRequired()).sortOrder(fr.getSortOrder() > 0 ? fr.getSortOrder() : i)
          .build();
      category.getFields().add(field);
    });
  }

  private ServiceCategoryResponse toResponse(ServiceCategory c) {
    List<CategoryFieldResponse> fieldResponses = c.getFields().stream()
        .map(f -> CategoryFieldResponse.builder().id(f.getId()).fieldLabel(f.getFieldLabel())
            .fieldKey(f.getFieldKey()).fieldType(f.getFieldType()).options(f.getOptions())
            .placeholder(f.getPlaceholder()).required(f.isRequired()).sortOrder(f.getSortOrder())
            .build())
        .toList();

    return ServiceCategoryResponse.builder().id(c.getId()).name(c.getName())
        .displayName(c.getDisplayName()).description(c.getDescription()).icon(c.getIcon())
        .active(c.isActive()).createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt())
        .fields(fieldResponses).build();
  }
}
