package com.mol.drivergps.entity_description;

import com.google.gson.annotations.SerializedName;

//@DatabaseTable(tableName = "driverData")
public class DriverData {

//   @DatabaseField(id = true)
//   private int id;

   //   @DatabaseField
//   @SerializedName("qr-code")
//   private String qr;

   //   @DatabaseField
   @SerializedName("lat")
   private double latitude;
   //   @DatabaseField
   @SerializedName("lng")
   private double longitude;
   //   @DatabaseField
   @SerializedName("date")
   private long nanoTime;
/*
   public DriverData() {
      // necessary for OrmLite \
   }
*/

   //   public DriverData(String qr, double latitude, double longitude, long nanoTime) {
   public DriverData(double latitude, double longitude, long nanoTime) {
//      this.qr = qr;
      this.latitude = latitude;
      this.longitude = longitude;
      this.nanoTime = nanoTime;
   }
/*
   public DriverData getDriverData() {
      return new DriverData(latitude, longitude, nanoTime);
//      return new DriverData(qr, latitude, longitude, nanoTime);
   }
*/
/*
   public int getId() {
      return id;
   }
*/

   @Override
   public String toString() {
      return "DriverData{" +
//               "qr='" + qr + '\'' +
               ", latitude=" + latitude +
               ", longitude=" + longitude +
               ", nanoTime=" + nanoTime +
               '}';
   }
}