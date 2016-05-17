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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.solveast.geo_tracker.GlobalKeys;
import com.solveast.geo_tracker.MyLog;
import com.solveast.geo_tracker.R;
import com.solveast.geo_tracker.entity.LocationPoint;

import java.io.IOException;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainService extends Service {

   private final static long MIN_PERIOD_MILLISECONDS = 10 * 1000;
   private final static float MIN_DISTANCE_IN_METERS = 10;

   private PendingIntent mPendingIntent;
   private String mQrFromActivity;
   private Realm mRealm; // represents instance of the database \

   private double mLatitude, mLongitude;
   private long mTime;
   private int mDistance;
   private float mSpeed;

   private ConnectivityManager mConnectivityManager;
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

      mQrFromActivity = intent.getStringExtra(GlobalKeys.QR_KEY);
      // when service is restarted after reboot - intent is empty \
      if (mQrFromActivity == null)
         // just restoring the data from shared preferances \
         mQrFromActivity = getApplicationContext().
                  getSharedPreferences(GlobalKeys.S_P_NAME, MODE_PRIVATE).
                  getString(GlobalKeys.S_P_QR_KEY, "");
      // we assume that QR-code contains valid web URL inside \
      MyLog.v("getStringExtra: " + mQrFromActivity);

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
            MyLog.v("onProviderDisabled = happened");
            Toast.makeText(MainService.this, getString(R.string.toastGpsProviderDisabled), Toast.LENGTH_SHORT).show();
            // TODO: 13.05.2016 make reaction to this event \
         }

         @Override
         public void onProviderEnabled(String provider) {
            MyLog.v("onProviderEnabled = started");
            Toast.makeText(MainService.this, getString(R.string.toastGpsProviderEnabled), Toast.LENGTH_SHORT).show();
            // TODO: 13.05.2016 make reaction to this event \
         }

         @Override
         public void onStatusChanged(String provider, int status, Bundle extras) {
            MyLog.v("onStatusChanged = started");
//            Toast.makeText(MainService.this, "onStatusChanged", Toast.LENGTH_SHORT).show();
            // TODO: 13.05.2016 investigate status and extras from here \
            // TODO: 13.05.2016 make data processing in special method \
         }

         @Override
         public void onLocationChanged(Location newLocation) {
            MyLog.v("onLocationChanged = started");
            // the only place where all interesting operations are done \
            processLocationUpdate(newLocation);
         }
      }; // end of LocationListener-description \\

      // this check is required by IDE - i decided to check both permissions at once \
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
               != PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
         // we can only inform user about absent permissions \
         MyLog.v("permissions are not set well");
         Toast.makeText(MainService.this, getString(R.string.toastPermissionsAreNotGiven), Toast.LENGTH_SHORT).show();

      } else {
         // if all permissions are given - time to launch listening to location updates \
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
      }

      mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
      // just for testing purpose and to check the URL at the service start \
      if (checkInternet()) sendInfoToServer();
   } // end of gpsTrackingStart-method \\

