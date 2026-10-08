package com.parking.model;

import java.time.LocalDate;

public class BookingRequest {
    private Long userId;
    private Long resourceId;
    private int quantity;
    private String customerName;
    private String customerPhone;
    private String vehicleNumber;
    private String bookingDate;

    public BookingRequest() {
        this.bookingDate = LocalDate.now().toString();
    }

    public BookingRequest(Long userId, Long resourceId, int quantity, String customerName,
                          String customerPhone, String vehicleNumber, String bookingDate) {
        this.userId = userId;
        this.resourceId = resourceId;
        this.quantity = quantity;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.vehicleNumber = vehicleNumber;
        this.bookingDate = (bookingDate != null && !bookingDate.trim().isEmpty())
                ? bookingDate.trim()
                : LocalDate.now().toString();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }
}
