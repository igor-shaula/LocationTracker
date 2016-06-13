package com.igor_shaula.location_tracker.storage.realm;

import io.realm.RealmObject;

/**
 * Created by igor shaula - to hold data for GSON and work with Realm \
 */
public class RealmLocationPoint extends RealmObject {

    private double latitude;
    private double longitude;
    private long time;
    // just for refinement of distance calculations \
    private float speed;

    public RealmLocationPoint() {
        // just to preserve it for Realm \
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