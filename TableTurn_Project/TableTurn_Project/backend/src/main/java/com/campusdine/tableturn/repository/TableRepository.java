package com.campusdine.tableturn.repository;

import com.campusdine.tableturn.model.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TableRepository extends JpaRepository<RestaurantTable, Long> {
    boolean existsByTableNumber(Integer tableNumber);
}
