package com.solveast.geo_tracker.entity;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;

/**
 * Created by igor shaula - to hold data for GSON and work with Realm \
 */
public class LocationPoint extends RealmObject {

    @SerializedName("lat")
    private double latitude;
    @SerializedName("lng")
    private double longitude;
    @SerializedName("date")
    private long timeInMilliSeconds;
    @SerializedName("distance")
    private int distanceInMeters;
    // just for refinement of distance calculations \
    private float speed;

    public LocationPoint(double latitude, double longitude,
                         long timeInMilliSeconds, int distanceInMeters, float speed) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeInMilliSeconds = timeInMilliSeconds;
        this.distanceInMeters = distanceInMeters;
        this.speed = speed;
    }

    public LocationPoint() {
        // just to preserve it for realm \
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

    public long getTimeInMilliSeconds() {
        return timeInMilliSeconds;
    }

    public void setTimeInMilliSeconds(long timeInMilliSeconds) {
        this.timeInMilliSeconds = timeInMilliSeconds;
    }

    public int getDistanceInMeters() {
        return distanceInMeters;
    }

    public void setDistanceInMeters(int distanceInMeters) {
        this.distanceInMeters = distanceInMeters;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }
}