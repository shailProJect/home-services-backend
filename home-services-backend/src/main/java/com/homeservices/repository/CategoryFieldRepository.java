package com.homeservices.repository;

import com.homeservices.entity.CategoryField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryFieldRepository extends JpaRepository<CategoryField, UUID> {

  List<CategoryField> findByCategoryIdOrderBySortOrderAsc(UUID categoryId);

  @Modifying
  @Query("DELETE FROM CategoryField f WHERE f.category.id = :categoryId")
  void deleteAllByCategoryId(UUID categoryId);
}
