package com.campusdine.tableturn.controller;

import com.campusdine.tableturn.dto.Dtos.TableRequest;
import com.campusdine.tableturn.dto.Dtos.WalkInRequest;
import com.campusdine.tableturn.model.Reservation;
import com.campusdine.tableturn.model.RestaurantTable;
import com.campusdine.tableturn.service.ReservationService;
import com.campusdine.tableturn.service.TableService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/tables")
public class TableController {

    private final TableService tableService;
    private final ReservationService reservationService;

    public TableController(TableService tableService, ReservationService reservationService) {
        this.tableService = tableService;
        this.reservationService = reservationService;
    }

    @GetMapping
    public List<RestaurantTable> all() {
        return tableService.findAll();
    }

    @GetMapping("/{id}")
    public RestaurantTable one(@PathVariable Long id) {
        return tableService.getOrThrow(id);
    }

    @GetMapping("/available")
    public List<RestaurantTable> available(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam int partySize,
            @RequestParam(defaultValue = "90") int durationMinutes) {
        return tableService.findAvailable(date, startTime, partySize, durationMinutes);
    }

    @PostMapping
    public ResponseEntity<RestaurantTable> create(@Valid @RequestBody TableRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tableService.create(req));
    }

    @PutMapping("/{id}")
    public RestaurantTable update(@PathVariable Long id, @Valid @RequestBody TableRequest req) {
        return tableService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tableService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Feature 5: mark a billed table free. */
    @PutMapping("/{id}/vacate")
    public RestaurantTable vacate(@PathVariable Long id) {
        return tableService.vacate(id);
    }

    /** Seat walk-in guests directly at a free table. */
    @PostMapping("/walk-in")
    public ResponseEntity<Reservation> walkIn(@Valid @RequestBody WalkInRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.seatWalkIn(req));
    }
}
