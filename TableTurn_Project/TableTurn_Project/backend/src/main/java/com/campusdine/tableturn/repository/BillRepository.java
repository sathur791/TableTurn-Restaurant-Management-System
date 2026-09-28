package com.campusdine.tableturn.repository;

import com.campusdine.tableturn.model.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {
    Optional<Bill> findByReservationId(Long reservationId);
}
