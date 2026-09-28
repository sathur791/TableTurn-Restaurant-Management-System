-- TableTurn - XAMPP/MySQL database
-- Import this file in phpMyAdmin, or let Spring Boot create/update the schema automatically.

CREATE DATABASE IF NOT EXISTS tableturn
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE tableturn;

CREATE TABLE IF NOT EXISTS restaurant_tables (
    id BIGINT NOT NULL AUTO_INCREMENT,
    table_number INT NOT NULL,
    capacity INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'FREE',
    PRIMARY KEY (id),
    UNIQUE KEY uk_restaurant_tables_number (table_number)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reservations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    table_id BIGINT NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    phone VARCHAR(30),
    party_size INT NOT NULL,
    reservation_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'RESERVATION',
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',
    PRIMARY KEY (id),
    KEY idx_reservations_table_date (table_id, reservation_date),
    CONSTRAINT fk_reservations_table FOREIGN KEY (table_id)
        REFERENCES restaurant_tables(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS bills (
    id BIGINT NOT NULL AUTO_INCREMENT,
    table_id BIGINT NOT NULL,
    reservation_id BIGINT NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    billed_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bills_reservation (reservation_id),
    KEY idx_bills_table (table_id),
    CONSTRAINT fk_bills_table FOREIGN KEY (table_id)
        REFERENCES restaurant_tables(id),
    CONSTRAINT fk_bills_reservation FOREIGN KEY (reservation_id)
        REFERENCES reservations(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    table_id BIGINT NOT NULL,
    reservation_id BIGINT NOT NULL,
    ordered_at DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    bill_id BIGINT NULL,
    PRIMARY KEY (id),
    KEY idx_orders_reservation (reservation_id),
    KEY idx_orders_table (table_id),
    KEY idx_orders_bill (bill_id),
    CONSTRAINT fk_orders_table FOREIGN KEY (table_id)
        REFERENCES restaurant_tables(id),
    CONSTRAINT fk_orders_reservation FOREIGN KEY (reservation_id)
        REFERENCES reservations(id),
    CONSTRAINT fk_orders_bill FOREIGN KEY (bill_id)
        REFERENCES bills(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_items_order (order_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- Starter restaurant tables. Safe to run more than once.
INSERT INTO restaurant_tables (table_number, capacity, status)
SELECT 1, 2, 'FREE' WHERE NOT EXISTS (SELECT 1 FROM restaurant_tables WHERE table_number = 1);
INSERT INTO restaurant_tables (table_number, capacity, status)
SELECT 2, 2, 'FREE' WHERE NOT EXISTS (SELECT 1 FROM restaurant_tables WHERE table_number = 2);
INSERT INTO restaurant_tables (table_number, capacity, status)
SELECT 3, 4, 'FREE' WHERE NOT EXISTS (SELECT 1 FROM restaurant_tables WHERE table_number = 3);
INSERT INTO restaurant_tables (table_number, capacity, status)
SELECT 4, 4, 'FREE' WHERE NOT EXISTS (SELECT 1 FROM restaurant_tables WHERE table_number = 4);
INSERT INTO restaurant_tables (table_number, capacity, status)
SELECT 5, 4, 'FREE' WHERE NOT EXISTS (SELECT 1 FROM restaurant_tables WHERE table_number = 5);
INSERT INTO restaurant_tables (table_number, capacity, status)
SELECT 6, 6, 'FREE' WHERE NOT EXISTS (SELECT 1 FROM restaurant_tables WHERE table_number = 6);
INSERT INTO restaurant_tables (table_number, capacity, status)
SELECT 7, 6, 'FREE' WHERE NOT EXISTS (SELECT 1 FROM restaurant_tables WHERE table_number = 7);
INSERT INTO restaurant_tables (table_number, capacity, status)
SELECT 8, 8, 'FREE' WHERE NOT EXISTS (SELECT 1 FROM restaurant_tables WHERE table_number = 8);
