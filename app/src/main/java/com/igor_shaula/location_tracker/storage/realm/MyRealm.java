package com.igor_shaula.location_tracker.storage.realm;

import android.content.Context;

import com.igor_shaula.location_tracker.entity.LocationPoint;
import com.igor_shaula.location_tracker.storage.StorageActions;
import com.igor_shaula.location_tracker.utilities.MyLog;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by igor shaula
 */
public class MyRealm implements StorageActions {

    private Realm mRealm;

    private static volatile MyRealm sRealmSingleton;

    // i prefer the way of Double Checked Locking & volatile \
    public static MyRealm getSingleton(Context context) {

        MyRealm localInstance = sRealmSingleton;
        // first check \
        if (localInstance == null)
            synchronized (MyRealm.class) {
                localInstance = sRealmSingleton;
                // second check \
                if (localInstance == null)
                    sRealmSingleton = localInstance = new MyRealm(context);
            }
        return localInstance;
    }

    // there is no need of Realm singleton - anyway this constructor will work only once \
    private MyRealm(Context context) {
        // Create the Realm configuration
        RealmConfiguration realmConfig = new RealmConfiguration
                .Builder(context)
                .deleteRealmIfMigrationNeeded()
                .build();
        mRealm = Realm.getInstance(realmConfig);
    }

    @Override
    public boolean write(LocationPoint locationPoint) {

        // simple check to avoid potential problems later \
        if (locationPoint == null) {
            MyLog.e("MyRealm - write: locationPoint == null !!!");
            return false;
        }

        // getting all fileds to later create object for Realm database \
        double latitude = locationPoint.getLatitude();
        double longitude = locationPoint.getLongitude();
        long time = locationPoint.getTime();
        float speed = locationPoint.getSpeed();

        // All writes must be wrapped in a transaction to facilitate safe multi threading
        mRealm.beginTransaction();

        RealmLocationPoint realmLocationPoint = mRealm.createObject(RealmLocationPoint.class);
        realmLocationPoint.setLatitude(latitude);
        realmLocationPoint.setLongitude(longitude);
        realmLocationPoint.setTime(time);
        realmLocationPoint.setSpeed(speed);

        // When the transaction is committed, all changes a synced to disk.
        mRealm.commitTransaction();

        return true;
    }

    @Override
    public List<LocationPoint> readAll() {
        RealmResults<RealmLocationPoint> realmLocationPointList = mRealm
                .where(RealmLocationPoint.class)
                .findAllSorted("timeInMs");

        // preparing all containers before the loop starts - to quicken it \
        List<LocationPoint> locationPointList = new ArrayList<>();
        LocationPoint locationPoint;
        RealmLocationPoint realmLocationPoint;
        int size = realmLocationPointList.size();

        // converting data to general format because of difference in entity objects \
        for (int i = 0; i < size; i++) {

            locationPoint = locationPointList.get(i);
            realmLocationPoint = realmLocationPointList.get(i);

            locationPoint.setLatitude(realmLocationPoint.getLatitude());
            locationPoint.setLongitude(realmLocationPoint.getLongitude());
            locationPoint.setTime(realmLocationPoint.getTime());
            locationPoint.setSpeed(realmLocationPoint.getSpeed());

            locationPointList.add(locationPoint);
        }
        return locationPointList;
    }

    @Override
    public void clearAll() {
        // initially clearing the database to get proper distance \
        mRealm.beginTransaction();
        mRealm.delete(RealmLocationPoint.class);
        mRealm.commitTransaction();
    }
}