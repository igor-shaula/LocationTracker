package com.mol.muleteer.entity_description;

import com.google.gson.annotations.SerializedName;

public class DriverData {

   @SerializedName("lat")
   private double latitude;
   @SerializedName("lng")
   private double longitude;
   @SerializedName("date")
   private long nanoTime;

   public DriverData(double latitude, double longitude, long nanoTime) {
      this.latitude = latitude;
      this.longitude = longitude;
      this.nanoTime = nanoTime;
   }

   @Override
   public String toString() {
      return "DriverData{" +
               "latitude=" + latitude +
               ", longitude=" + longitude +
               ", nanoTime=" + nanoTime +
               '}';
   }
}