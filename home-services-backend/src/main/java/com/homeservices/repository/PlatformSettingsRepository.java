package com.homeservices.repository;

import com.homeservices.entity.PlatformSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformSettingsRepository extends JpaRepository<PlatformSettings, Long> {
  // Singleton row is always id=1; use findById(1L) or the helper in PlatformSettingsService
}
