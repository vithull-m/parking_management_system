package com.parking.service;

import com.parking.dao.ResourceDao;
import com.parking.model.Resource;

import java.util.List;
import java.util.Optional;

public class AvailabilityService {
    private final ResourceDao resourceDao;

    public AvailabilityService(ResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

    /**
     * Search active parking slots with identifier, category/schedule, rate and availability.
     */
    public List<Resource> searchActiveSlots(String keyword) {
        return resourceDao.searchActive(keyword);
    }

    /**
     * Get all active slots.
     */
    public List<Resource> getAllActiveSlots() {
        return resourceDao.searchActive(null);
    }

    /**
     * Get slot by ID.
     */
    public Optional<Resource> getSlotById(Long id) {
        return resourceDao.findById(id);
    }

    /**
     * Get slot by Identifier (e.g., R101).
     */
    public Optional<Resource> getSlotByIdentifier(String identifier) {
        return resourceDao.findByIdentifier(identifier);
    }

    /**
     * Check if a slot is active and has sufficient available quantity.
     */
    public boolean isSlotAvailable(Long slotId, int requestedQuantity) {
        if (requestedQuantity <= 0) {
            return false;
        }
        Optional<Resource> opt = resourceDao.findById(slotId);
        return opt.isPresent() && opt.get().isActive() && opt.get().getAvailableQuantity() >= requestedQuantity;
    }
}
