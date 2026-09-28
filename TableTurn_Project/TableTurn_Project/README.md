# TableTurn — Restaurant Table Reservation and Order Tracker

This project implements the supplied **TableTurn** assessment model: a campus restaurant needs table reservations by time slot and order tracking per table until billing. The assessment asks for Spring Boot, REST APIs, CRUD, service-layer business rules, validation/global error handling, and a database design around Table, Reservation, Order, OrderItem and Bill.

## Project structure

- `backend/` — Java 17 + Spring Boot 3.2.5 + Spring Data JPA + MySQL REST API
- `frontend/` — standalone HTML/CSS/JavaScript UI integrated with the backend API
- `database/tableturn.sql` — XAMPP/phpMyAdmin MySQL schema + 8 starter tables
- `backend/postman/TableTurn.postman_collection.json` — API test collection

## Required software

- Java 17+
- Maven 3.9+
- XAMPP MySQL
- Chrome/Edge
- IntelliJ IDEA / VS Code / Eclipse

## Run the database

1. Start MySQL from XAMPP.
2. Import `database/tableturn.sql` in phpMyAdmin, OR simply let Spring Boot create/update the database automatically.
3. Default credentials are root with an empty password. Change `backend/src/main/resources/application.properties` if needed.

## Run the backend

From `backend`:

```bash
mvn spring-boot:run
```

Or open `backend/pom.xml` in IntelliJ and run:
`com.campusdine.tableturn.TableTurnApplication`

Backend API: `http://localhost:8080/api`

## Run the frontend

Start the backend first, then open `frontend/index.html` in Chrome/Edge, or use VS Code Live Server.

The frontend sends requests to `http://localhost:8080/api`. Backend CORS is enabled for the API so a separate local frontend can communicate with it.

## Main workflow

1. View restaurant tables.
2. Reserve a free table for a date/time/party size.
3. Seat a reservation when the guest arrives, or seat a walk-in.
4. Place one or more orders against the current seated table.
5. Generate one final bill containing all orders for that sitting.
6. Vacate the table only after the bill exists; the table becomes FREE.

## Business rules implemented

- Table capacity must fit the party size.
- Overlapping active reservations for the same table/time window are rejected.
- A table must be FREE before a walk-in or booked reservation can be seated.
- Orders can only be placed for an OCCUPIED table with an active SEATED sitting.
- No new order is accepted after a bill has been generated for that sitting.
- A bill is generated from the sum of all order-item line totals for the current sitting.
- Only one bill is allowed per sitting.
- A table cannot be vacated until its bill has been generated.
- Invalid request data returns clear HTTP 400/409/404 JSON responses.

## API summary

- `GET /api/tables`
- `POST /api/tables`
- `PUT /api/tables/{id}`
- `DELETE /api/tables/{id}`
- `GET /api/tables/available?date=YYYY-MM-DD&startTime=HH:mm&partySize=4&durationMinutes=90`
- `POST /api/tables/walk-in`
- `PUT /api/tables/{id}/vacate`
- `GET /api/reservations`
- `POST /api/reservations`
- `GET /api/reservations/{id}`
- `PUT /api/reservations/{id}/seat`
- `PUT /api/reservations/{id}/cancel`
- `POST /api/orders`
- `GET /api/orders/table/{tableId}`
- `POST /api/bills/table/{tableId}`
- `GET /api/bills`
- `GET /api/bills/{id}`
