package com.parking.service;

import com.parking.dao.UserDao;
import com.parking.exception.ValidationException;
import com.parking.model.User;
import com.parking.model.UserRole;

import java.util.List;
import java.util.Optional;

public class AuthService {
    private final UserDao userDao;
    private User currentUser;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public User register(String username, String password, String fullName, String phone) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username cannot be empty.");
        }
        if (password == null || password.trim().isEmpty() || password.length() < 4) {
            throw new ValidationException("Password must be at least 4 characters long.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("Full name cannot be blank.");
        }
        if (phone == null || !phone.trim().matches("^[0-9]{10}$")) {
            throw new ValidationException("Phone number must contain exactly 10 digits.");
        }

        String sanitizedUsername = username.trim().toLowerCase();
        if (userDao.findByUsername(sanitizedUsername).isPresent()) {
            throw new ValidationException("Username '" + sanitizedUsername + "' is already taken. Please choose another.");
        }

        User newUser = new User(sanitizedUsername, password.trim(), fullName.trim(), UserRole.USER, phone.trim());
        User createdUser = userDao.insert(newUser);
        this.currentUser = createdUser;
        return createdUser;
    }

    public User login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username cannot be empty.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("Password cannot be empty.");
        }

        Optional<User> opt = userDao.findByUsername(username.trim().toLowerCase());
        if (opt.isEmpty() || !opt.get().getPassword().equals(password)) {
            throw new ValidationException("Invalid username or password.");
        }

        this.currentUser = opt.get();
        return this.currentUser;
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public List<User> getAllUsers() {
        return userDao.findAll();
    }
}
