package com.parking.model;

public class FeeBreakdown {
    private final double unitRate;
    private final int quantity;
    private final double baseAmount;
    private final double bookingCharge;
    private final double grandTotal;

    public FeeBreakdown(double unitRate, int quantity) {
        this.unitRate = unitRate;
        this.quantity = quantity;
        this.baseAmount = unitRate * quantity;
        // Rule: ₹50 if base < ₹1000, otherwise ₹0
        this.bookingCharge = (this.baseAmount < 1000.0) ? 50.0 : 0.0;
        this.grandTotal = this.baseAmount + this.bookingCharge;
    }

    public static FeeBreakdown calculate(double unitRate, int quantity) {
        return new FeeBreakdown(unitRate, quantity);
    }

    public double getUnitRate() {
        return unitRate;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getBaseAmount() {
        return baseAmount;
    }

    public double getBookingCharge() {
        return bookingCharge;
    }

    public double getGrandTotal() {
        return grandTotal;
    }

    @Override
    public String toString() {
        return String.format("Base: ₹%.2f, Booking Charge: ₹%.2f, Grand Total: ₹%.2f",
                baseAmount, bookingCharge, grandTotal);
    }
}
