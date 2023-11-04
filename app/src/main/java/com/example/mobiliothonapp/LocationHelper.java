package com.example.mobiliothonapp;

public class LocationHelper {
    String username;
    private double longitude, latitude;
    private float currentSpeed;
    private boolean car = false;
    private boolean emergency = false;

    public LocationHelper() {
    }

    public boolean isCar() {
        return car;
    }

    public void setCar(boolean car) {
        this.car = car;
    }

    public LocationHelper(String username, double longitude, double latitude, float currentSpeed, boolean car, boolean emergency) {
        this.username = username;
        this.longitude = longitude;
        this.latitude = latitude;
        this.currentSpeed = currentSpeed;
        this.car = car;
        this.emergency = emergency;
    }

    public boolean isEmergency() {
        return emergency;
    }

    public void setEmergency(boolean emergency) {
        this.emergency = emergency;
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
