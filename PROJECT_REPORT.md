# ParkFlow Parking Management System

## Project Report

**Project type:** Web-based parking reservation and inventory management system
**Application:** ParkFlow
**Primary technologies:** Java 21, embedded HTTP server, SQLite, JDBC, Gson, HTML, CSS, and JavaScript
**Build and test tools:** Maven and JUnit 5

## 1. Executive Summary

ParkFlow is a web application for managing parking inventory and reservations. Customers can create accounts, search available parking spots, make reservations, view their booking history, and cancel eligible bookings. Administrators can review bookings, advance their operational status, monitor inventory and usage, and add or edit parking spots.

The application uses a Java HTTP server to provide JSON API endpoints and serve a browser-based interface. Persistent data is stored in SQLite. Booking creation uses a database transaction to save the reservation and its rate snapshot, reduce spot availability, and record the initial status history together.

## 2. Problem Statement

Parking operations need a reliable way to present availability, reserve capacity, track customer bookings, and keep spot information current. Manual or disconnected processes can lead to overbooking, inconsistent rates, and difficulty tracing booking status changes.

ParkFlow centralizes these tasks in a web application and applies server-side validation and database-backed rules to core reservation and inventory operations.

## 3. Project Objectives

- Provide a browser-based interface for customers and administrators.
- Allow customers to register, sign in, search spots, create reservations, and view booking history.
- Prevent bookings that exceed current availability or violate booking validation rules.
- Preserve the price and spot details used when a booking was created.
- Support a sequential reservation status workflow and an audit history.
- Allow administrators to manage parking spots, including capacity, rate, schedule, and active status.
- Persist application data in SQLite and verify core rules with automated tests.

## 4. Scope and User Roles

### Customer

- Register an account and sign in.
- Browse active spots and search by identifier, name, or category.
- Reserve between one and five spots, subject to current availability.
- View personal bookings and status history.
- Cancel their own bookings while they are in the `CONFIRMED` state.

### Administrator

- View system-wide bookings and summary metrics.
- Advance bookings through the allowed operational status sequence.
- View reservation details and audit history.
- Review all parking inventory, including inactive spots.
- Add new parking spots and edit existing spot details.

The project is a single-server web application intended for local or demonstration use. It does not provide payment processing or multi-server deployment.

## 5. Technology Stack

| Area | Technology |
|---|---|
| Runtime | Java 21 |
| HTTP and API | `com.sun.net.httpserver.HttpServer` |
| Persistence | SQLite through JDBC |
| JSON | Gson |
| Browser interface | HTML5, CSS3, vanilla JavaScript, Fetch API |
| Build | Apache Maven |
| Automated tests | JUnit 5 |

The former Swing desktop interface has been removed. The web application is the current user interface.

## 6. System Architecture

The application is organized in layers:

1. **Presentation layer:** Static HTML, CSS, and JavaScript implement authentication, slot search, booking views, and admin operations.
2. **HTTP layer:** `WebServer` serves static assets, parses API requests, invokes services, and returns JSON responses.
3. **Service layer:** `AuthService`, `AvailabilityService`, `BookingService`, and `ResourceManagementService` enforce application rules and coordinate operations.
4. **Data access layer:** DAO classes execute parameterized JDBC queries for users, schedules, spots, reservations, and booking history.
5. **Database layer:** `DatabaseManager` initializes SQLite tables, configures foreign keys, and seeds demo data when the relevant tables are empty.

### Main source areas

- [`WebServer.java`](./src/main/java/com/parking/web/WebServer.java) — HTTP server, static file hosting, and API handlers.
- [`DatabaseManager.java`](./src/main/java/com/parking/db/DatabaseManager.java) — SQLite schema creation and demo-data initialization.
- [`BookingService.java`](./src/main/java/com/parking/service/BookingService.java) — booking, cancellation, authorization checks, and status transitions.
- [`ResourceManagementService.java`](./src/main/java/com/parking/service/ResourceManagementService.java) — admin authorization and spot add/update validation.
- [`src/main/resources/static/`](./src/main/resources/static/) — browser application assets.
- [`ParkingManagementSystemTest.java`](./src/test/java/com/parking/ParkingManagementSystemTest.java) — automated business-rule tests.

## 7. Functional Overview

### Customer booking workflow

1. The customer signs in or registers.
2. The browser requests active spots from the server and displays current rates and availability.
3. The customer selects a spot, quantity, vehicle details, and booking date.
4. The application validates the submitted details and rechecks availability when confirming.
5. Within a transaction, the application stores the reservation and item snapshot, reduces available capacity, and adds the initial status-history entry.
6. The customer can later inspect the booking or cancel it if it is still confirmed.

### Administrator inventory workflow

1. The administrator opens **Admin Operations → Live Slot Inventory**.
2. The inventory table displays active and inactive spots, capacity, remaining availability, occupancy, and status.
3. The administrator adds a spot or chooses **Edit** on an existing row.
4. The server validates the administrator, required values, schedule, rate, identifier uniqueness, and capacity.
5. On capacity edits, the system calculates occupied spots from the current capacity and availability. It preserves that occupancy and rejects a new total capacity below the occupied count.
6. The interface refreshes the inventory and customer-facing availability after a successful save.

### Booking status workflow

The allowed operational transitions are:

```text
CONFIRMED → CHECKED_IN → COMPLETED
```

The application rejects invalid transitions. Cancellation is a separate action available to customers only while a booking is `CONFIRMED`.

## 8. Business Rules

