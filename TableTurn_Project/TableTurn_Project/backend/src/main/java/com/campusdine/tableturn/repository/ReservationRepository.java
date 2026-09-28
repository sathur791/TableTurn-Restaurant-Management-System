package com.campusdine.tableturn.repository;

import com.campusdine.tableturn.model.Reservation;
import com.campusdine.tableturn.model.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** Two windows overlap when existing.start < new.end AND existing.end > new.start. */
    @Query("""
            select count(r) > 0 from Reservation r
            where r.table.id = :tableId
              and r.reservationDate = :date
              and r.status in :statuses
              and r.startTime < :end
              and r.endTime > :start
            """)
    boolean existsOverlap(@Param("tableId") Long tableId,
                          @Param("date") LocalDate date,
                          @Param("start") LocalTime start,
                          @Param("end") LocalTime end,
                          @Param("statuses") Collection<ReservationStatus> statuses);

    Optional<Reservation> findFirstByTableIdAndStatus(Long tableId, ReservationStatus status);

    boolean existsByTableId(Long tableId);

    Page<Reservation> findByReservationDate(LocalDate date, Pageable pageable);
}
