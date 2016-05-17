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
   private long timeInMs;
   @SerializedName("distance")
   private int distanceInM;
   // just for refinement of distance calculations \
   private float speed;

   public LocationPoint(double latitude, double longitude,
                        long timeInMs, int distanceInM, float speed) {
      this.latitude = latitude;
      this.longitude = longitude;
      this.timeInMs = timeInMs;
      this.distanceInM = distanceInM;
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

   public long getTimeInMs() {
      return timeInMs;
   }

   public void setTimeInMs(long timeInMs) {
      this.timeInMs = timeInMs;
   }

   public int getDistanceInM() {
      return distanceInM;
   }

   public void setDistanceInM(int distanceInM) {
      this.distanceInM = distanceInM;
   }

   public float getSpeed() {
      return speed;
   }

   public void setSpeed(float speed) {
      this.speed = speed;
   }
}