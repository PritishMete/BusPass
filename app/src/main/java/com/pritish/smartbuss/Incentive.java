package com.pritish.smartbuss;

import java.io.Serializable;

// Incentive.java
public class Incentive implements Serializable {
    private final String time;
    private final String route;
    private final double amount;

    public Incentive(String time, String route, double amount) {
        this.time = time;
        this.route = route;
        this.amount = amount;
    }

    // Getters
    public String getTime() { return time; }
    public String getRoute() { return route; }
    public double getAmount() { return amount; }
}