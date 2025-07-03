package com.pritish.smartbuss;

public class Passenger {
    private String price;
    private String route;
    private String transactionID;

    // Empty constructor for Firebase
    public Passenger() {
    }

    // Constructor with fields
    public Passenger(String price, String route, String transactionID) {
        this.price = price;
        this.route = route;
        this.transactionID = transactionID;
    }

    // Getter and Setter methods
    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getTransactionId() {
        return transactionID;
    }

    public void setTransactionId(String transactionID) {
        this.transactionID = transactionID;
    }
}
