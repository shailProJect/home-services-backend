package com.homeservices.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.homeservices.entity.ServiceCategory;

@Repository
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, UUID> {
  Optional<ServiceCategory> findByName(String name);

  boolean existsByName(String name);

  List<ServiceCategory> findAllByActiveTrueOrderByDisplayNameAsc();

  List<ServiceCategory> findAllByOrderByDisplayNameAsc();
}
