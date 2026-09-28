package com.campusdine.tableturn.service;

import com.campusdine.tableturn.dto.Dtos.ReservationRequest;
import com.campusdine.tableturn.dto.Dtos.WalkInRequest;
import com.campusdine.tableturn.exception.ApiException;
import com.campusdine.tableturn.model.*;
import com.campusdine.tableturn.repository.ReservationRepository;
import com.campusdine.tableturn.repository.TableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    static final Set<ReservationStatus> ACTIVE = EnumSet.of(ReservationStatus.BOOKED, ReservationStatus.SEATED);
    private static final int DEFAULT_DURATION = 90;

    private final ReservationRepository reservations;
    private final TableRepository tables;
    private final TableService tableService;

    public ReservationService(ReservationRepository reservations, TableRepository tables, TableService tableService) {
        this.reservations = reservations;
        this.tables = tables;
        this.tableService = tableService;
    }

    public Page<Reservation> list(LocalDate date, Pageable pageable) {
        return date == null ? reservations.findAll(pageable) : reservations.findByReservationDate(date, pageable);
    }

    public Reservation getOrThrow(Long id) {
        return reservations.findById(id)
                .orElseThrow(() -> ApiException.notFound("Reservation with id " + id + " not found"));
    }

    /** Feature 1 + 2: reserve a table for a date/time/party size and block overlapping bookings. */
    @Transactional
    public Reservation reserve(ReservationRequest req) {
        RestaurantTable table = tableService.getOrThrow(req.tableId());
        checkCapacity(table, req.partySize());

        int duration = req.durationMinutes() == null ? DEFAULT_DURATION : req.durationMinutes();
        LocalTime start = req.startTime();
        LocalTime end = start.plusMinutes(duration);
        if (!end.isAfter(start)) {
            throw ApiException.badRequest("Reservation must finish before midnight");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startAt = LocalDateTime.of(req.date(), start);
        LocalDateTime endAt = LocalDateTime.of(req.date(), end);
        if (startAt.isBefore(now.minusMinutes(15))) {
            throw ApiException.badRequest("Cannot reserve a time slot in the past");
        }

        // Rule: a table must be free before it can be assigned (checked before saving).
        boolean windowIsNow = !now.isBefore(startAt) && now.isBefore(endAt);
        if (windowIsNow && table.getStatus() != TableStatus.FREE) {
            throw ApiException.conflict("Table " + table.getTableNumber() + " is currently occupied");
        }
        if (reservations.existsOverlap(table.getId(), req.date(), start, end, ACTIVE)) {
            throw ApiException.conflict("Table " + table.getTableNumber() + " is already booked between "
                    + start + " and " + end + " on " + req.date());
        }

        Reservation r = new Reservation();
        r.setTable(table);
        r.setCustomerName(req.customerName().trim());
        r.setPhone(req.phone());
        r.setPartySize(req.partySize());
        r.setReservationDate(req.date());
        r.setStartTime(start);
        r.setEndTime(end);
        r.setType(ReservationType.RESERVATION);
        r.setStatus(ReservationStatus.BOOKED);
        Reservation saved = reservations.save(r);
        log.info("[NOTIFY] Reservation #{} BOOKED: table {} for {} on {} at {}",
                saved.getId(), table.getTableNumber(), saved.getCustomerName(), req.date(), start);
        return saved;
    }

    /** Guest with a booking has arrived: table must be FREE, then becomes OCCUPIED. */
    @Transactional
    public Reservation seat(Long id) {
        Reservation r = getOrThrow(id);
        if (r.getStatus() != ReservationStatus.BOOKED) {
            throw ApiException.conflict("Only BOOKED reservations can be seated (current status: " + r.getStatus() + ")");
        }
        RestaurantTable table = r.getTable();
        if (table.getStatus() != TableStatus.FREE) {
            throw ApiException.conflict("Table " + table.getTableNumber() + " is not free yet");
        }
        table.setStatus(TableStatus.OCCUPIED);
        tables.save(table);
        r.setStatus(ReservationStatus.SEATED);
        log.info("[NOTIFY] Table {} is now OCCUPIED (reservation #{})", table.getTableNumber(), r.getId());
        return reservations.save(r);
    }

    /** Walk-in guests: table must be FREE and not reserved for the next 90 minutes. */
    @Transactional
    public Reservation seatWalkIn(WalkInRequest req) {
        RestaurantTable table = tableService.getOrThrow(req.tableId());
        checkCapacity(table, req.partySize());
        if (table.getStatus() != TableStatus.FREE) {
            throw ApiException.conflict("Table " + table.getTableNumber() + " is not free");
        }
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        LocalTime start = now.toLocalTime();
        LocalTime end = start.plusMinutes(DEFAULT_DURATION);
        if (!end.isAfter(start)) {
            end = LocalTime.of(23, 59);
        }
        if (reservations.existsOverlap(table.getId(), now.toLocalDate(), start, end, ACTIVE)) {
            throw ApiException.conflict("Table " + table.getTableNumber() + " has a reservation starting soon");
        }

        Reservation r = new Reservation();
        r.setTable(table);
        r.setCustomerName(req.customerName().trim());
        r.setPartySize(req.partySize());
        r.setReservationDate(now.toLocalDate());
        r.setStartTime(start);
        r.setEndTime(end);
        r.setType(ReservationType.WALK_IN);
        r.setStatus(ReservationStatus.SEATED);
        table.setStatus(TableStatus.OCCUPIED);
        tables.save(table);
        log.info("[NOTIFY] Walk-in seated at table {} ({} guests)", table.getTableNumber(), req.partySize());
        return reservations.save(r);
    }

    @Transactional
    public Reservation cancel(Long id) {
        Reservation r = getOrThrow(id);
        if (r.getStatus() != ReservationStatus.BOOKED) {
            throw ApiException.conflict("Only BOOKED reservations can be cancelled (current status: " + r.getStatus() + ")");
        }
        r.setStatus(ReservationStatus.CANCELLED);
        log.info("[NOTIFY] Reservation #{} CANCELLED", r.getId());
        return reservations.save(r);
    }

    private void checkCapacity(RestaurantTable table, int partySize) {
        if (partySize > table.getCapacity()) {
            throw ApiException.badRequest("Party of " + partySize + " exceeds capacity " + table.getCapacity()
                    + " of table " + table.getTableNumber());
        }
    }
}
