package com.campusdine.tableturn.service;

import com.campusdine.tableturn.dto.Dtos.TableRequest;
import com.campusdine.tableturn.exception.ApiException;
import com.campusdine.tableturn.model.*;
import com.campusdine.tableturn.repository.BillRepository;
import com.campusdine.tableturn.repository.ReservationRepository;
import com.campusdine.tableturn.repository.TableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class TableService {

    private static final Logger log = LoggerFactory.getLogger(TableService.class);

    private final TableRepository tables;
    private final ReservationRepository reservations;
    private final BillRepository bills;

    public TableService(TableRepository tables, ReservationRepository reservations, BillRepository bills) {
        this.tables = tables;
        this.reservations = reservations;
        this.bills = bills;
    }

    public List<RestaurantTable> findAll() {
        return tables.findAll(Sort.by("tableNumber"));
    }

    public RestaurantTable getOrThrow(Long id) {
        return tables.findById(id).orElseThrow(() -> ApiException.notFound("Table with id " + id + " not found"));
    }

    @Transactional
    public RestaurantTable create(TableRequest req) {
        if (tables.existsByTableNumber(req.tableNumber())) {
            throw ApiException.conflict("Table number " + req.tableNumber() + " already exists");
        }
        return tables.save(new RestaurantTable(req.tableNumber(), req.capacity()));
    }

    @Transactional
    public RestaurantTable update(Long id, TableRequest req) {
        RestaurantTable table = getOrThrow(id);
        if (!table.getTableNumber().equals(req.tableNumber()) && tables.existsByTableNumber(req.tableNumber())) {
            throw ApiException.conflict("Table number " + req.tableNumber() + " already exists");
        }
        table.setTableNumber(req.tableNumber());
        table.setCapacity(req.capacity());
        return tables.save(table);
    }

    @Transactional
    public void delete(Long id) {
        RestaurantTable table = getOrThrow(id);
        if (table.getStatus() != TableStatus.FREE) {
            throw ApiException.conflict("Table " + table.getTableNumber() + " is occupied and cannot be deleted");
        }
        if (reservations.existsByTableId(id)) {
            throw ApiException.conflict("Table " + table.getTableNumber() + " has booking history and cannot be deleted");
        }
        tables.delete(table);
    }

    /** Tables that fit the party and have no active booking overlapping the requested window. */
    public List<RestaurantTable> findAvailable(LocalDate date, LocalTime start, int partySize, int durationMinutes) {
        LocalTime end = start.plusMinutes(durationMinutes);
        if (!end.isAfter(start)) {
            throw ApiException.badRequest("Time slot must finish before midnight");
        }
        return findAll().stream()
                .filter(t -> t.getCapacity() >= partySize)
                .filter(t -> !reservations.existsOverlap(t.getId(), date, start, end, ReservationService.ACTIVE))
                .toList();
    }

    /** Rule: a table can only be vacated after its bill has been generated. Then it becomes FREE. */
    @Transactional
    public RestaurantTable vacate(Long id) {
        RestaurantTable table = getOrThrow(id);
        if (table.getStatus() == TableStatus.FREE) {
            throw ApiException.conflict("Table " + table.getTableNumber() + " is already free");
        }
        Reservation sitting = reservations.findFirstByTableIdAndStatus(id, ReservationStatus.SEATED)
                .orElseThrow(() -> ApiException.conflict("No active sitting found for table " + table.getTableNumber()));
        if (bills.findByReservationId(sitting.getId()).isEmpty()) {
            throw ApiException.conflict("Generate the bill for table " + table.getTableNumber() + " before vacating it");
        }
        sitting.setStatus(ReservationStatus.COMPLETED);
        reservations.save(sitting);
        table.setStatus(TableStatus.FREE);
        tables.save(table);
        log.info("[NOTIFY] Table {} is now FREE (sitting #{} completed)", table.getTableNumber(), sitting.getId());
        return table;
    }
}
