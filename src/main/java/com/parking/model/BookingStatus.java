package com.parking.model;

public enum BookingStatus {
    CONFIRMED("Confirmed", "Booking confirmed and slot reserved"),
    CHECKED_IN("Checked-In", "Vehicle arrived and checked-in at parking bay"),
    COMPLETED("Completed", "Vehicle departed and parking session completed"),
    CANCELLED("Cancelled", "Booking cancelled and slot restored");

    private final String displayName;
    private final String description;

    BookingStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determines if the transition from this status to nextStatus is allowed for an Admin.
     * Flow: CONFIRMED -> CHECKED_IN -> COMPLETED (cannot skip stages).
     */
    public boolean isValidAdminTransition(BookingStatus nextStatus) {
        if (this == CONFIRMED && nextStatus == CHECKED_IN) {
            return true;
        }
        if (this == CHECKED_IN && nextStatus == COMPLETED) {
            return true;
        }
        return false;
    }

    /**
     * Returns the next sequential operational status for Admin, or null if at end or terminal.
     */
    public BookingStatus getNextAdminStatus() {
        if (this == CONFIRMED) {
            return CHECKED_IN;
        }
        if (this == CHECKED_IN) {
            return COMPLETED;
        }
        return null;
    }

    /**
     * Determines if this booking is eligible for cancellation by the user.
     * User can cancel only CONFIRMED bookings.
     */
    public boolean isCancellableByUser() {
        return this == CONFIRMED;
    }
}