// ACTIONS FROM LISTENER ===========================================================================

   // this method is called only from inside location listener
   private void processLocationUpdate(Location location) {
      // only one (current) location point - for only one line of network requests \

      MyLog.i(location.getProvider() + " - location accuracy = " + location.getAccuracy());

      // this check is required by IDE \
      if (ActivityCompat.checkSelfPermission(getApplicationContext(),
               Manifest.permission.ACCESS_FINE_LOCATION)
               != PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
         return;
      }

      // extracting needed fields - we cannot take the whole object because of Realm restrictions \
      mLatitude = location.getLatitude();
      mLongitude = location.getLongitude();
      mTime = location.getTime();
      if (location.hasSpeed()) mSpeed = location.getSpeed();
      else mSpeed = 0; // explicitly clearing value from previous possible point \

      // the only place of saving current point into database \
      saveToDB(mLatitude, mLongitude, mTime, mSpeed);

      RealmResults<LocationPoint> locationPointList = mRealm.where(LocationPoint.class).findAll();
      mDistance = (int) getTotalDistance(locationPointList);

      // first we have to check internet availability and inform activity about it \
      if (checkInternet()) sendInfoToServer(); // this is the last action for service job \
      // creation and two puts are made with one line here \

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
      locationPoint.setTimeInMs(currentTime);
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
            MyLog.i("hasNext & i = " + i);

            startPoint = locationPointList.get(i);
            startLat = startPoint.getLatitude();
            startLong = startPoint.getLongitude();
            endPoint = locationPointList.iterator().next();
            endLat = endPoint.getLatitude();
            endLong = endPoint.getLongitude();

            // we have to measure distance only between real points - not zeroes \
            if (startLat != 0.0 && startLong != 0.0 && endLat != 0.0 && endLong != 0.0) {

               MyLog.i("startPoint speed = " + startPoint.getSpeed());

               // before calculating distance checking if it could be real \
               if (startPoint.getSpeed() > 1) { // meters per second

                  // result of calculations is stored inside the resultArray \
                  Location.distanceBetween(startLat, startLong, endLat, endLong, resultArray);

                  MyLog.i("calculations done: resultArray[0] = " + resultArray[0]);

                  // quick decision to cut off location noise and count only car movement \
                  if (resultArray[0] > MIN_DISTANCE_IN_METERS)
                     totalDistanceInMeters += resultArray[0];

               } // end of check-speed-condition \\
            } // end of check-four-non-zero-condition \\
         } // end of hasNext-condition \\
      } // end of for-loop \\
      MyLog.i("totalDistanceInMeters = " + totalDistanceInMeters);

      return totalDistanceInMeters;
   } // end of getTotalDistance-method \\

   // it works before every connection attempt \
   private boolean checkInternet() {

      NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
      try {
         if (networkInfo != null) {
            if (networkInfo.isConnected()) {
               mPendingIntent.send(GlobalKeys.P_I_CONNECTION_ON);
               return true;
            } else {
               mPendingIntent.send(GlobalKeys.P_I_CONNECTION_OFF);
               return false;
            }
         } else {
            mPendingIntent.send(GlobalKeys.P_I_CONNECTION_OFF);
            return false;
         }
      } catch (PendingIntent.CanceledException e) {
         e.printStackTrace();
         return false;
      }
   }

   // my OkHTTP usage to send tracking data to the server - Retrofit didn't work \
   private void sendInfoToServer() {
      MyLog.v("sendInfoToServer = started");

      // at first creating object to send to the server \
      LocationPoint locationPoint = new LocationPoint(mLatitude, mLongitude, mTime, mDistance, mSpeed);
      // currently speed will not be transmitted to the server \

      Gson gson = new GsonBuilder().create();

      String jsonToSend = gson.toJson(locationPoint, LocationPoint.class);
      MyLog.v("jsonToSend: " + jsonToSend);

      MediaType JSON = MediaType.parse("application/json; charset=utf-8");

      RequestBody body = RequestBody.create(JSON, jsonToSend);

      try {
         Request request = new Request.Builder()
                  .url(mQrFromActivity)
//               .cacheControl(new CacheControl.Builder().noCache().build()) // no effect \
                  .post(body)
                  .build();

         OkHttpClient okHttpClient = new OkHttpClient();

         okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
               MyLog.d("onFailure: " + call.request().method());
               MyLog.d("onFailure: " + call.request().body().contentType().toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
               MyLog.d("onResponse: " + response.message());
               if (response.isSuccessful()) {
                  MyLog.d("onResponse: successfull - OkHTTP");
//                  MyLog.d("onResponse: " + response.body().contentType().toString());
                  MyLog.d("onResponse: " + response.body().string());
               } else {
                  MyLog.d("onResponse: " + call.request().body().contentType().toString());
                  MyLog.d("sFromResponse: " + response.message());
               }
            }
         });
      } catch (IllegalArgumentException iae) {
         iae.printStackTrace();
         // we must inform activity about wrong qr-code \
         Intent intent = new Intent().putExtra(GlobalKeys.QR_KEY_INVALID, mQrFromActivity);
         sendIntentToActivity(intent, GlobalKeys.P_I_CODE_QR_KEY_INVALID);
      }
   } // end of sendInfoToServer-method \\
}