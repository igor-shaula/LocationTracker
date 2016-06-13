package com.igor_shaula.location_tracker.entity;

/**
 * Created by igor shaula
 * <p>
 * this is general description for location data entity \
 */
public class LocationPoint {

    private double latitude;
    private double longitude;
    private long time;
    // just for refinement of distance calculations \
    private float speed;

    public LocationPoint(double latitude, double longitude,
                         long time, float speed) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
        this.speed = speed;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }
}