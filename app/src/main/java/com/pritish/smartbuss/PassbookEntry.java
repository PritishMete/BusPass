package com.pritish.smartbuss;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class PassbookEntry {
    private String transactionId; // Unique ID for this passbook entry
    private String type; // "DEBIT" or "CREDIT"
    private double amount; // Always positive, type determines if it's added or subtracted
    private String date; // "yyyy-MM-dd"
    private String time; // "HH:mm:ss"
    private String remark; // General remark for the transaction
    private double balanceAfterTransaction;
    private long timestamp; // For sorting

    // --- NEW Fields for Booking-Related Details ---
    private String refBookingId; // Transaction ID of the original Booking
    private String refBookingRoute;
    private String refBookingStartStop;
    private String refBookingDestinationStop;
    // --- END NEW Fields ---

    // Default constructor for Firebase
    public PassbookEntry() {}

    // Existing constructor - remains useful for non-booking entries or basic entries
    public PassbookEntry(String transactionId, String type, double amount, String date, String time, String remark) {
        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.time = time;
        this.remark = remark; // General remark
        this.timestamp = parseTimestamp(date, time);
        // Booking details would be null by default with this constructor, can be set via setters
    }

    // Optional: A new constructor could be added for convenience if creating many booking-related entries
    public PassbookEntry(String transactionId, String type, double amount, String date, String time, String remark,
                         String refBookingId, String refBookingRoute, String refBookingStartStop, String refBookingDestinationStop) {
        this(transactionId, type, amount, date, time, remark); // Calls the existing constructor
        this.refBookingId = refBookingId;
        this.refBookingRoute = refBookingRoute;
        this.refBookingStartStop = refBookingStartStop;
        this.refBookingDestinationStop = refBookingDestinationStop;
    }


    // Getters and Setters for existing fields
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public double getBalanceAfterTransaction() { return balanceAfterTransaction; }
    public void setBalanceAfterTransaction(double balanceAfterTransaction) { this.balanceAfterTransaction = balanceAfterTransaction; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // --- Getters and Setters for NEW Booking-Related Fields ---
    public String getRefBookingId() { return refBookingId; }
    public void setRefBookingId(String refBookingId) { this.refBookingId = refBookingId; }

    public String getRefBookingRoute() { return refBookingRoute; }
    public void setRefBookingRoute(String refBookingRoute) { this.refBookingRoute = refBookingRoute; }

    public String getRefBookingStartStop() { return refBookingStartStop; }
    public void setRefBookingStartStop(String refBookingStartStop) { this.refBookingStartStop = refBookingStartStop; }

    public String getRefBookingDestinationStop() { return refBookingDestinationStop; }
    public void setRefBookingDestinationStop(String refBookingDestinationStop) { this.refBookingDestinationStop = refBookingDestinationStop; }
    // --- END Getters and Setters for NEW Fields ---

    private long parseTimestamp(String dateStr, String timeStr) {
        if (dateStr == null || timeStr == null) return 0;
        try {
            // Ensure your date and time strings strictly follow this format when parsing
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date parsedDate = dateTimeFormat.parse(dateStr + " " + timeStr);
            return parsedDate != null ? parsedDate.getTime() : System.currentTimeMillis(); // Fallback to current time if parsing somehow yields null
        } catch (ParseException e) {
            // Log the error for debugging
            // android.util.Log.e("PassbookEntry", "Error parsing timestamp for date: " + dateStr + ", time: " + timeStr, e);
            // Fallback strategy: try to parse date only, or return current time as a last resort
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date parsedDate = dateFormat.parse(dateStr);
                if (parsedDate != null) {
                    // If only date is parsable, maybe append 00:00:00 or use start of day
                    return parsedDate.getTime();
                }
            } catch (ParseException ex) {
                // android.util.Log.e("PassbookEntry", "Error parsing date only: " + dateStr, ex);
            }
            return System.currentTimeMillis(); // Fallback to current time if all parsing fails
        }
    }

    // Comparator for sorting entries by timestamp (descending - newest first)
    public static class EntryComparator implements Comparator<PassbookEntry> {
        @Override
        public int compare(PassbookEntry o1, PassbookEntry o2) {
            return Long.compare(o2.getTimestamp(), o1.getTimestamp()); // Newest first
        }
    }
}