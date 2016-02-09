package com.mol.muleteer.zzz_not_used.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.mol.muleteer.entity_description.DriverData;

import java.sql.SQLException;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

   public static final String DATABASE_NAME = "driver.db";
   public static final int DATABASE_VERSION = 1;

   private DriverDao driverDao = null;

   public DatabaseHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
   }

   @Override
   public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
      Log.d("onCreate", "started");
      try {
         TableUtils.createTableIfNotExists(connectionSource, DriverData.class);

         Log.i(DatabaseHelper.class.getSimpleName(), "onCreated worked!");
      } catch (SQLException e) {
         Log.e(DatabaseHelper.class.getSimpleName(), "onCreated failed!!!", e);

         e.printStackTrace();
      }
   }

   @Override
   public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource,
                         int oldVersion, int newVersion) {
      Log.d("onUpgrade", "started");
      try {
         TableUtils.dropTable(connectionSource, DriverData.class, true);

         Log.i(DatabaseHelper.class.getSimpleName(), "onUpgrade worked!");
      } catch (SQLException e) {
         Log.e(DatabaseHelper.class.getSimpleName(), "onUpgrade failed!!!", e);

         e.printStackTrace();
      }
   }

   public DriverDao getDriverDao() {
      if (driverDao == null) {
         try {
            driverDao = new DriverDao(getConnectionSource(), DriverData.class);
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
      return driverDao;
   }
}