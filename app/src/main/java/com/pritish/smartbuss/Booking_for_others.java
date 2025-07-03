package com.pritish.smartbuss;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class Booking_for_others {
    // Required fields
    private String transactionId;
    private String startStop;
    private String destinationStop;
    private String passengerPhone;
    private String bookedByPhone;
    private float price;
    private String date;
    private String time;
    private int personCount;

    // Status fields
    private String scannedAt;
    private String status;
    private long validUntil;
    private boolean valid;

    // Security fields
    private String qrCodeData;
    private String verificationToken;
    private String ticketKey; // New field for SMS verification

    // Default constructor (required for Firebase)
    public Booking_for_others() {
        this.status = "valid";
        // Ticket is valid for 12 hours from creation
        this.validUntil = System.currentTimeMillis() + (12 * 60 * 60 * 1000L);
        updateDerivedFields();
    }

    // Main constructor with personCount and bookedBy
    public Booking_for_others(String transactionId, String startStop, String destinationStop,
                              String passengerPhone, float price, String date,
                              String time, int personCount, String bookedByPhone) {
        this(); // Call default constructor
        this.transactionId = transactionId;
        this.startStop = startStop;
        this.destinationStop = destinationStop;
        this.passengerPhone = passengerPhone;
        this.bookedByPhone = bookedByPhone;
        this.price = price;
        this.date = date;
        this.time = time;
        this.personCount = personCount;
        generateSecurityData();
        updateDerivedFields();
    }

    private void generateSecurityData() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        String expiryTimestamp = "INVALID_EXPIRY";

        if (this.validUntil > 0) {
            expiryTimestamp = sdf.format(new Date(this.validUntil));
        }

        int sStopHash = (this.startStop != null) ? this.startStop.hashCode() : 0;
        int dStopHash = (this.destinationStop != null) ? this.destinationStop.hashCode() : 0;
        int combinedStopHash = sStopHash + dStopHash;

        String currentTransactionId = (this.transactionId != null) ? this.transactionId : "UNKNOWN_ID";
        String currentPassengerPhone = (this.passengerPhone != null) ? this.passengerPhone : "UNKNOWN_PHONE";

        this.qrCodeData = String.format("SMARTBUS|%s|%s|%s|%s|%s",
                currentTransactionId,
                currentPassengerPhone,
                expiryTimestamp,
                combinedStopHash,
                this.bookedByPhone != null ? this.bookedByPhone : "UNKNOWN_BOOKER");

        if (this.verificationToken == null) {
            this.verificationToken = UUID.randomUUID().toString();
        }
    }

    public void updateDerivedFields() {
        this.valid = "valid".equals(this.status) && System.currentTimeMillis() < this.validUntil;
    }

    // Getters and Setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        generateSecurityData();
    }

    public String getStartStop() { return startStop; }
    public void setStartStop(String startStop) {
        this.startStop = startStop;
        generateSecurityData();
    }

    public String getDestinationStop() { return destinationStop; }
    public void setDestinationStop(String destinationStop) {
        this.destinationStop = destinationStop;
        generateSecurityData();
    }

    public String getPassengerPhone() { return passengerPhone; }
    public void setPassengerPhone(String passengerPhone) {
        this.passengerPhone = passengerPhone;
        generateSecurityData();
    }

    public String getBookedByPhone() { return bookedByPhone; }
    public void setBookedByPhone(String bookedByPhone) {
        this.bookedByPhone = bookedByPhone;
        generateSecurityData();
    }

    public float getPrice() { return price; }
    public void setPrice(float price) { this.price = price; }

    public String getFormattedPrice() {
        return String.format(Locale.getDefault(), "%.2f", price);
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public int getPersonCount() { return personCount; }
    public void setPersonCount(int personCount) { this.personCount = personCount; }

    public String getScannedAt() { return scannedAt; }
    public void setScannedAt(String scannedAt) { this.scannedAt = scannedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        updateDerivedFields();
    }

    public long getValidUntil() { return validUntil; }
    public void setValidUntil(long validUntil) {
        this.validUntil = validUntil;
        generateSecurityData();
        updateDerivedFields();
    }

    public boolean isValid() {
        updateDerivedFields();
        return valid;
    }
    public void setValid(boolean valid) { this.valid = valid; }

    public String getQrCodeData() { return qrCodeData; }
    public void setQrCodeData(String qrCodeData) { this.qrCodeData = qrCodeData; }

    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }

    // Getter and Setter for the new field
    public String getTicketKey() { return ticketKey; }
    public void setTicketKey(String ticketKey) { this.ticketKey = ticketKey; }

    public String getFormattedValidUntil() {
        if (this.validUntil <= 0) return "N/A";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .format(new Date(this.validUntil));
    }

    public void markAsUsed() {
        this.status = "used";
        this.scannedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .format(new Date());
        updateDerivedFields();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking_for_others booking = (Booking_for_others) o;
        return Objects.equals(transactionId, booking.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        boolean currentValidity = isValid();
        return "Booking_for_others{" +
                "transactionId='" + transactionId + '\'' +
                ", start='" + startStop + '\'' +
                ", destination='" + destinationStop + '\'' +
                ", passengerPhone='" + (passengerPhone != null && passengerPhone.length() > 3 ?
                "***" + passengerPhone.substring(passengerPhone.length() - 3) : "N/A") + '\'' +
                ", booked by='" + (bookedByPhone != null && bookedByPhone.length() > 3 ?
                "***" + bookedByPhone.substring(bookedByPhone.length() - 3) : "N/A") + '\'' +
                ", price=" + getFormattedPrice() +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", personCount=" + personCount +
                ", status='" + status + '\'' +
                ", valid=" + currentValidity +
                ", validUntil=" + getFormattedValidUntil() +
                ", scannedAt='" + (scannedAt != null ? scannedAt : "Not scanned") + '\'' +
                ", qrCodeDataPresent=" + (qrCodeData != null && !qrCodeData.isEmpty()) +
                ", verificationTokenPresent=" + (verificationToken != null && !verificationToken.isEmpty()) +
                ", ticketKey='" + (ticketKey != null ? ticketKey : "N/A") + '\'' + // Added for debugging
                '}';
    }
}