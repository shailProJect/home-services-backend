package com.homeservices.config;

import com.homeservices.entity.ServiceCategory;
import com.homeservices.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * UPDATED DataSeeder
 *
 * Seeds the default categories on first start. Each entry now includes: name — uppercase internal
 * key displayName — human-readable label icon — emoji shown in the UI description — short info card
 * text
 *
 * New categories can be added dynamically via the Admin Panel at runtime; this seeder only runs
 * once (checks existsByName before inserting).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

  private final ServiceCategoryRepository serviceCategoryRepository;

  record SeedEntry(String name, String displayName, String icon, String description) {
  }

  private static final List<SeedEntry> DEFAULT_CATEGORIES = List.of(
      new SeedEntry("ELECTRICIAN", "Electrician", "⚡",
          "Wiring, installations, repairs & electrical safety."),
      new SeedEntry("PLUMBER", "Plumber", "🔧",
          "Pipe repair, drain cleaning & fixture installation."),
      new SeedEntry("CARPENTER", "Carpenter", "🪚", "Furniture, doors, windows & custom woodwork."),
      new SeedEntry("PAINTER", "Painter", "🎨", "Interior & exterior painting and surface prep."),
      new SeedEntry("CLEANER", "Cleaner", "🧹", "Deep cleaning, sanitization & housekeeping."),
      new SeedEntry("AC_TECHNICIAN", "AC Technician", "❄️",
          "AC installation, servicing & gas refilling."),
      new SeedEntry("APPLIANCE_REPAIR", "Appliance Repair", "🔌",
          "Washing machine, refrigerator & appliance fixes."));

  @Override
  public void run(String... args) {
    seedCategories();
  }

  private void seedCategories() {
    for (SeedEntry entry : DEFAULT_CATEGORIES) {
      if (!serviceCategoryRepository.existsByName(entry.name())) {
        serviceCategoryRepository
            .save(ServiceCategory.builder().name(entry.name()).displayName(entry.displayName())
                .icon(entry.icon()).description(entry.description()).active(true).build());
        log.info("Seeded category: {}", entry.name());
      }
    }
  }
}
