package com.pritish.smartbuss;

import java.util.Locale;

// It's important that this class exists and has the necessary methods.
public class BookingWrapper {
    private Booking regularBooking;
    private Booking_for_others forOthersBooking;
    private boolean isForOthers;

    // Constructor for regular bookings
    public BookingWrapper(Booking regularBooking, boolean isForOthers) {
        this.regularBooking = regularBooking;
        this.isForOthers = isForOthers; // Should be false
    }

    // Constructor for bookings made for others
    public BookingWrapper(Booking_for_others forOthersBooking, boolean isForOthers) {
        this.forOthersBooking = forOthersBooking;
        this.isForOthers = isForOthers; // Should be true
    }

    public boolean isForOthers() {
        return isForOthers;
    }

    public Booking getRegularBooking() {
        return regularBooking;
    }

    public Booking_for_others getForOthersBooking() {
        return forOthersBooking;
    }

    // Common getters - these are essential for the adapter
    public String getTransactionId() {
        return isForOthers ? (forOthersBooking != null ? forOthersBooking.getTransactionId() : null)
                : (regularBooking != null ? regularBooking.getTransactionId() : null);
    }

    public String getDate() {
        return isForOthers ? (forOthersBooking != null ? forOthersBooking.getDate() : "N/A")
                : (regularBooking != null ? regularBooking.getDate() : "N/A");
    }

    public String getTime() {
        return isForOthers ? (forOthersBooking != null ? forOthersBooking.getTime() : "N/A")
                : (regularBooking != null ? regularBooking.getTime() : "N/A");
    }

    public String getStartStop() {
        return isForOthers ? (forOthersBooking != null ? forOthersBooking.getStartStop() : "N/A")
                : (regularBooking != null ? regularBooking.getStartStop() : "N/A");
    }

    // DestinationStop might only be in Regular Booking
    public String getDestinationStop() {
        if (isForOthers) {
            return forOthersBooking != null ? forOthersBooking.getDestinationStop() : "N/A";
        } else {
            return regularBooking != null ? regularBooking.getDestinationStop() : "N/A";
        }
    }

    // Price might only be in Regular Booking
// In BookingWrapper.java
    public String getPrice() {
        if (!isForOthers && regularBooking != null) {
            return String.format(Locale.getDefault(), "₹%.2f", regularBooking.getPrice());
        }
        if (isForOthers && forOthersBooking != null) {
            return String.format(Locale.getDefault(), "₹%.2f", forOthersBooking.getPrice());
        }
        return "N/A";
    }

    // Phone number used for QR code and potentially display
    public String getPhoneNumber() {
        return isForOthers ? (forOthersBooking != null ? forOthersBooking.getPassengerPhone() : null)
                : (regularBooking != null ? regularBooking.getPhoneNumber() : null);
    }

    // ScannedAt status
    public String getScannedAt() {
        if (!isForOthers && regularBooking != null) {
            return regularBooking.getScannedAt();
        }
        // If Booking_for_others also can be scanned and has a similar field:
        // return isForOthers ? (forOthersBooking != null ? forOthersBooking.getScannedAt() : null)
        //                    : (regularBooking != null ? regularBooking.getScannedAt() : null);
        return null; // Default if not applicable or not present in forOthersBooking
    }
}