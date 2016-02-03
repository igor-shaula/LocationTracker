package com.mol.drivergps.entity_description;

import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "driverData")
public class DriverData {

   @DatabaseField(id = true)
   private int id = 1;

   @SerializedName("qr-code")
   @DatabaseField
   private String qr;

   @SerializedName("coordinates")
   @DatabaseField
   private String coordinates;

   @SerializedName("time")
   @DatabaseField
   private String time;

   public DriverData() {
      // necessary for OrmLite \
   }

   public DriverData(String qr, String coordinates, String time) {
      this.qr = qr;
      this.coordinates = coordinates;
      this.time = time;
   }

   public DriverData getDriverData() {
      return new DriverData(qr, coordinates, time);
   }

   public int getId() {
      return id;
   }

   public String getQr() {
      return qr;
   }

   public void setQr(String qr) {
      this.qr = qr;
   }

   public String getCoordinates() {
      return coordinates;
   }

   public void setCoordinates(String coordinates) {
      this.coordinates = coordinates;
   }

   public String getTime() {
      return time;
   }

   public void setTime(String time) {
      this.time = time;
   }

   @Override
   public String toString() {
      return "DriverData{" +
               "qr='" + qr + '\'' +
               ", coordinates='" + coordinates + '\'' +
               ", time='" + time + '\'' +
               '}';
   }
}