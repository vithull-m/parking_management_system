package com.parking.model;

import java.util.Objects;

public class Resource {
    private Long id;
    private String identifier; // e.g. R101, R102
    private String name;       // e.g. Parking Slot A
    private Long scheduleId;
    private Schedule schedule; // populated via join
    private double rate;       // e.g. 499.0
    private int totalCapacity; // e.g. 10
    private int availableQuantity; // e.g. 10
    private boolean active;

    public Resource() {
        this.active = true;
    }

    public Resource(Long id, String identifier, String name, Long scheduleId, double rate, int totalCapacity, int availableQuantity, boolean active) {
        this.id = id;
        this.identifier = identifier;
        this.name = name;
        this.scheduleId = scheduleId;
        this.rate = rate;
        this.totalCapacity = totalCapacity;
        this.availableQuantity = availableQuantity;
        this.active = active;
    }

    public Resource(String identifier, String name, Long scheduleId, double rate, int totalCapacity, int availableQuantity, boolean active) {
        this(null, identifier, name, scheduleId, rate, totalCapacity, availableQuantity, active);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
        if (schedule != null) {
            this.scheduleId = schedule.getId();
        }
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(int totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isAvailable(int requiredQuantity) {
        return active && availableQuantity >= requiredQuantity && requiredQuantity > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resource resource = (Resource) o;
        return Objects.equals(id, resource.id) && Objects.equals(identifier, resource.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, identifier);
    }

    @Override
    public String toString() {
        return identifier + " - " + name + " (₹" + rate + ", Avail: " + availableQuantity + ")";
    }
}
