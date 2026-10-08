package com.parking.service;

import com.parking.dao.ResourceDao;
import com.parking.dao.ScheduleDao;
import com.parking.dao.UserDao;
import com.parking.exception.UnauthorizedException;
import com.parking.exception.ValidationException;
import com.parking.model.Resource;
import com.parking.model.User;

public class ResourceManagementService {
    private final ResourceDao resourceDao;
    private final ScheduleDao scheduleDao;
    private final UserDao userDao;

    public ResourceManagementService(ResourceDao resourceDao, ScheduleDao scheduleDao, UserDao userDao) {
        this.resourceDao = resourceDao;
        this.scheduleDao = scheduleDao;
        this.userDao = userDao;
    }

    public Resource addSpot(Long adminUserId, Resource resource) {
        authorizeAdmin(adminUserId);
        validate(resource);
        if (resourceDao.findByIdentifier(resource.getIdentifier()).isPresent()) {
            throw new ValidationException("A parking spot with this identifier already exists.");
        }
        return resourceDao.insertManaged(resource);
    }

    public Resource updateSpot(Long adminUserId, Long spotId, Resource updates) {
        authorizeAdmin(adminUserId);
        if (spotId == null || resourceDao.findById(spotId).isEmpty()) {
            throw new ValidationException("Parking spot not found.");
        }
        validate(updates);
        var duplicate = resourceDao.findByIdentifier(updates.getIdentifier());
        if (duplicate.isPresent() && !duplicate.get().getId().equals(spotId)) {
            throw new ValidationException("A parking spot with this identifier already exists.");
        }

        updates.setId(spotId);
        if (!resourceDao.updateManaged(updates)) {
            throw new ValidationException("Total capacity cannot be lower than the number of occupied spots.");
        }
        return resourceDao.findById(spotId)
                .orElseThrow(() -> new ValidationException("Parking spot not found."));
    }

    private void authorizeAdmin(Long adminUserId) {
        if (adminUserId == null) {
            throw new UnauthorizedException("Administrator ID is required.");
        }
        User user = userDao.findById(adminUserId)
                .orElseThrow(() -> new UnauthorizedException("Administrator not found."));
        if (!user.isAdmin()) {
            throw new UnauthorizedException("Access Denied: Only Administrators can manage parking spots.");
        }
    }

    private void validate(Resource resource) {
        if (resource == null) {
            throw new ValidationException("Parking spot details are required.");
        }
        String identifier = resource.getIdentifier() == null ? "" : resource.getIdentifier().trim();
        String name = resource.getName() == null ? "" : resource.getName().trim();
        if (identifier.isEmpty() || identifier.length() > 30) {
            throw new ValidationException("Spot identifier must be between 1 and 30 characters.");
        }
        if (name.isEmpty() || name.length() > 100) {
            throw new ValidationException("Spot name must be between 1 and 100 characters.");
        }
        if (resource.getScheduleId() == null || scheduleDao.findById(resource.getScheduleId()).isEmpty()) {
            throw new ValidationException("Select a valid parking category and schedule.");
        }
        if (!Double.isFinite(resource.getRate()) || resource.getRate() < 0) {
            throw new ValidationException("Rate must be a finite, non-negative amount.");
        }
        if (resource.getTotalCapacity() < 1) {
            throw new ValidationException("Total capacity must be at least one spot.");
        }
        resource.setIdentifier(identifier);
        resource.setName(name);
    }
}