- Reservation quantity must be an integer from **1 to 5**.
- A spot must be active and have sufficient availability at confirmation time.
- Booking charges are **₹50 when the base amount is below ₹1,000**, and **₹0 when it is ₹1,000 or more**.
- A reservation stores the rate and spot details used at the time of booking. Later catalog edits do not rewrite existing reservation snapshots.
- Customer name and vehicle number must not be blank; phone must contain exactly ten digits; booking date must use `YYYY-MM-DD`.
- Booking creation, capacity reduction, reservation-item snapshot, and initial history record are committed atomically.
- A customer can access and cancel only their own bookings.
- Cancellation restores capacity once; a repeated cancellation is rejected.
- Only administrators can add or edit spots and advance system-wide booking statuses.
- A spot's total capacity cannot be reduced below the number of occupied spots.

## 9. Data Model

| Table | Purpose | Important data |
|---|---|---|
| `users` | Customer and administrator accounts | Username, password, full name, role, phone |
| `schedules` | Spot categories and operating hours | Schedule code, category, hours |
| `resources` | Parking spot inventory | Identifier, name, schedule, rate, total and available capacity, active flag |
| `reservations` | Booking-level information | Booking reference, customer, vehicle, date, totals, status |
| `reservation_items` | Booking-time spot and rate snapshot | Spot reference, identifier/name/category snapshots, rate, quantity, subtotal |
| `booking_status_history` | Audit trail of status changes | Previous and new status, actor, role, remarks, timestamp |

Foreign-key constraints are enabled for database connections. Spot identifiers and account usernames are unique.

## 10. API Summary

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/auth/register` | Register a customer |
| `POST` | `/api/auth/login` | Authenticate a user |
| `GET` | `/api/auth/users` | Return user list used by the application |
| `GET` | `/api/slots` | Search active spots |
| `POST` | `/api/bookings/calculate` | Calculate a fee breakdown |
| `POST` | `/api/bookings` | Create a reservation |
| `GET` | `/api/bookings?userId=X` | List a user's bookings |
| `GET` | `/api/bookings/{id}/history?userId=X` | Read booking status history |
| `POST` | `/api/bookings/{id}/cancel` | Cancel an eligible booking |
| `GET` | `/api/admin/bookings?userId=X` | List all bookings; administrator only |
| `POST` | `/api/admin/status` | Advance a booking status; administrator only |
| `GET` | `/api/admin/stats?userId=X` | Return admin metrics, inventory, and schedules |
| `POST` | `/api/admin/slots` | Add a spot; administrator only |
| `PUT` | `/api/admin/slots?id=X` | Update a spot; administrator only |

The add/update spot requests include the administrator ID and the spot's identifier, name, schedule ID, rate, total capacity, and active state.

## 11. User Interface

The browser interface includes:

- Sign-in and registration screens, with demo login shortcuts.
- A searchable available-spot view and reservation form.
- Customer booking history, details, cancellation, and printable booking information.
- Booking status history presented as an audit timeline.
- Administrator booking filters, status actions, metrics, and live inventory.
- An admin inventory form for creating and editing parking spots.

The frontend uses responsive CSS and communicates with the backend using JSON over Fetch API requests.

## 12. Testing and Verification

The JUnit suite contains **14 tests** covering:

- Quantity boundaries and pricing thresholds.
- Booking an unavailable spot and changes in availability.
- Successful transactional booking, stored snapshots, and history.
- Invalid customer booking input.
- Access restrictions, valid status transitions, and repeated cancellation.
- Persistence across database-manager instances.
- Preservation of historical rates after a spot rate changes.
- Customer registration validation and login.
- Admin spot creation and editing, occupancy preservation, non-admin rejection, and capacity-limit rejection.

The latest project validation completed with `mvn test`: **14 tests passed, with zero failures or errors**.

## 13. Setup and Execution

### Requirements

- Java 21 or later compatible with the configured Java 21 compilation target.
- Apache Maven 3.8 or newer.

### Start the application

From the project root:

```bash
mvn exec:java
```

Then open [http://localhost:8080](http://localhost:8080) in a browser. The server initializes the database and demo data on startup.

Run the automated tests with:

```bash
mvn test
```

### Demo accounts

The database initializer creates `user1`, `user2`, and `admin` demo accounts when the user table is empty. Their demonstration credentials are documented in the project README. These credentials are for local demonstrations, not production use.

The application creates or updates `parking_system.db` in the project working directory when it starts.

## 14. Limitations and Future Improvements

- **Authentication hardening:** Passwords are currently stored and compared as plain text, and the browser keeps user data in local storage. Production use should add password hashing, server-managed sessions or signed tokens, and CSRF protections as appropriate.
- **Authorization model:** Some API operations identify the requesting user through a client-supplied user ID. A production deployment should derive identity from a validated server-side authentication context rather than trusting request IDs.
- **Input and output hardening:** Add consistent request-size limits, stronger JSON schema validation, and context-aware HTML escaping for all dynamic UI output.
- **Operational controls:** Add configurable server port/database path, structured logging, health checks, and deployment configuration.
- **Feature extensions:** Potential additions include payments, reservation time windows, reports/export, and spot deletion or archival policies.

## 15. Conclusion

ParkFlow provides a functional web-based foundation for parking inventory and reservation management. Its layered Java architecture, SQLite persistence, transactional booking flow, rate snapshots, audit history, and administrator inventory tools support the core operational use cases. Automated tests cover the principal validation, authorization, booking, and inventory-management rules. Security and operational improvements are recommended before using the application with production data or exposing it beyond a trusted environment.
