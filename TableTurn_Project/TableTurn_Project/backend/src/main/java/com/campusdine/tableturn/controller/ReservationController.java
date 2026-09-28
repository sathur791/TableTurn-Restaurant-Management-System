package com.campusdine.tableturn.controller;

import com.campusdine.tableturn.dto.Dtos.ReservationRequest;
import com.campusdine.tableturn.model.Reservation;
import com.campusdine.tableturn.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    /** Supports ?date=yyyy-MM-dd&page=0&size=10&sort=startTime,asc */
    @GetMapping
    public Page<Reservation> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(date, pageable);
    }

    @GetMapping("/{id}")
    public Reservation one(@PathVariable Long id) {
        return service.getOrThrow(id);
    }

    @PostMapping
    public ResponseEntity<Reservation> reserve(@Valid @RequestBody ReservationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.reserve(req));
    }

    @PutMapping("/{id}/seat")
    public Reservation seat(@PathVariable Long id) {
        return service.seat(id);
    }

    @PutMapping("/{id}/cancel")
    public Reservation cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
