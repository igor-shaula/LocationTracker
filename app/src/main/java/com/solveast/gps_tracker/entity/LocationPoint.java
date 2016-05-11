package com.solveast.gps_tracker.entity;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;

/**
 * Created by igor shaula - to work with Realm \
 */
public class LocationPoint extends RealmObject {

//   private int id;

   @SerializedName("lat")
   private double latitude;
   @SerializedName("long")
   private double longitude;
   @SerializedName("time")
   private long time;

   public LocationPoint(double latitude, double longitude, long milliseconds) {
      this.latitude = latitude;
      this.longitude = longitude;
      this.time = milliseconds;
   }

   public LocationPoint() {
      // just to preserve it for realm \
   }

/*
   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }
*/

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
}