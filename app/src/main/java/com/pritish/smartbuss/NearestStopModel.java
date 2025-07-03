package com.pritish.smartbuss;

public class NearestStopModel {
    private String stopName;
    private float distance;

    public NearestStopModel(String stopName, float distance) {
        this.stopName = stopName;
        this.distance = distance;
    }

    public String getStopName() {
        return stopName;
    }

    public float getDistance() {
        return distance;
    }
}