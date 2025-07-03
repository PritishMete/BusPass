//package com.pritish.smartbuss;
//
//import java.io.Serializable; // Add this import if you want to pass it through Bundles
//
//public class TicketItem implements Serializable { // Implement Serializable for passing through Bundles
//    private String from;
//    private String to;
//    private String time;
//    private String personCount;
//    private String fare;
//    private String transactionId;
//    private String busNumber;
//    private String scannedAt;
//
//    public TicketItem() {
//        // Default constructor needed for Firebase deserialization
//    }
//
//    public String getFrom() { return from; }
//    public void setFrom(String from) { this.from = from; }
//
//    public String getTo() { return to; }
//    public void setTo(String to) { this.to = to; }
//
//    public String getTime() { return time; }
//    public void setTime(String time) { this.time = time; }
//
//    public String getPersonCount() { return personCount; }
//    public void setPersonCount(String personCount) { this.personCount = personCount; }
//
//    public String getFare() { return fare; }
//    public void setFare(String fare) { this.fare = fare; }
//
//    public String getTransactionId() { return transactionId; }
//    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
//
//    public String getBusNumber() { return busNumber; }
//    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
//
//    public String getScannedAt() { return scannedAt; }
//    public void setScannedAt(String scannedAt) { this.scannedAt = scannedAt; }
//}
