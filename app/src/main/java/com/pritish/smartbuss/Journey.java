package com.pritish.smartbuss;

public class Journey {
    private String routeNumber;
    private String status;
    private String startLocation;
    private String endLocation;
    private String time;
    private String price;

    public Journey(String routeNumber, String status, String startLocation,
                   String endLocation, String time, String price) {
        this.routeNumber = routeNumber;
        this.status = status;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.time = time;
        this.price = price;
    }

    public String getRouteNumber() { return routeNumber; }
    public String getStatus() { return status; }
    public String getStartLocation() { return startLocation; }
    public String getEndLocation() { return endLocation; }
    public String getTime() { return time; }
    public String getPrice() { return price; }
}