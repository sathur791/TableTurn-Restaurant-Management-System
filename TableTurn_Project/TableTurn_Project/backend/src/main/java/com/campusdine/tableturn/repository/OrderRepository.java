package com.campusdine.tableturn.repository;

import com.campusdine.tableturn.model.CustomerOrder;
import com.campusdine.tableturn.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByReservationIdOrderByOrderedAtAsc(Long reservationId);

    List<CustomerOrder> findByReservationIdAndStatus(Long reservationId, OrderStatus status);
}
