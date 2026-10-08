package com.parking.dao;

import com.parking.db.DatabaseManager;
import com.parking.model.BookingStatus;
import com.parking.model.BookingStatusHistory;
import com.parking.model.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BookingStatusHistoryDao {
    private final DatabaseManager dbManager;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public BookingStatusHistoryDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public BookingStatusHistory insert(Connection conn, BookingStatusHistory history) throws SQLException {
        String sql = """
            INSERT INTO booking_status_history (reservation_id, from_status, to_status, changed_by_user_id, changed_by_role, remarks, changed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, history.getReservationId());
            ps.setString(2, history.getFromStatus() != null ? history.getFromStatus().name() : null);
            ps.setString(3, history.getToStatus().name());
            ps.setLong(4, history.getChangedByUserId());
            ps.setString(5, history.getChangedByRole().name());
            ps.setString(6, history.getRemarks());
            ps.setString(7, history.getChangedAt().format(FORMATTER));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    history.setId(rs.getLong(1));
                }
            }
        }
        return history;
    }

    public List<BookingStatusHistory> findByReservationId(Long reservationId) {
        List<BookingStatusHistory> list = new ArrayList<>();
        String sql = """
            SELECT h.id, h.reservation_id, h.from_status, h.to_status, h.changed_by_user_id,
                   u.username AS changed_by_username, h.changed_by_role, h.remarks, h.changed_at
            FROM booking_status_history h
            LEFT JOIN users u ON h.changed_by_user_id = u.id
            WHERE h.reservation_id = ?
            ORDER BY h.id ASC
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding status history: " + e.getMessage(), e);
        }
        return list;
    }

    private BookingStatusHistory mapRow(ResultSet rs) throws SQLException {
        BookingStatusHistory h = new BookingStatusHistory();
        h.setId(rs.getLong("id"));
        h.setReservationId(rs.getLong("reservation_id"));
        String fromStr = rs.getString("from_status");
        if (fromStr != null) {
            h.setFromStatus(BookingStatus.valueOf(fromStr));
        }
        h.setToStatus(BookingStatus.valueOf(rs.getString("to_status")));
        h.setChangedByUserId(rs.getLong("changed_by_user_id"));
        h.setChangedByUsername(rs.getString("changed_by_username"));
        h.setChangedByRole(UserRole.valueOf(rs.getString("changed_by_role")));
        h.setRemarks(rs.getString("remarks"));
        String changedAtStr = rs.getString("changed_at");
        if (changedAtStr != null) {
            h.setChangedAt(LocalDateTime.parse(changedAtStr, FORMATTER));
        }
        return h;
    }
}
