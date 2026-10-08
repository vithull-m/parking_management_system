package com.parking.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Reservation {
    private Long id;
    private String bookingReference;
    private Long userId;
    private String customerName;
    private String customerPhone;
    private String vehicleNumber;
    private String bookingDate; // e.g. 2026-10-08
    private double baseAmount;
    private double bookingCharge;
    private double grandTotal;
    private BookingStatus status;
    private LocalDateTime createdAt;

    private List<ReservationItem> items = new ArrayList<>();
    private List<BookingStatusHistory> statusHistories = new ArrayList<>();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Reservation() {
        this.status = BookingStatus.CONFIRMED;
        this.createdAt = LocalDateTime.now();
    }

    public Reservation(Long id, String bookingReference, Long userId, String customerName,
                       String customerPhone, String vehicleNumber, String bookingDate,
                       double baseAmount, double bookingCharge, double grandTotal,
                       BookingStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.bookingReference = bookingReference;
        this.userId = userId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.vehicleNumber = vehicleNumber;
        this.bookingDate = bookingDate;
        this.baseAmount = baseAmount;
        this.bookingCharge = bookingCharge;
        this.grandTotal = grandTotal;
        this.status = status;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public double getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(double baseAmount) {
        this.baseAmount = baseAmount;
    }

    public double getBookingCharge() {
        return bookingCharge;
    }

    public void setBookingCharge(double bookingCharge) {
        this.bookingCharge = bookingCharge;
    }

    public double getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(double grandTotal) {
        this.grandTotal = grandTotal;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getFormattedCreatedAt() {
        return createdAt != null ? createdAt.format(FORMATTER) : "";
    }

    public List<ReservationItem> getItems() {
        return items;
    }

    public void setItems(List<ReservationItem> items) {
        this.items = items;
    }

    public void addItem(ReservationItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }

    public List<BookingStatusHistory> getStatusHistories() {
        return statusHistories;
    }

    public void setStatusHistories(List<BookingStatusHistory> statusHistories) {
        this.statusHistories = statusHistories;
    }

    public void addStatusHistory(BookingStatusHistory history) {
        if (this.statusHistories == null) {
            this.statusHistories = new ArrayList<>();
        }
        this.statusHistories.add(history);
    }

    public int getTotalQuantity() {
        if (items == null || items.isEmpty()) return 0;
        return items.stream().mapToInt(ReservationItem::getQuantity).sum();
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", ref='" + bookingReference + '\'' +
                ", customer='" + customerName + '\'' +
                ", grandTotal=" + grandTotal +
                ", status=" + status +
                '}';
    }
}
