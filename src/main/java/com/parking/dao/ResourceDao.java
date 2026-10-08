package com.parking.dao;

import com.parking.db.DatabaseManager;
import com.parking.model.Resource;
import com.parking.model.Schedule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResourceDao {
    private final DatabaseManager dbManager;

    public ResourceDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public Optional<Resource> findById(Long id) {
        String sql = """
            SELECT r.id, r.identifier, r.name, r.schedule_id, r.rate, r.total_capacity, r.available_quantity, r.is_active,
                   s.schedule_code, s.category, s.operating_hours
            FROM resources r
            JOIN schedules s ON r.schedule_id = s.id
            WHERE r.id = ?
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding resource by id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<Resource> findByIdentifier(String identifier) {
        String sql = """
            SELECT r.id, r.identifier, r.name, r.schedule_id, r.rate, r.total_capacity, r.available_quantity, r.is_active,
                   s.schedule_code, s.category, s.operating_hours
            FROM resources r
            JOIN schedules s ON r.schedule_id = s.id
            WHERE UPPER(r.identifier) = UPPER(?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, identifier);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding resource by identifier: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<Resource> findByIdForUpdate(Connection conn, Long id) throws SQLException {
        String sql = """
            SELECT r.id, r.identifier, r.name, r.schedule_id, r.rate, r.total_capacity, r.available_quantity, r.is_active,
                   s.schedule_code, s.category, s.operating_hours
            FROM resources r
            JOIN schedules s ON r.schedule_id = s.id
            WHERE r.id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Resource> findAll() {
        List<Resource> list = new ArrayList<>();
        String sql = """
            SELECT r.id, r.identifier, r.name, r.schedule_id, r.rate, r.total_capacity, r.available_quantity, r.is_active,
                   s.schedule_code, s.category, s.operating_hours
            FROM resources r
            JOIN schedules s ON r.schedule_id = s.id
            ORDER BY r.identifier ASC
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all resources: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Resource> searchActive(String keyword) {
        List<Resource> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT r.id, r.identifier, r.name, r.schedule_id, r.rate, r.total_capacity, r.available_quantity, r.is_active,
                   s.schedule_code, s.category, s.operating_hours
            FROM resources r
            JOIN schedules s ON r.schedule_id = s.id
            WHERE r.is_active = 1
        """);

        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        if (hasKeyword) {
            sql.append(" AND (UPPER(r.identifier) LIKE ? OR UPPER(r.name) LIKE ? OR UPPER(s.category) LIKE ?)");
        }
        sql.append(" ORDER BY r.identifier ASC");

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (hasKeyword) {
                String pattern = "%" + keyword.trim().toUpperCase() + "%";
                ps.setString(1, pattern);
                ps.setString(2, pattern);
                ps.setString(3, pattern);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error searching active resources: " + e.getMessage(), e);
        }
        return list;
    }

    public boolean reduceAvailability(Connection conn, Long resourceId, int quantity) throws SQLException {
        String sql = """
            UPDATE resources
            SET available_quantity = available_quantity - ?
            WHERE id = ? AND available_quantity >= ? AND is_active = 1
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setLong(2, resourceId);
            ps.setInt(3, quantity);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    public boolean restoreAvailability(Connection conn, Long resourceId, int quantity) throws SQLException {
        String sql = """
            UPDATE resources
            SET available_quantity = MIN(total_capacity, available_quantity + ?)
            WHERE id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setLong(2, resourceId);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    public void updateRate(Long resourceId, double newRate) {
        String sql = "UPDATE resources SET rate = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newRate);
            ps.setLong(2, resourceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating resource rate: " + e.getMessage(), e);
        }
    }

    public Resource insertManaged(Resource resource) {
        String sql = """
            INSERT INTO resources (identifier, name, schedule_id, rate, total_capacity, available_quantity, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, resource.getIdentifier());
            ps.setString(2, resource.getName());
            ps.setLong(3, resource.getScheduleId());
            ps.setDouble(4, resource.getRate());
            ps.setInt(5, resource.getTotalCapacity());
            ps.setInt(6, resource.getTotalCapacity());
            ps.setInt(7, resource.isActive() ? 1 : 0);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
            throw new SQLException("No ID was returned for the new parking spot.");
        } catch (SQLException e) {
            throw new RuntimeException("Error creating parking spot: " + e.getMessage(), e);
        }
    }

    public boolean updateManaged(Resource resource) {
        String sql = """
            UPDATE resources
            SET identifier = ?, name = ?, schedule_id = ?, rate = ?,
                available_quantity = ? - (total_capacity - available_quantity),
                total_capacity = ?, is_active = ?
            WHERE id = ? AND total_capacity - available_quantity <= ?
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resource.getIdentifier());
            ps.setString(2, resource.getName());
            ps.setLong(3, resource.getScheduleId());
            ps.setDouble(4, resource.getRate());
            ps.setInt(5, resource.getTotalCapacity());
            ps.setInt(6, resource.getTotalCapacity());
            ps.setInt(7, resource.isActive() ? 1 : 0);
            ps.setLong(8, resource.getId());
            ps.setInt(9, resource.getTotalCapacity());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating parking spot: " + e.getMessage(), e);
        }
    }

    private Resource mapRow(ResultSet rs) throws SQLException {
        Resource r = new Resource();
        r.setId(rs.getLong("id"));
        r.setIdentifier(rs.getString("identifier"));
        r.setName(rs.getString("name"));
        r.setScheduleId(rs.getLong("schedule_id"));
        r.setRate(rs.getDouble("rate"));
        r.setTotalCapacity(rs.getInt("total_capacity"));
        r.setAvailableQuantity(rs.getInt("available_quantity"));
        r.setActive(rs.getInt("is_active") == 1);

        Schedule s = new Schedule();
        s.setId(rs.getLong("schedule_id"));
        s.setScheduleCode(rs.getString("schedule_code"));
        s.setCategory(rs.getString("category"));
        s.setOperatingHours(rs.getString("operating_hours"));
        r.setSchedule(s);

        return r;
    }
}
