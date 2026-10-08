package com.parking.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:parking_system.db";
    private final String dbUrl;

    public DatabaseManager() {
        this(DEFAULT_DB_URL);
    }

    public DatabaseManager(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    public void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Users table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    full_name TEXT NOT NULL,
                    role TEXT NOT NULL,
                    phone TEXT
                );
            """);

            // Schedules table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS schedules (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    schedule_code TEXT UNIQUE NOT NULL,
                    category TEXT NOT NULL,
                    operating_hours TEXT NOT NULL
                );
            """);

            // Resources (Parking Slots) table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS resources (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    identifier TEXT UNIQUE NOT NULL,
                    name TEXT NOT NULL,
                    schedule_id INTEGER NOT NULL REFERENCES schedules(id),
                    rate REAL NOT NULL,
                    total_capacity INTEGER NOT NULL,
                    available_quantity INTEGER NOT NULL,
                    is_active INTEGER NOT NULL DEFAULT 1
                );
            """);

            // Reservations table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    booking_reference TEXT UNIQUE NOT NULL,
                    user_id INTEGER NOT NULL REFERENCES users(id),
                    customer_name TEXT NOT NULL,
                    customer_phone TEXT NOT NULL,
                    vehicle_number TEXT NOT NULL,
                    booking_date TEXT NOT NULL,
                    base_amount REAL NOT NULL,
                    booking_charge REAL NOT NULL,
                    grand_total REAL NOT NULL,
                    status TEXT NOT NULL,
                    created_at TEXT NOT NULL
                );
            """);

            // Reservation Items (Resource & Rate Snapshot) table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservation_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    reservation_id INTEGER NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
                    resource_id INTEGER NOT NULL REFERENCES resources(id),
                    resource_identifier_snapshot TEXT NOT NULL,
                    resource_name_snapshot TEXT NOT NULL,
                    category_snapshot TEXT NOT NULL,
                    rate_snapshot REAL NOT NULL,
                    quantity INTEGER NOT NULL,
                    subtotal REAL NOT NULL
                );
            """);

            // Booking Status History table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS booking_status_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    reservation_id INTEGER NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
                    from_status TEXT,
                    to_status TEXT NOT NULL,
                    changed_by_user_id INTEGER NOT NULL REFERENCES users(id),
                    changed_by_role TEXT NOT NULL,
                    remarks TEXT,
                    changed_at TEXT NOT NULL
                );
            """);

            seedDemoDataIfEmpty(conn);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema: " + e.getMessage(), e);
        }
    }

    private void seedDemoDataIfEmpty(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Check if users exist
            var rsUser = stmt.executeQuery("SELECT COUNT(*) FROM users;");
            if (rsUser.next() && rsUser.getInt(1) == 0) {
                // Seed 2 Users + 1 Admin
                stmt.execute("""
                    INSERT INTO users (username, password, full_name, role, phone) VALUES
                    ('user1', 'user123', 'John Doe', 'USER', '9876543210'),
                    ('user2', 'user123', 'Jane Smith', 'USER', '9123456780'),
                    ('admin', 'admin123', 'System Administrator', 'ADMIN', '9999999999');
                """);
            }

            // Check if schedules exist
            var rsSch = stmt.executeQuery("SELECT COUNT(*) FROM schedules;");
            if (rsSch.next() && rsSch.getInt(1) == 0) {
                stmt.execute("""
                    INSERT INTO schedules (schedule_code, category, operating_hours) VALUES
                    ('SCH-24X7', 'Standard Car Parking - 24x7', '24 Hours / 7 Days'),
                    ('SCH-DAY', 'Compact & Sedan Bay - Daytime', '06:00 AM - 10:00 PM'),
                    ('SCH-TWO-WHEEL', 'Two-Wheeler Zone', '24 Hours / 7 Days'),
                    ('SCH-EV', 'EV Fast-Charge Bay', '24 Hours / 7 Days'),
                    ('SCH-VALET', 'Premium Covered & Valet Bay', '24 Hours / 7 Days');
                """);
            }

            // Check if resources exist
            var rsRes = stmt.executeQuery("SELECT COUNT(*) FROM resources;");
            if (rsRes.next() && rsRes.getInt(1) == 0) {
                // Demo Data:
                // R101 – Parking Slot A – ₹499 – 10
                // R102 – Parking Slot B – ₹799 – 5
                // R103 – Parking Slot C – ₹199 – 20
                // R104 – Parking Slot D – ₹1299 – 0
                // R105 – Parking Slot E – ₹1000 – 3
                stmt.execute("""
                    INSERT INTO resources (identifier, name, schedule_id, rate, total_capacity, available_quantity, is_active) VALUES
                    ('R101', 'Parking Slot A', 1, 499.00, 10, 10, 1),
                    ('R102', 'Parking Slot B', 2, 799.00, 5, 5, 1),
                    ('R103', 'Parking Slot C', 3, 199.00, 20, 20, 1),
                    ('R104', 'Parking Slot D', 4, 1299.00, 10, 0, 1),
                    ('R105', 'Parking Slot E', 5, 1000.00, 5, 3, 1);
                """);
            }
        }
    }

    public void resetDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS booking_status_history;");
            stmt.execute("DROP TABLE IF EXISTS reservation_items;");
            stmt.execute("DROP TABLE IF EXISTS reservations;");
            stmt.execute("DROP TABLE IF EXISTS resources;");
            stmt.execute("DROP TABLE IF EXISTS schedules;");
            stmt.execute("DROP TABLE IF EXISTS users;");
            initializeDatabase();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reset database: " + e.getMessage(), e);
        }
    }

    public String getDbUrl() {
        return dbUrl;
    }
}
