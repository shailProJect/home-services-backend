package com.homeservices.config;

import com.homeservices.entity.ServiceCategory;
import com.homeservices.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ServiceCategoryRepository serviceCategoryRepository;

    @Override
    public void run(String... args) {
        seedCategories();
    }

    private void seedCategories() {
        List<String> categories = List.of("ELECTRICIAN", "PLUMBER", "CARPENTER", "PAINTER", "CLEANER");

        for (String name : categories) {
            if (!serviceCategoryRepository.existsByName(name)) {
                serviceCategoryRepository.save(
                        ServiceCategory.builder().name(name).build()
                );
                log.info("Seeded category: {}", name);
            }
        }
    }
}
