package com.parking.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BookingStatusHistory {
    private Long id;
    private Long reservationId;
    private BookingStatus fromStatus;
    private BookingStatus toStatus;
    private Long changedByUserId;
    private String changedByUsername; // populated for display
    private UserRole changedByRole;
    private String remarks;
    private LocalDateTime changedAt;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public BookingStatusHistory() {
        this.changedAt = LocalDateTime.now();
    }

    public BookingStatusHistory(Long id, Long reservationId, BookingStatus fromStatus, BookingStatus toStatus,
                                Long changedByUserId, String changedByUsername, UserRole changedByRole,
                                String remarks, LocalDateTime changedAt) {
        this.id = id;
        this.reservationId = reservationId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedByUserId = changedByUserId;
        this.changedByUsername = changedByUsername;
        this.changedByRole = changedByRole;
        this.remarks = remarks;
        this.changedAt = changedAt != null ? changedAt : LocalDateTime.now();
    }

    public BookingStatusHistory(Long reservationId, BookingStatus fromStatus, BookingStatus toStatus,
                                Long changedByUserId, UserRole changedByRole, String remarks) {
        this(null, reservationId, fromStatus, toStatus, changedByUserId, null, changedByRole, remarks, LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public BookingStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(BookingStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public BookingStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(BookingStatus toStatus) {
        this.toStatus = toStatus;
    }

    public Long getChangedByUserId() {
        return changedByUserId;
    }

    public void setChangedByUserId(Long changedByUserId) {
        this.changedByUserId = changedByUserId;
    }

    public String getChangedByUsername() {
        return changedByUsername;
    }

    public void setChangedByUsername(String changedByUsername) {
        this.changedByUsername = changedByUsername;
    }

    public UserRole getChangedByRole() {
        return changedByRole;
    }

    public void setChangedByRole(UserRole changedByRole) {
        this.changedByRole = changedByRole;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getFormattedChangedAt() {
        return changedAt != null ? changedAt.format(FORMATTER) : "";
    }

    @Override
    public String toString() {
        return "BookingStatusHistory{" +
                "id=" + id +
                ", reservationId=" + reservationId +
                ", from=" + fromStatus +
                ", to=" + toStatus +
                ", changedBy=" + changedByRole +
                ", at=" + getFormattedChangedAt() +
                '}';
    }
}
