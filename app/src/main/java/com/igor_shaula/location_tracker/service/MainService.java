package com.igor_shaula.location_tracker.service;

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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.igor_shaula.location_tracker.R;
import com.igor_shaula.location_tracker.entity.LocationPoint;
import com.igor_shaula.location_tracker.storage.StorageActions;
import com.igor_shaula.location_tracker.storage.realm.MyRealm;
import com.igor_shaula.location_tracker.utilities.GlobalKeys;
import com.igor_shaula.location_tracker.utilities.MyLog;

import java.lang.ref.WeakReference;
import java.util.List;

public class MainService extends Service {

    private final static long MIN_PERIOD_MILLISECONDS = 10 * 1000;
    private final static float MIN_DISTANCE_IN_METERS = 1;
    private static final String STORAGE_THREAD = "my storage thread";
    private static final int STORAGE_INIT_CLEAR = 0;
    private static final int STORAGE_SAVE_NEW = 1;
    private static final int STORAGE_READ_ALL = 2;

    private PendingIntent mPendingIntent;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    private StorageActions mStorageAgent;
    private Handler mHandler;
    private Thread mStorageThread;
    private int runnableState;

    // service ought not keep data in self - so these variables are crutches for multithreading usage \
    private double mLatitude, mLongitude;
    private long mTime;
    private float mSpeed, mAccuracy;
    private List<LocationPoint> mDataFromStorage;

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

        // it will be used to send messages from inside worker threads and catch them inside UI thread \
        mHandler = new MyHandler(this);

        // all even potentially hard work is kept in other threads \
        mStorageThread = new Thread(rStorageTask, STORAGE_THREAD);
        mStorageThread.setDaemon(true);
        mStorageThread.start();

        // finally launching main sequence to get the location data \
        gpsTrackingStart();

        return Service.START_REDELIVER_INTENT;

