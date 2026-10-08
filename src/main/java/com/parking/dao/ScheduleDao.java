package com.parking.dao;

import com.parking.db.DatabaseManager;
import com.parking.model.Schedule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ScheduleDao {
    private final DatabaseManager dbManager;

    public ScheduleDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public Optional<Schedule> findById(Long id) {
        String sql = "SELECT id, schedule_code, category, operating_hours FROM schedules WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding schedule by id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<Schedule> findAll() {
        List<Schedule> list = new ArrayList<>();
        String sql = "SELECT id, schedule_code, category, operating_hours FROM schedules ORDER BY id ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all schedules: " + e.getMessage(), e);
        }
        return list;
    }

    private Schedule mapRow(ResultSet rs) throws SQLException {
        Schedule s = new Schedule();
        s.setId(rs.getLong("id"));
        s.setScheduleCode(rs.getString("schedule_code"));
        s.setCategory(rs.getString("category"));
        s.setOperatingHours(rs.getString("operating_hours"));
        return s;
    }
}
