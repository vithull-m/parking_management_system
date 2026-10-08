package com.parking.model;

public class ReservationItem {
    private Long id;
    private Long reservationId;
    private Long resourceId;
    private String resourceIdentifierSnapshot;
    private String resourceNameSnapshot;
    private String categorySnapshot;
    private double rateSnapshot;
    private int quantity;
    private double subtotal;

    public ReservationItem() {
    }

    public ReservationItem(Long id, Long reservationId, Long resourceId, String resourceIdentifierSnapshot,
                           String resourceNameSnapshot, String categorySnapshot, double rateSnapshot,
                           int quantity, double subtotal) {
        this.id = id;
        this.reservationId = reservationId;
        this.resourceId = resourceId;
        this.resourceIdentifierSnapshot = resourceIdentifierSnapshot;
        this.resourceNameSnapshot = resourceNameSnapshot;
        this.categorySnapshot = categorySnapshot;
        this.rateSnapshot = rateSnapshot;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    public ReservationItem(Long resourceId, String resourceIdentifierSnapshot, String resourceNameSnapshot,
                           String categorySnapshot, double rateSnapshot, int quantity) {
        this(null, null, resourceId, resourceIdentifierSnapshot, resourceNameSnapshot,
                categorySnapshot, rateSnapshot, quantity, rateSnapshot * quantity);
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

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceIdentifierSnapshot() {
        return resourceIdentifierSnapshot;
    }

    public void setResourceIdentifierSnapshot(String resourceIdentifierSnapshot) {
        this.resourceIdentifierSnapshot = resourceIdentifierSnapshot;
    }

    public String getResourceNameSnapshot() {
        return resourceNameSnapshot;
    }

    public void setResourceNameSnapshot(String resourceNameSnapshot) {
        this.resourceNameSnapshot = resourceNameSnapshot;
    }

    public String getCategorySnapshot() {
        return categorySnapshot;
    }

    public void setCategorySnapshot(String categorySnapshot) {
        this.categorySnapshot = categorySnapshot;
    }

    public double getRateSnapshot() {
        return rateSnapshot;
    }

    public void setRateSnapshot(double rateSnapshot) {
        this.rateSnapshot = rateSnapshot;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    @Override
    public String toString() {
        return resourceIdentifierSnapshot + " (" + resourceNameSnapshot + ") x " + quantity + " @ ₹" + rateSnapshot + " = ₹" + subtotal;
    }
}
