package com.campusdine.tableturn.service;

import com.campusdine.tableturn.dto.Dtos.ItemRequest;
import com.campusdine.tableturn.dto.Dtos.OrderRequest;
import com.campusdine.tableturn.exception.ApiException;
import com.campusdine.tableturn.model.*;
import com.campusdine.tableturn.repository.BillRepository;
import com.campusdine.tableturn.repository.OrderRepository;
import com.campusdine.tableturn.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final BillRepository bills;
    private final ReservationRepository reservations;
    private final TableService tableService;

    public OrderService(OrderRepository orders, BillRepository bills,
                        ReservationRepository reservations, TableService tableService) {
        this.orders = orders;
        this.bills = bills;
        this.reservations = reservations;
        this.tableService = tableService;
    }

    /** Feature 3: place an order (multiple items) against a seated table. */
    @Transactional
    public CustomerOrder placeOrder(OrderRequest req) {
        RestaurantTable table = tableService.getOrThrow(req.tableId());
        Reservation sitting = currentSitting(table);
        if (bills.findByReservationId(sitting.getId()).isPresent()) {
            throw ApiException.conflict("Bill already generated for table " + table.getTableNumber()
                    + ". Vacate the table before taking new orders");
        }
        CustomerOrder order = new CustomerOrder();
        order.setTable(table);
        order.setReservation(sitting);
        for (ItemRequest i : req.items()) {
            OrderItem item = new OrderItem();
            item.setItemName(i.itemName().trim());
            item.setQuantity(i.quantity());
            item.setUnitPrice(i.unitPrice());
            order.addItem(item);
        }
        CustomerOrder saved = orders.save(order);
        log.info("[NOTIFY] Order #{} placed for table {} ({} items, subtotal {})",
                saved.getId(), table.getTableNumber(), saved.getItems().size(), saved.getSubtotal());
        return saved;
    }

    /** All orders of the table's current sitting. */
    public List<CustomerOrder> currentOrders(Long tableId) {
        RestaurantTable table = tableService.getOrThrow(tableId);
        return orders.findByReservationIdOrderByOrderedAtAsc(currentSitting(table).getId());
    }

    /** Feature 4: bill total = sum of every order item of the current sitting. */
    @Transactional
    public Bill generateBill(Long tableId) {
        RestaurantTable table = tableService.getOrThrow(tableId);
        Reservation sitting = currentSitting(table);
        if (bills.findByReservationId(sitting.getId()).isPresent()) {
            throw ApiException.conflict("Bill already generated for table " + table.getTableNumber());
        }
        List<CustomerOrder> sittingOrders = orders.findByReservationIdOrderByOrderedAtAsc(sitting.getId());
        if (sittingOrders.isEmpty()) {
            throw ApiException.conflict("Table " + table.getTableNumber() + " has no orders to bill");
        }
        BigDecimal total = sittingOrders.stream()
                .map(CustomerOrder::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Bill bill = new Bill();
        bill.setTable(table);
        bill.setReservation(sitting);
        bill.setTotalAmount(total);
        bill = bills.save(bill);
        for (CustomerOrder o : sittingOrders) {
            o.setBill(bill);
            o.setStatus(OrderStatus.BILLED);
        }
        orders.saveAll(sittingOrders);
        bill.setOrders(sittingOrders);
        log.info("[NOTIFY] Bill #{} generated for table {}: total {}", bill.getId(), table.getTableNumber(), total);
        return bill;
    }

    public List<Bill> listBills() {
        return bills.findAll(Sort.by(Sort.Direction.DESC, "billedAt"));
    }

    public Bill getBill(Long id) {
        return bills.findById(id).orElseThrow(() -> ApiException.notFound("Bill with id " + id + " not found"));
    }

    private Reservation currentSitting(RestaurantTable table) {
        if (table.getStatus() != TableStatus.OCCUPIED) {
            throw ApiException.conflict("Table " + table.getTableNumber()
                    + " is not seated. Seat a reservation or walk-in first");
        }
        return reservations.findFirstByTableIdAndStatus(table.getId(), ReservationStatus.SEATED)
                .orElseThrow(() -> ApiException.conflict("No active sitting for table " + table.getTableNumber()));
    }
}
