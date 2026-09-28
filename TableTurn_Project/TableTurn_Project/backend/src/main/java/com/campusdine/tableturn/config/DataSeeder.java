package com.campusdine.tableturn.config;

import com.campusdine.tableturn.model.RestaurantTable;
import com.campusdine.tableturn.repository.TableRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Creates 8 sample tables on first run so the app is usable immediately. */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedTables(TableRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                repo.saveAll(List.of(
                        new RestaurantTable(1, 2), new RestaurantTable(2, 2),
                        new RestaurantTable(3, 4), new RestaurantTable(4, 4),
                        new RestaurantTable(5, 4), new RestaurantTable(6, 6),
                        new RestaurantTable(7, 6), new RestaurantTable(8, 8)));
            }
        };
    }
}
