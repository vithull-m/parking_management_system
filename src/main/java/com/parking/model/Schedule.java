package com.parking.model;

import java.util.Objects;

public class Schedule {
    private Long id;
    private String scheduleCode;
    private String category;
    private String operatingHours;

    public Schedule() {
    }

    public Schedule(Long id, String scheduleCode, String category, String operatingHours) {
        this.id = id;
        this.scheduleCode = scheduleCode;
        this.category = category;
        this.operatingHours = operatingHours;
    }

    public Schedule(String scheduleCode, String category, String operatingHours) {
        this(null, scheduleCode, category, operatingHours);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getScheduleCode() {
        return scheduleCode;
    }

    public void setScheduleCode(String scheduleCode) {
        this.scheduleCode = scheduleCode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOperatingHours() {
        return operatingHours;
    }

    public void setOperatingHours(String operatingHours) {
        this.operatingHours = operatingHours;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Schedule schedule = (Schedule) o;
        return Objects.equals(id, schedule.id) && Objects.equals(scheduleCode, schedule.scheduleCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, scheduleCode);
    }

    @Override
    public String toString() {
        return category + " (" + operatingHours + ")";
    }
}
