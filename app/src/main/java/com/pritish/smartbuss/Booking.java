package com.pritish.smartbuss;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class Booking {
    // Required fields
    private String transactionId;
    private String startStop;
    private String destinationStop;
    private String phoneNumber;
    private float price;  // Changed from String to float
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

    // Default constructor (required for Firebase)
    public Booking() {
        this.status = "valid";
        this.validUntil = System.currentTimeMillis() + (12 * 60 * 60 * 1000L);
        updateDerivedFields();
    }

    // Main constructor with personCount
    public Booking(String transactionId, String startStop, String destinationStop,
                   String phoneNumber, float price, String date, String time, int personCount) {
        this();
        this.transactionId = transactionId;
        this.startStop = startStop;
        this.destinationStop = destinationStop;
        this.phoneNumber = phoneNumber;
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
        String currentPhoneNumber = (this.phoneNumber != null) ? this.phoneNumber : "UNKNOWN_PHONE";

        this.qrCodeData = String.format("SMARTBUS|%s|%s|%s|%s",
                currentTransactionId,
                currentPhoneNumber,
                expiryTimestamp,
                combinedStopHash);

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

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        generateSecurityData();
    }

    public float getPrice() { return price; }  // Changed return type to float
    public void setPrice(float price) { this.price = price; }  // Changed parameter type to float

    // Added method to get price as formatted string
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
        Booking booking = (Booking) o;
        return Objects.equals(transactionId, booking.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        boolean currentValidity = isValid();
        return "Booking{" +
                "transactionId='" + transactionId + '\'' +
                ", startStop='" + startStop + '\'' +
                ", destinationStop='" + destinationStop + '\'' +
                ", phoneNumber='" + (phoneNumber != null && phoneNumber.length() > 3 ?
                "***" + phoneNumber.substring(phoneNumber.length() - 3) : "N/A") + '\'' +
                ", price=" + getFormattedPrice() +  // Updated to use formatted price
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", personCount=" + personCount +
                ", status='" + status + '\'' +
                ", valid=" + currentValidity +
                ", validUntil=" + getFormattedValidUntil() +
                ", scannedAt='" + (scannedAt != null ? scannedAt : "Not scanned") + '\'' +
                ", qrCodeDataPresent=" + (qrCodeData != null && !qrCodeData.isEmpty()) +
                ", verificationTokenPresent=" + (verificationToken != null && !verificationToken.isEmpty()) +
                '}';
    }
}