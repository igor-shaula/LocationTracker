package com.mol.drivergps.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.mol.drivergps.entity_description.DriverData;

import java.sql.SQLException;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

   public static final String TAG = DatabaseHelper.class.getSimpleName();
   public static final String DATABASE_NAME = "driver.db";
   public static final int DATABASE_VERSION = 2;

   private static final String CREATED_OK = "onCreated worked!";
   private static final String NOT_CREATED = "onCreated failed!!!";
   private static final String UPGRADED_OK = "onUpgrade worked!";
   private static final String NOT_UPGRADED = "onUpgrade failed!!!";

   private DriverDao driverDao = null;

   public DatabaseHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
   }

   @Override
   public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
      Log.d("onCreate", "started");
      try {
         TableUtils.createTableIfNotExists(connectionSource, DriverData.class);

         Log.i(DatabaseHelper.class.getName(), CREATED_OK);
      } catch (SQLException e) {
         Log.e(DatabaseHelper.class.getName(), NOT_CREATED, e);

         e.printStackTrace();
      }
   }

   @Override
   public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource,
                         int oldVersion, int newVersion) {
      try {
         TableUtils.dropTable(connectionSource, DriverData.class, true);

         Log.i(DatabaseHelper.class.getName(), CREATED_OK);
      } catch (SQLException e) {
         Log.e(DatabaseHelper.class.getName(), NOT_CREATED, e);

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