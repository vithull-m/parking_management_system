package com.parking.dao;

import com.parking.db.DatabaseManager;
import com.parking.model.BookingStatus;
import com.parking.model.Reservation;
import com.parking.model.ReservationItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationDao {
    private final DatabaseManager dbManager;
    private final BookingStatusHistoryDao historyDao;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ReservationDao(DatabaseManager dbManager, BookingStatusHistoryDao historyDao) {
        this.dbManager = dbManager;
        this.historyDao = historyDao;
    }

    public Reservation insert(Connection conn, Reservation reservation) throws SQLException {
        String sql = """
            INSERT INTO reservations (booking_reference, user_id, customer_name, customer_phone, vehicle_number,
                                     booking_date, base_amount, booking_charge, grand_total, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, reservation.getBookingReference());
            ps.setLong(2, reservation.getUserId());
            ps.setString(3, reservation.getCustomerName());
            ps.setString(4, reservation.getCustomerPhone());
            ps.setString(5, reservation.getVehicleNumber());
            ps.setString(6, reservation.getBookingDate());
            ps.setDouble(7, reservation.getBaseAmount());
            ps.setDouble(8, reservation.getBookingCharge());
            ps.setDouble(9, reservation.getGrandTotal());
            ps.setString(10, reservation.getStatus().name());
            ps.setString(11, reservation.getCreatedAt().format(FORMATTER));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong(1));
                }
            }
        }

        // Insert items
        if (reservation.getItems() != null && !reservation.getItems().isEmpty()) {
            String itemSql = """
                INSERT INTO reservation_items (reservation_id, resource_id, resource_identifier_snapshot,
                                              resource_name_snapshot, category_snapshot, rate_snapshot, quantity, subtotal)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
            try (PreparedStatement psItem = conn.prepareStatement(itemSql, Statement.RETURN_GENERATED_KEYS)) {
                for (ReservationItem item : reservation.getItems()) {
                    psItem.setLong(1, reservation.getId());
                    psItem.setLong(2, item.getResourceId());
                    psItem.setString(3, item.getResourceIdentifierSnapshot());
                    psItem.setString(4, item.getResourceNameSnapshot());
                    psItem.setString(5, item.getCategorySnapshot());
                    psItem.setDouble(6, item.getRateSnapshot());
                    psItem.setInt(7, item.getQuantity());
                    psItem.setDouble(8, item.getSubtotal());
                    psItem.executeUpdate();
                    try (ResultSet rs = psItem.getGeneratedKeys()) {
                        if (rs.next()) {
                            item.setId(rs.getLong(1));
                        }
                    }
                }
            }
        }

        return reservation;
    }

    public Optional<Reservation> findById(Long id) {
        String sql = """
            SELECT id, booking_reference, user_id, customer_name, customer_phone, vehicle_number,
                   booking_date, base_amount, booking_charge, grand_total, status, created_at
            FROM reservations
            WHERE id = ?
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Reservation res = mapRow(rs);
                    loadItems(conn, res);
                    res.setStatusHistories(historyDao.findByReservationId(res.getId()));
                    return Optional.of(res);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding reservation by id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<Reservation> findByIdForUpdate(Connection conn, Long id) throws SQLException {
        String sql = """
            SELECT id, booking_reference, user_id, customer_name, customer_phone, vehicle_number,
                   booking_date, base_amount, booking_charge, grand_total, status, created_at
            FROM reservations
            WHERE id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Reservation res = mapRow(rs);
                    loadItems(conn, res);
                    return Optional.of(res);
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Reservation> findByReference(String bookingReference) {
        String sql = """
            SELECT id, booking_reference, user_id, customer_name, customer_phone, vehicle_number,
                   booking_date, base_amount, booking_charge, grand_total, status, created_at
            FROM reservations
            WHERE UPPER(booking_reference) = UPPER(?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bookingReference);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Reservation res = mapRow(rs);
                    loadItems(conn, res);
                    res.setStatusHistories(historyDao.findByReservationId(res.getId()));
                    return Optional.of(res);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding reservation by reference: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<Reservation> findByUserId(Long userId) {
        List<Reservation> list = new ArrayList<>();
        String sql = """
            SELECT id, booking_reference, user_id, customer_name, customer_phone, vehicle_number,
                   booking_date, base_amount, booking_charge, grand_total, status, created_at
            FROM reservations
            WHERE user_id = ?
            ORDER BY id DESC
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reservation res = mapRow(rs);
                    loadItems(conn, res);
                    list.add(res);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user reservations: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Reservation> findAll() {
        List<Reservation> list = new ArrayList<>();
        String sql = """
            SELECT id, booking_reference, user_id, customer_name, customer_phone, vehicle_number,
                   booking_date, base_amount, booking_charge, grand_total, status, created_at
            FROM reservations
            ORDER BY id DESC
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Reservation res = mapRow(rs);
                loadItems(conn, res);
                list.add(res);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all reservations: " + e.getMessage(), e);
        }
        return list;
    }

    public boolean updateStatus(Connection conn, Long reservationId, BookingStatus newStatus) throws SQLException {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setLong(2, reservationId);
            return ps.executeUpdate() > 0;
        }
    }

    private void loadItems(Connection conn, Reservation res) throws SQLException {
        String sql = """
            SELECT id, reservation_id, resource_id, resource_identifier_snapshot,
                   resource_name_snapshot, category_snapshot, rate_snapshot, quantity, subtotal
            FROM reservation_items
            WHERE reservation_id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, res.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReservationItem item = new ReservationItem();
                    item.setId(rs.getLong("id"));
                    item.setReservationId(rs.getLong("reservation_id"));
                    item.setResourceId(rs.getLong("resource_id"));
                    item.setResourceIdentifierSnapshot(rs.getString("resource_identifier_snapshot"));
                    item.setResourceNameSnapshot(rs.getString("resource_name_snapshot"));
                    item.setCategorySnapshot(rs.getString("category_snapshot"));
                    item.setRateSnapshot(rs.getDouble("rate_snapshot"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setSubtotal(rs.getDouble("subtotal"));
                    res.addItem(item);
                }
            }
        }
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getLong("id"));
        r.setBookingReference(rs.getString("booking_reference"));
        r.setUserId(rs.getLong("user_id"));
        r.setCustomerName(rs.getString("customer_name"));
        r.setCustomerPhone(rs.getString("customer_phone"));
        r.setVehicleNumber(rs.getString("vehicle_number"));
        r.setBookingDate(rs.getString("booking_date"));
        r.setBaseAmount(rs.getDouble("base_amount"));
        r.setBookingCharge(rs.getDouble("booking_charge"));
        r.setGrandTotal(rs.getDouble("grand_total"));
        r.setStatus(BookingStatus.valueOf(rs.getString("status")));
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            r.setCreatedAt(LocalDateTime.parse(createdAtStr, FORMATTER));
        }
        return r;
    }
}
