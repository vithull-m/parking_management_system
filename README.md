# 🅿️ ParkFlow – Vehicle Parking Management Web Application

A beginner-friendly, modular **Vehicle Parking Management Web Application** built with **Java 21**, **HTML5/CSS3/JavaScript (Modern Light UI)**, **SQLite**, and **JDBC**.

---

## 🌟 Key Features

1. **🌐 Modern Web Application (HTML / CSS / JS):**
   - Clean, light, responsive dashboard with soft slate/blue palette, card layouts, badges, and modal dialogs.
   - Real-time search and filter for parking slots.
   - Live price calculation dialog showing Base Amount, Booking Fee (₹50 if base < ₹1000, else ₹0), and Grand Total.
   - Formatted printable digital parking pass / receipt.
   - Audit trail timeline modal showing complete status transition history.
2. **👤 New User Registration:**
   - Any new visitor can register directly via the webpage (`Full Name`, `Username`, `10-digit Phone`, `Password`).
   - Validates unique username, 10-digit phone, and min 4-character password.
   - Auto-logs in immediately upon successful registration.
3. **⚡ 1-Click Demo Login Shortcuts:**
   - Instant 1-click login for fast testing:
     - `User 1` (John Doe)
     - `User 2` (Jane Smith)
     - `Admin` (System Administrator)
4. **🔒 Mandatory Rules & Transactional Safety:**
   - Quantity strictly enforced to **1–5 slots**.
   - Rechecks live slot availability and captures unit rate snapshot atomically in SQLite.
   - Status flow: `CONFIRMED ➔ CHECKED_IN ➔ COMPLETED`. Admin cannot skip stages.
   - User cancellation permitted only for `CONFIRMED` bookings; restores slot availability **exactly once**.
   - Users can access/cancel only their own bookings.
5. **🛡️ Admin Inventory Management:**
   - Administrators can add parking spots and edit identifiers, names, category/schedule, rates, capacity, and booking availability.
   - Capacity updates preserve occupied spots and cannot reduce total capacity below current occupancy.

---

## 🛠️ Tech Stack & Project Architecture

- **Backend:** Java 21 (LTS), Embedded HTTP Server (`com.sun.net.httpserver`), Gson
- **Database:** SQLite 3 via `org.xerial:sqlite-jdbc` with foreign keys and ACID transactions
- **Frontend:** Pure HTML5, Modern CSS3 (CSS Variables, Flexbox/Grid, Responsive), Vanilla JavaScript (ES6+, Fetch API)
- **Testing:** JUnit 5 (14 Comprehensive Tests)
- **Build Tool:** Apache Maven

### Layered Project Structure
```
d:/parking_management_system/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/parking/
│   │   │   ├── web/
│   │   │   │   └── WebServer.java               # Built-in Web Server & REST API Handlers
│   │   │   ├── db/
│   │   │   │   └── DatabaseManager.java         # SQLite Connection, DDL Schema & Demo Seeding
│   │   │   ├── model/
│   │   │   │   ├── User.java                    # User Entity
│   │   │   │   ├── UserRole.java                # USER, ADMIN Enum
│   │   │   │   ├── Schedule.java                # Operating Hours & Category Entity
│   │   │   │   ├── Resource.java                # Parking Slot Entity
│   │   │   │   ├── Reservation.java             # Booking Entity
│   │   │   │   ├── ReservationItem.java         # Rate & Resource Snapshot Entity
│   │   │   │   ├── BookingStatus.java           # Status Enum (CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED)
│   │   │   │   ├── BookingStatusHistory.java    # Audit Trail History Entity
│   │   │   │   ├── BookingRequest.java          # Booking Input DTO
│   │   │   │   └── FeeBreakdown.java            # Pricing DTO
│   │   │   ├── dao/
│   │   │   │   ├── UserDao.java                 # User Data Access & User Registration
│   │   │   │   ├── ScheduleDao.java             # Schedule Data Access
│   │   │   │   ├── ResourceDao.java             # Parking Slot Data Access & Atomic Capacity Updates
│   │   │   │   ├── ReservationDao.java          # Booking & Item Snapshot Data Access
│   │   │   │   └── BookingStatusHistoryDao.java # Audit Trail Data Access
│   │   │   ├── service/
│   │   │   │   ├── AvailabilityService.java     # Slot Search & Live Availability Checks
│   │   │   │   ├── BookingService.java          # Transactional Booking, Cancellation & Admin Transitions
│   │   │   │   └── AuthService.java             # Login, Registration & Session Tracking
│   │   │   ├── exception/
│   │   │   │   ├── ValidationException.java     # Business Rule Validation Errors
│   │   │   │   ├── UnauthorizedException.java   # Access Control Errors
│   │   │   │   └── ServiceException.java        # Generic Service Errors
│   │   └── resources/
│   │       └── static/
│   │           ├── index.html                   # Single Page Application HTML Layout
│   │           ├── style.css                    # Clean Light CSS Stylesheet
│   │           └── app.js                       # Frontend Controller & REST API Client
│   └── test/
│       └── java/com/parking/
│           └── ParkingManagementSystemTest.java # 14 Comprehensive JUnit 5 Tests
```

