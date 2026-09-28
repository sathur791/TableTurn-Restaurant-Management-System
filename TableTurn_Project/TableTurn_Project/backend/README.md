# TableTurn - Restaurant Table Reservation and Order Tracker

Spring Boot 3 (Java 17) + Spring Data JPA + MySQL (XAMPP) with a built-in web UI.

## Run
1. In the XAMPP Control Panel start **MySQL** (Apache is not needed).
2. Open the folder in IntelliJ (File > Open > pom.xml > Open as Project). Wait for Maven to import.
   Use JDK 17+. If asked, enable Lombok annotation processing (Settings > Build > Compiler > Annotation Processors).
3. Run `TableTurnApplication`. The database `tableturn` and all tables are created automatically; 8 sample tables are seeded.
4. Open http://localhost:8080 for the UI. Import `postman/TableTurn.postman_collection.json` into Postman for API testing.

If your MySQL root user has a password, set it in `src/main/resources/application.properties`.

## Entities
RestaurantTable 1-N Reservation (a sitting) 1-N CustomerOrder 1-N OrderItem; Bill 1-1 Reservation, 1-N CustomerOrder.

## Business rules (enforced in the service layer, before saving)
- A table must be FREE before a walk-in or booked party can be seated; reservation for "now" also needs a FREE table.
- Overlapping booking windows on the same table/date are rejected (409).
- Orders only for OCCUPIED tables; bill total = sum of all order items of the current sitting; one bill per sitting.
- A table can be vacated (set FREE) only after its bill exists.

## Endpoints
| Method | URL | Purpose |
|---|---|---|
| GET/POST | /api/tables | list / add table |
| GET/PUT/DELETE | /api/tables/{id} | view / edit / delete |
| GET | /api/tables/available?date&startTime&partySize&durationMinutes | free tables for slot |
| POST | /api/tables/walk-in | seat walk-in |
| PUT | /api/tables/{id}/vacate | mark free after billing |
| GET/POST | /api/reservations | paged list (?date, page, size, sort) / reserve |
| PUT | /api/reservations/{id}/seat, /cancel | seat / cancel |
| POST | /api/orders | place order with items |
| GET | /api/orders/table/{tableId} | current sitting's orders |
| POST | /api/bills/table/{tableId} | generate bill |
| GET | /api/bills, /api/bills/{id} | bills |
