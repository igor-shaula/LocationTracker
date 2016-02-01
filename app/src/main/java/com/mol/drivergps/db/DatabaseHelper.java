package com.mol.drivergps.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.mol.drivergps.dao.DriverDao;
import com.mol.drivergps.entity_description.Driver;

import java.sql.SQLException;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    public static final String TAG = DatabaseHelper.class.getSimpleName();
    public static final String DATABASE_NAME = "driver.db";
    public static final int DATABASE_VERSION = 1;
    private DriverDao driverDao = null;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // это метод запускается только раз, если БД еще нет
    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            TableUtils.createTableIfNotExists(connectionSource, Driver.class);

        } catch (SQLException e) {
            Log.e(TAG, "error creating database " + DATABASE_NAME);
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource,
                          int oldVersion, int newVersion) {
        try {
            TableUtils.dropTable(connectionSource, Driver.class, true);
            onCreate(database, connectionSource);
        } catch (SQLException e) {
            Log.e(TAG, "error update database version " + DATABASE_VERSION + ",DB name " + DATABASE_NAME);
            e.printStackTrace();
        }
    }

    public DriverDao getDriverDao(){
        if (driverDao == null) {
            try {
                driverDao = new DriverDao(getConnectionSource(), Driver.class);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return driverDao;
    }
}