---

## 🌐 REST API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register new user (`fullName`, `username`, `phone`, `password`) |
| `POST` | `/api/auth/login` | Login user (`username`, `password`) |
| `GET` | `/api/auth/users` | List demo users |
| `GET` | `/api/slots` | List active parking slots (supports search query `?q=...`) |
| `POST` | `/api/bookings/calculate` | Calculate fee breakdown (`rate`, `quantity`) |
| `POST` | `/api/bookings` | Atomically confirm booking with rate snapshot & capacity reduction |
| `GET` | `/api/bookings?userId=X` | View logged-in user's bookings |
| `GET` | `/api/bookings/{id}/history?userId=X` | View booking status transition audit trail |
| `POST` | `/api/bookings/{id}/cancel` | Cancel eligible booking and restore slot capacity |
| `GET` | `/api/admin/bookings?userId=X` | View all bookings across system (Admin only) |
| `POST` | `/api/admin/status` | Advance operational status (`CONFIRMED ➔ CHECKED_IN ➔ COMPLETED`) |
| `GET` | `/api/admin/stats?userId=X` | Summary metrics & live slot occupancy rates (Admin only) |
| `POST` | `/api/admin/slots` | Add a parking spot (Admin only; send `adminUserId`, `identifier`, `name`, `scheduleId`, `rate`, `totalCapacity`, and `active`) |
| `PUT` | `/api/admin/slots?id=X` | Update a parking spot (Admin only; same fields as add) |

---

## 👥 Demo Accounts & Pre-seeded Parking Slots

| Role | Username | Password | Full Name | Phone |
|---|---|---|---|---|
| **User 1** | `user1` | `user123` | John Doe | `9876543210` |
| **User 2** | `user2` | `user123` | Jane Smith | `9123456780` |
| **Admin** | `admin` | `admin123` | System Administrator | `9999999999` |

### Demo Parking Slots
- **`R101`** – Parking Slot A – **₹499.00** – 10 / 10 spots available
- **`R102`** – Parking Slot B – **₹799.00** – 5 / 5 spots available
- **`R103`** – Parking Slot C – **₹199.00** – 20 / 20 spots available
- **`R104`** – Parking Slot D – **₹1299.00** – 0 / 10 spots (Sold Out)
- **`R105`** – Parking Slot E – **₹1000.00** – 3 / 5 spots available

---

## 🗄️ Database Schema

### 1. `users`
- `id` (INTEGER PRIMARY KEY AUTOINCREMENT)
- `username` (TEXT UNIQUE NOT NULL)
- `password` (TEXT NOT NULL)
- `full_name` (TEXT NOT NULL)
- `role` (TEXT NOT NULL) — `USER` or `ADMIN`
- `phone` (TEXT)

### 2. `schedules`
- `id` (INTEGER PRIMARY KEY AUTOINCREMENT)
- `schedule_code` (TEXT UNIQUE NOT NULL)
- `category` (TEXT NOT NULL)
- `operating_hours` (TEXT NOT NULL)

### 3. `resources` (Parking Slots)
- `id` (INTEGER PRIMARY KEY AUTOINCREMENT)
- `identifier` (TEXT UNIQUE NOT NULL) — e.g. `R101`
- `name` (TEXT NOT NULL) — e.g. `Parking Slot A`
- `schedule_id` (INTEGER NOT NULL REFERENCES schedules(id))
- `rate` (REAL NOT NULL) — e.g. `499.00`
- `total_capacity` (INTEGER NOT NULL)
- `available_quantity` (INTEGER NOT NULL)
- `is_active` (INTEGER NOT NULL DEFAULT 1)

### 4. `reservations`
- `id` (INTEGER PRIMARY KEY AUTOINCREMENT)
- `booking_reference` (TEXT UNIQUE NOT NULL) — e.g. `PK-20261008-AB12CD`
- `user_id` (INTEGER NOT NULL REFERENCES users(id))
- `customer_name` (TEXT NOT NULL)
- `customer_phone` (TEXT NOT NULL)
- `vehicle_number` (TEXT NOT NULL)
- `booking_date` (TEXT NOT NULL)
- `base_amount` (REAL NOT NULL)
- `booking_charge` (REAL NOT NULL) — ₹50 if base < ₹1000, else ₹0
- `grand_total` (REAL NOT NULL)
- `status` (TEXT NOT NULL) — `CONFIRMED`, `CHECKED_IN`, `COMPLETED`, `CANCELLED`
- `created_at` (TEXT NOT NULL)

