package com.solveast.geo_tracker.service;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.solveast.geo_tracker.GlobalKeys;
import com.solveast.geo_tracker.MyLog;
import com.solveast.geo_tracker.R;
import com.solveast.geo_tracker.entity.LocationPoint;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

public class MainService extends Service {

    private final static long MIN_PERIOD_MILLISECONDS = 10 * 1000;
    private final static float MIN_DISTANCE_IN_METERS = 10;

    private PendingIntent mPendingIntent;
    private Realm mRealm; // represents instance of the database \

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

// LIFECYCLE =======================================================================================

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MyLog.v("onStartCommand = MainService started");

        // now starting service as from zero \
        mPendingIntent = intent.getParcelableExtra(GlobalKeys.P_I_KEY);
        // when service is restarted after reboot - intent is empty \
        if (mPendingIntent == null)
            mPendingIntent = PendingIntent.getActivity(getApplicationContext(),
                    GlobalKeys.REQUEST_CODE_MAIN_SERVICE, new Intent(), 0);

        // Create the Realm configuration
        RealmConfiguration realmConfig = new RealmConfiguration
                .Builder(this)
                .deleteRealmIfMigrationNeeded()
                .build();
        // Open the Realm for the UI thread.
        mRealm = Realm.getInstance(realmConfig);

        // initially clearing the database to get proper distance \
        mRealm.beginTransaction();
        mRealm.delete(LocationPoint.class);
//      mRealm.where(LocationPoint.class).findAll().deleteAllFromRealm();
        mRealm.commitTransaction();

        // main job for the service \
        gpsTrackingStart();

