/*
package com.solveast.gps_tracker.zzz_not_used.database;

import android.util.Log;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.solveast.gps_tracker.entity_description.DriverData;

import java.sql.SQLException;

public class DriverDao extends BaseDaoImpl<DriverData, Integer> {

   public DriverDao(ConnectionSource connectionSource, Class<DriverData> dataClass) throws SQLException {
      super(connectionSource, dataClass);
   }

   public void addNewDriverData(DriverData driverData) {
      Log.d("addNewDriverData", driverData.toString());
      try {
         createOrUpdate(driverData);
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }

   public DriverData getDriverData() {
      Log.d("getDriverData", "started");
      try {
         return this.queryForId(1);
      } catch (SQLException e) {
         e.printStackTrace();
      }
      return null;
   }
}*/
