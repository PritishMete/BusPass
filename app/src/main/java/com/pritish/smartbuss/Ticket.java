package com.pritish.smartbuss;

public class Ticket {
    private String ticketId;
    private String userPhone;
    private String startLocation;
    private String endLocation;
    private String routeName;
    private double price;
    private long timestamp;

    public Ticket() {
        // Default constructor required for calls to DataSnapshot.getValue(Ticket.class)
    }

    public Ticket(String ticketId, String userPhone, String startLocation, String endLocation, String routeName, double price, long timestamp) {
        this.ticketId = ticketId;
        this.userPhone = userPhone;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.routeName = routeName;
        this.price = price;
        this.timestamp = timestamp;
    }

    // Getters and setters...
}

