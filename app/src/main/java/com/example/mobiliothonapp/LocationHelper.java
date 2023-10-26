package com.example.mobiliothonapp;

public class LocationHelper {
    String username;
    private double longitude, latitude;
    private float currentSpeed;

    public LocationHelper() {
    }

    public LocationHelper(String username, double longitude, double latitude, float currentSpeed) {
        this.username = username;
        this.longitude = longitude;
        this.latitude = latitude;
        this.currentSpeed = currentSpeed;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public float getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(float currentSpeed) {
        this.currentSpeed = currentSpeed;
    }
}