        return Service.START_REDELIVER_INTENT;
    } // end of onStartCommand-method \\

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocationManager.removeUpdates(mPendingIntent);
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.removeUpdates(mLocationListener);
    }

    // PREPARING MECHANISM =============================================================================

    // launched from onStartCommand \
    private void gpsTrackingStart() {

        // this is the global data source of all location information \
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // for now all actions are launched from inside this listener \
        mLocationListener = new LocationListener() {

            @Override
            public void onProviderDisabled(String provider) {
                MyLog.i("onProviderDisabled: " + provider);
                Toast.makeText(MainService.this, getString(R.string.toastGpsProviderDisabled), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderEnabled(String provider) {
                MyLog.i("onProviderEnabled: " + provider);
                Toast.makeText(MainService.this, getString(R.string.toastGpsProviderEnabled), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                MyLog.i("onStatusChanged: provider: " + provider + " & status = " + status);
                MyLog.i("onStatusChanged: extras: " + extras.getString("satellites"));
            }

            @Override
            public void onLocationChanged(Location newLocation) {
                MyLog.i("onLocationChanged = started");
                // the only place where all interesting operations are done \
                processLocationUpdate(newLocation);
            }
        }; // end of LocationListener-description \\

        // this check is required by IDE - i decided to check both permissions at once \
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // we can only inform user about absent permissions \
            MyLog.e("permissions are not set well");
            Toast.makeText(MainService.this, getString(R.string.toastPermissionsAreNotGiven), Toast.LENGTH_SHORT).show();

        } else {
            // if all permissions are given - time to launch listening to location updates \
/*
         // the most common way - to listen to every adapter - but this causes triple updates \
         String[] providers = {
                  LocationManager.PASSIVE_PROVIDER, // to save battery - unknown accuracy
                  LocationManager.NETWORK_PROVIDER, // cell and wifi - coarse
                  LocationManager.GPS_PROVIDER // for GPS - fine
         };
         for (String provider : providers) {
            mLocationManager.requestLocationUpdates(
                     provider,
                     MIN_PERIOD_MILLISECONDS,
                     MIN_DISTANCE_IN_METERS,
                     mLocationListener);
            LocationProvider locationProvider = mLocationManager.getProvider(provider);
            MyLog.i("accuracy of " + provider + " = " + locationProvider.getAccuracy());
         }
*/
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, // for GPS - fine precision
                    MIN_PERIOD_MILLISECONDS,
                    MIN_DISTANCE_IN_METERS,
                    mLocationListener);
            LocationProvider locationProvider = mLocationManager.getProvider(LocationManager.GPS_PROVIDER);
            MyLog.i("accuracy of GPS_PROVIDER = " + locationProvider.getAccuracy());
        }
    } // end of gpsTrackingStart-method \\

// ACTIONS FROM LISTENER ===========================================================================

    // this method is called only from inside location listener
    private void processLocationUpdate(Location location) {
        // only one (current) location point - for only one line of network requests \

        MyLog.i("provider = " + location.getProvider() + " - location accuracy = " + location.getAccuracy());

        // extracting needed fields - we cannot take the whole object because of Realm restrictions \
        double mLatitude = location.getLatitude();
        double mLongitude = location.getLongitude();
        long mTime = location.getTime();
        float mSpeed;
        if (location.hasSpeed()) mSpeed = location.getSpeed();
        else mSpeed = 0; // explicitly clearing value from previous possible point \
//      if(location.hasAccuracy()) ...

        // TODO: 20.05.2016 try to use the app without database for now - to check where's problem \

        // the only place of saving current point into database \
        saveToDB(mLatitude, mLongitude, mTime, mSpeed);

        RealmResults<LocationPoint> locationPointList = mRealm.where(LocationPoint.class).findAllSorted("timeInMs");
//      RealmResults<LocationPoint> locationPointList = mRealm.where(LocationPoint.class).findAll();
        int mDistance = (int) getTotalDistance(locationPointList);

        Intent intentToReturn = new Intent()
                .putExtra(GlobalKeys.GPS_LATITUDE, mLatitude)
                .putExtra(GlobalKeys.GPS_LONGITUDE, mLongitude)
                .putExtra(GlobalKeys.GPS_TAKING_TIME, mTime)
                .putExtra(GlobalKeys.DISTANCE, mDistance);
        sendIntentToActivity(intentToReturn, GlobalKeys.P_I_CODE_DATA_FROM_GPS); // 100
    } // end of processLocationUpdate-method \\

// UTILS ===========================================================================================

    // universal point to send info to MainActivity \
    private void sendIntentToActivity(Intent intent, int code) {
        try {
            mPendingIntent.send(this, code, intent);
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    // saving to database - realized with RealM \
    private void saveToDB(double latitude, double longitude, long currentTime, float speed) {

        // All writes must be wrapped in a transaction to facilitate safe multi threading
        mRealm.beginTransaction();

        LocationPoint locationPoint = mRealm.createObject(LocationPoint.class);
        locationPoint.setLatitude(latitude);
        locationPoint.setLongitude(longitude);
        locationPoint.setTimeInMilliSeconds(currentTime);
        locationPoint.setSpeed(speed);

        // When the transaction is committed, all changes a synced to disk.
        mRealm.commitTransaction();
    }

    // returns believable value of total passed distance \
    private float getTotalDistance(RealmResults<LocationPoint> locationPointList) {
        int capacity = locationPointList.size();
        MyLog.i("capacity = " + capacity);

        // preparing rewritable containers for the following loop \
        LocationPoint startPoint, endPoint;
        double startLat, startLong, endLat, endLong;
        float[] resultArray = new float[1];
        float totalDistanceInMeters = 0;

        // getting all data and receiving numbers at every step \
        for (int i = 0; i < capacity; i++) {
            // all works only if there are more than one point at all \
            if (locationPointList.iterator().hasNext()) {
                // this iterator is not from Collcetions framework - it's from Realm \
                MyLog.i("hasNext & i = " + i);

                // this is the simplest way to react on time negative difference \
                endPoint = locationPointList.get(i);
//            startPoint = locationPointList.get(i);
                endLat = endPoint.getLatitude();
//            startLat = startPoint.getLatitude();
                endLong = endPoint.getLongitude();
//            startLong = startPoint.getLongitude();
                startPoint = locationPointList.iterator().next();
//            endPoint = locationPointList.iterator().next();
                startLat = startPoint.getLatitude();
//            endLat = endPoint.getLatitude();
                startLong = startPoint.getLongitude();
//            endLong = endPoint.getLongitude();

                MyLog.i(i + " calculations: startPoint.millis = " + startPoint.getTimeInMilliSeconds());
                MyLog.i(i + " calculations: endPoint.millis _ = " + endPoint.getTimeInMilliSeconds());
                // somehow result was <0 otherwise - if end minus start \
                long deltaTime = (endPoint.getTimeInMilliSeconds() - startPoint.getTimeInMilliSeconds()) / 1000;
//                     long deltaTime = endPoint.getTimeInMilliSeconds() - startPoint.getTimeInMilliSeconds();
                MyLog.i(i + " calculations: seconds(end - start) = " + deltaTime);

                // we have to measure distance only between real points - not zeroes \
                if (startLat != 0.0 && startLong != 0.0 && endLat != 0.0 && endLong != 0.0) {

                    MyLog.i("startPoint speed = " + startPoint.getSpeed());

                    // before calculating distance checking if it could be real \
                    if (startPoint.getSpeed() > 1) { // meters per second

                        // result of calculations is stored inside the resultArray \
                        Location.distanceBetween(startLat, startLong, endLat, endLong, resultArray);
                        MyLog.i(i + " calculations done: resultArray[0] = " + resultArray[0]);

                        // quick decision to cut off location noise and count only car movement \
                        if (resultArray[0] > MIN_DISTANCE_IN_METERS) {
                         /*
                         * i usually receive data with much higher values than it should be \
                         * so it's obvious that i have to make those strange values some lower \
                         * first simple attempt - to use measurement of speed to correct the result \
                         */
                            // excessive check '>0' because deltaTime was negative when 'end minus start' \
                            float predictedDistance = startPoint.getSpeed() * deltaTime;
//                     float predictedDistance = deltaTime > 0 ? startPoint.getSpeed() * deltaTime : 0;
                            MyLog.i(i + " calculations: predictedDistance = " + predictedDistance);

                            float minimumOfTwo = resultArray[0];
                            if (predictedDistance < minimumOfTwo)
//                     if (predictedDistance != 0 && predictedDistance < minimumOfTwo)
                                minimumOfTwo = predictedDistance;

                            totalDistanceInMeters += minimumOfTwo;
                        }
                    } // end of check-speed-condition \\
                } // end of check-four-non-zero-condition \\
            } // end of hasNext-condition \\
        } // end of for-loop \\
        MyLog.i("totalDistanceInMeters = " + totalDistanceInMeters);

        return totalDistanceInMeters;
    } // end of getTotalDistance-method \\
}