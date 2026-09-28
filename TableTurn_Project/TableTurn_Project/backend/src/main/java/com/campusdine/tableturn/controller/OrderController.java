package com.campusdine.tableturn.controller;

import com.campusdine.tableturn.dto.Dtos.OrderRequest;
import com.campusdine.tableturn.model.Bill;
import com.campusdine.tableturn.model.CustomerOrder;
import com.campusdine.tableturn.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping("/orders")
    public ResponseEntity<CustomerOrder> place(@Valid @RequestBody OrderRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.placeOrder(req));
    }

    @GetMapping("/orders/table/{tableId}")
    public List<CustomerOrder> currentOrders(@PathVariable Long tableId) {
        return service.currentOrders(tableId);
    }

    @PostMapping("/bills/table/{tableId}")
    public ResponseEntity<Bill> generateBill(@PathVariable Long tableId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.generateBill(tableId));
    }

    @GetMapping("/bills")
    public List<Bill> bills() {
        return service.listBills();
    }

    @GetMapping("/bills/{id}")
    public Bill bill(@PathVariable Long id) {
        return service.getBill(id);
    }
}