        // TODO: 14.06.2016 use WakeLock to prevent processor from sleeping \
    } // end of onStartCommand-method \\

    @Override
    public void onDestroy() {
        super.onDestroy();
        // time to clean all resources \
        mLocationManager.removeUpdates(mPendingIntent);
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationManager.removeUpdates(mLocationListener);
        // which way is better - remove every or all at once \
        mHandler.removeCallbacks(rStorageTask);
        if (mHandler != null)
            mHandler.removeCallbacksAndMessages(null);
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
                Toast.makeText(MainService.this, getString(R.string.toastGpsProviderOff), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderEnabled(String provider) {
                MyLog.i("onProviderEnabled: " + provider);
                Toast.makeText(MainService.this, getString(R.string.toastGpsProviderOn), Toast.LENGTH_SHORT).show();
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
                && ActivityCompat.checkSelfPermission(this,
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

    // this method is called only from inside location listener - works in main thread \
    private void processLocationUpdate(Location location) {
        // only one (current) location point - for only one line of network requests \

        MyLog.i("provider = " + location.getProvider() + " - location accuracy = " + location.getAccuracy());

        // extracting needed fields - we cannot take the whole object because of Realm restrictions \
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        mTime = location.getTime();
        if (location.hasSpeed()) mSpeed = location.getSpeed();
        else mSpeed = 0; // explicitly clearing value from previous possible point \
        if (location.hasAccuracy()) mAccuracy = location.getAccuracy();
        else mAccuracy = 0; // explicitly clearing value from previous possible point \

        // the only place of saving current point into database \
        runnableState = 1;
        mStorageThread.start(); // java.lang.IllegalThreadStateException: Thread already started

        final int[] distance = new int[1];
        // my way to launch one action after another accounting worker threads completion \
        new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    // i have to launch reading data thread only when writing new point is done \
                    case STORAGE_SAVE_NEW:
                        MyLog.i("handleMessage: saving thread finished - can begin reading");
                        // now it is possible to safely read all data from the storage \
                        runnableState = 2;
                        mStorageThread.start();
                        return true;
                    // begin calculations only when reading thread is completed \
                    case STORAGE_READ_ALL:
                        // the next step has to be busy with saving new point of data \
                        runnableState = 1;
                        MyLog.i("handleMessage: reading thread finished - can begin calculations");
                        distance[0] = (int) getTotalDistance(mDataFromStorage);
                        // preparing and sending data to MainActivity to update its UI \
                        Intent intentToReturn = new Intent()
                                .putExtra(GlobalKeys.GPS_LATITUDE, mLatitude)
                                .putExtra(GlobalKeys.GPS_LONGITUDE, mLongitude)
                                .putExtra(GlobalKeys.GPS_TAKING_TIME, mTime)
                                .putExtra(GlobalKeys.DISTANCE, distance[0]);
                        sendIntentToActivity(intentToReturn, GlobalKeys.P_I_CODE_DATA_FROM_GPS); // 100

                        return true;
                } // end of switch-statement \\
                return false;
            } // end of handleMessage-method \\
        }); // end of Handler instance definition \\
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

    // returns believable value of total passed distance \
    private float getTotalDistance(List<LocationPoint> locationPointList) {
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

                MyLog.i(i + " calculations: startPoint.millis = " + startPoint.getTime());
                MyLog.i(i + " calculations: endPoint.millis _ = " + endPoint.getTime());
                // somehow result was <0 otherwise - if end minus start \
                long deltaTime = (endPoint.getTime() - startPoint.getTime()) / 1000;
//                     long deltaTime = endPoint.getTime() - startPoint.getTime();
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

// MULTITHREADING ==================================================================================

    private Runnable rStorageTask = new Runnable() {
        @Override
        public void run() {
            // avoiding potential concurrency for resources with main thread \
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            switch (runnableState) {
                // thread works this way by default but only once from the start of the service \
                case STORAGE_INIT_CLEAR:
                    // create instance of abstract storage - choose realization only here \
                    mStorageAgent = MyRealm.getSingleton(MainService.this);
//                    mStorageAgent = InMemory.getSingleton();
                    mStorageAgent.clearAll();
                    mHandler.sendEmptyMessage(STORAGE_INIT_CLEAR);
                    break;
                // saving new point \
                case STORAGE_SAVE_NEW:
                    // taking arguments in such way looks like a crutch - but it's needed \
                    mStorageAgent.write(new LocationPoint(mLatitude, mLongitude, mTime, mSpeed, mAccuracy));
                    mHandler.sendEmptyMessage(STORAGE_SAVE_NEW);
                    break;
                // reading all \
                case STORAGE_READ_ALL:
                    mDataFromStorage = mStorageAgent.readAll();
                    mHandler.sendEmptyMessage(STORAGE_READ_ALL);
                    break;
            } // end of switch-statement \\
        } // end of run-method \\
    };

    // created to avoid memory leaks if class not static when using default Handler-class \
    private static class MyHandler extends Handler {

        WeakReference<MainService> weakReference;

        private MyHandler(MainService mainService) {
            weakReference = new WeakReference<>(mainService);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            MainService mainService = weakReference.get();
            if (mainService == null) {
                MyLog.e("handleMessage: mainService == null");
                return;
            }

            String whatMeaning = "";
            switch (msg.what) {
                case STORAGE_INIT_CLEAR:
                    whatMeaning = "storage is prepared and cleaned";
                    break;
                case STORAGE_SAVE_NEW:
                    whatMeaning = "new data is written to the storage";
                    break;
                case STORAGE_READ_ALL:
                    whatMeaning = "all data is read from the storage";
                    break;
            }
            // what is need to be updated in UI thread - is here \
            Toast.makeText(mainService, "handleMessage: " + whatMeaning, Toast.LENGTH_SHORT).show();
            MyLog.i("handleMessage: " + whatMeaning);
        }
    } // end of MyHandler-inner-class \\
}