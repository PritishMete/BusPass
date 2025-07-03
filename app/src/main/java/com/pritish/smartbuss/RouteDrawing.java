package com.pritish.smartbuss;
// RouteDrawing.java

import android.content.Context;
import com.google.android.gms.maps.model.LatLng;

public class RouteDrawing {
    private Context context;
    private LatLng origin;
    private LatLng destination;

    // Private constructor accepting a Builder
    private RouteDrawing(Builder builder) {
        this.context = builder.context;
        this.origin = builder.origin;
        this.destination = builder.destination;
    }

    // The Builder class for creating RouteDrawing objects
    public static class Builder {
        private Context context;
        private LatLng origin;
        private LatLng destination;

        // Constructor for Builder, requires context
        public Builder(Context context) {
            this.context = context;
        }

        // Method to set the origin
        public Builder setOrigin(LatLng origin) {
            this.origin = origin;
            return this;
        }

        // Method to set the destination
        public Builder setDestination(LatLng destination) {
            this.destination = destination;
            return this;
        }

        // Method to build the RouteDrawing instance
        public RouteDrawing build() {
            return new RouteDrawing(this);
        }
    }

    // Example of an execute method that can perform the route drawing task
    public void execute() {
        // Add your code here to draw the route on the map using origin and destination
        // This is just a placeholder function to demonstrate the idea
        System.out.println("Drawing route from " + origin + " to " + destination);
    }
}