### 5. `reservation_items` (Resource & Rate Snapshot)
- `id` (INTEGER PRIMARY KEY AUTOINCREMENT)
- `reservation_id` (INTEGER NOT NULL REFERENCES reservations(id))
- `resource_id` (INTEGER NOT NULL REFERENCES resources(id))
- `resource_identifier_snapshot` (TEXT NOT NULL)
- `resource_name_snapshot` (TEXT NOT NULL)
- `category_snapshot` (TEXT NOT NULL)
- `rate_snapshot` (REAL NOT NULL)
- `quantity` (INTEGER NOT NULL) — 1 to 5
- `subtotal` (REAL NOT NULL)

### 6. `booking_status_history` (Audit Trail)
- `id` (INTEGER PRIMARY KEY AUTOINCREMENT)
- `reservation_id` (INTEGER NOT NULL REFERENCES reservations(id))
- `from_status` (TEXT)
- `to_status` (TEXT NOT NULL)
- `changed_by_user_id` (INTEGER NOT NULL REFERENCES users(id))
- `changed_by_role` (TEXT NOT NULL)
- `remarks` (TEXT)
- `changed_at` (TEXT NOT NULL)

---

## 📋 Business Rules & Validation

1. **Quantity Limit:** Must be **1–5 only**. Rejects 0, negative, decimals, and >5.
2. **Availability Check:** Re-verified during transactional confirmation.
3. **Rate Snapshot:** The exact current unit rate is captured and persisted in `reservation_items`. Subsequent master catalog rate edits do not alter past bookings.
4. **Fee Formula:**
   - $\text{Base Amount} = \text{Rate} \times \text{Quantity}$
   - $\text{Booking Charge} = ₹50\text{ if Base} < ₹1000\text{, otherwise } ₹0$
   - $\text{Grand Total} = \text{Base Amount} + \text{Booking Charge}$
5. **Input Validation:** Non-blank customer name; exactly 10 numeric digits for phone; valid vehicle number; valid `YYYY-MM-DD` date.
6. **Transactional Atomicity:** Booking creation, snapshot saving, capacity reduction, and history logging succeed or fail together. If confirmation fails, **no booking is created and availability is unchanged**.
7. **Status Flow:** `CONFIRMED ➔ CHECKED_IN ➔ COMPLETED`. Admin cannot skip stages.
8. **Cancellation:** Users can cancel **only** their own `CONFIRMED` bookings. Cancellation restores slot capacity **exactly once**; repeated cancellation is rejected.

---

## 🚀 How to Run the Web Application

### Prerequisites
- **Java 21** (LTS)
- **Apache Maven 3.8+**
### 1. Run the Web Server:
```bash
mvn exec:java
```
*Or execute the packaged standalone JAR:*
```bash
java -jar target/parking-management-system.jar
```

### 2. Open in your Browser:
👉 **[http://localhost:8080](http://localhost:8080)**

---

## 🧪 Run Automated Test Suite (14 Tests)

```bash
mvn test
```

### Tests Covered:
1. **Quantity Boundaries:** Accepts 1 & 5; rejects 0, negative, and 6+.
2. **Unavailable Slot:** Rejects booking for R104 (0 spots available) with 0 DB side-effects.
3. **Fare Boundaries:** Tests ₹999 (₹50 fee), ₹1000 (₹0 fee), and ₹1001 (₹0 fee), plus multiple quantity combinations.
4. **Successful Booking:** Verifies atomic reservation, item snapshots, capacity reduction, and initial history record.
5. **Invalid Details:** Rejects blank customer name, invalid phone digits (<10 or letters), blank vehicle, and invalid dates.
6. **Availability Change:** Rejects booking if another user consumes capacity before confirmation.
7. **Access Denial:** User 2 cannot view or cancel User 1's booking; non-admin cannot perform operational status changes.
8. **Status Transitions:** Verifies `CONFIRMED ➔ CHECKED_IN ➔ COMPLETED` and rejects invalid stage skips.
9. **Cancellation Twice:** Verifies second cancellation is blocked and capacity is restored only once.
10. **Restart Persistence:** Verifies bookings and capacity persist across database-manager instances.
11. **Rate-Change Snapshot:** Master catalog rate changes do not affect existing booking snapshots.
12. **User Registration:** Tests registering new users via the web service, validates 10-digit phone, rejects duplicate usernames, and tests instant login.
13. **Admin Inventory Management:** Verifies administrators can add and update spots while preserving occupied capacity.
14. **Inventory Authorization and Capacity:** Rejects non-admin changes and capacity reductions below current occupancy.
