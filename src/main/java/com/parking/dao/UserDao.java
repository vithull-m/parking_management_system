package com.parking.dao;

import com.parking.db.DatabaseManager;
import com.parking.model.User;
import com.parking.model.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {
    private final DatabaseManager dbManager;

    public UserDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public User insert(User user) {
        String sql = "INSERT INTO users (username, password, full_name, role, phone) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole().name());
            ps.setString(5, user.getPhone());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getLong(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Error creating user: " + e.getMessage(), e);
        }
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT id, username, password, full_name, role, phone FROM users WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password, full_name, role, phone FROM users WHERE username = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by username: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, password, full_name, role, phone FROM users ORDER BY id ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all users: " + e.getMessage(), e);
        }
        return list;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(UserRole.valueOf(rs.getString("role")));
        u.setPhone(rs.getString("phone"));
        return u;
    }
}
