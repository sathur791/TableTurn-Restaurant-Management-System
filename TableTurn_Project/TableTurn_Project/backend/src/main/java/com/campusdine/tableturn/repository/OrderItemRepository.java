package com.campusdine.tableturn.repository;

import com.campusdine.tableturn.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
