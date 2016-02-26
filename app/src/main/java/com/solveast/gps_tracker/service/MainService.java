package com.solveast.gps_tracker.service;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.solveast.gps_tracker.GlobalKeys;
import com.solveast.gps_tracker.MyLog;
import com.solveast.gps_tracker.entity.DriverData;

import java.io.IOException;

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

   private PendingIntent pendingIntent;
   private String qrFromActivity;

   private double latitude, longitude;
   private long timeTaken;

   private LocationManager locationManager;
   private ConnectivityManager connectivityManager;

   // definition of special object for listener \
   private LocationListener locationListener = new LocationListener() {

      @Override
      public void onProviderDisabled(String provider) {
         MyLog.v("onProviderDisabled = happened");
         reactOnLocationListener(provider, null);
      }

      @Override
      public void onProviderEnabled(String provider) {
         MyLog.v("onProviderEnabled = started");
         reactOnLocationListener(provider, null);
      }

      @Override
      public void onStatusChanged(String provider, int status, Bundle extras) {
         MyLog.v("onStatusChanged = started");
         reactOnLocationListener(provider, null);
      }

      @Override
      public void onLocationChanged(Location newLocation) {
         MyLog.v("onLocationChanged = started");
         reactOnLocationListener(null, newLocation);
//         showLocation(newLocation); // contains checkInternet() inside \
      }
   }; // new LocationListener nameless class description ended \

// service lifecycle started \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

   @Nullable
   @Override
   public IBinder onBind(Intent intent) {
      return null;
   }

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
      MyLog.v("onStartCommand = MainService started");

      // now starting service as from zero \
      pendingIntent = intent.getParcelableExtra(GlobalKeys.P_I_KEY);
      // when service is restarted after reboot - intent is empty \
      if (pendingIntent == null)
         pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, new Intent(), 0);

      qrFromActivity = intent.getStringExtra(GlobalKeys.QR_KEY);
      // when service is restarted after reboot - intent is empty \
      if (qrFromActivity == null)
         // just restoring the data from shared preferances \
         qrFromActivity = getApplicationContext().
               getSharedPreferences(GlobalKeys.S_P_NAME, MODE_PRIVATE).
               getString(GlobalKeys.S_P_QR_KEY, "");
      // we assume that QR-code contains valid web URL inside \
      MyLog.v("getStringExtra: " + qrFromActivity);

      // main job for the service \
      gpsTrackingStart();
      return Service.START_REDELIVER_INTENT;
   }

// service lifecycle ended \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

   public void gpsTrackingStart() {

      locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
      connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

      // this check is required by IDE \
      if (ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this,
                  Manifest.permission.ACCESS_FINE_LOCATION)
                  != PackageManager.PERMISSION_GRANTED) {
         MyLog.v("permissions are not set well");
      } else {
         locationManager.requestLocationUpdates(
               LocationManager.GPS_PROVIDER,
               MIN_PERIOD_MILLISECONDS,
               MIN_DISTANCE_IN_METERS,
               locationListener);
      }
      // just for testing purpose and to check the URL at the service start \
      sendInfoToServer();
   }

   private void reactOnLocationListener(String provider, Location newLocation) {
      // only one location point - for only one line of network requests \
      Location location;

      // this check is required by IDE \
      if (ActivityCompat.checkSelfPermission(getApplicationContext(),
            Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(getApplicationContext(),
                  Manifest.permission.ACCESS_COARSE_LOCATION)
                  != PackageManager.PERMISSION_GRANTED) {
         return;
      }
      if (provider == null) location = newLocation;
      else location = locationManager.getLastKnownLocation(provider);

      if (location != null) {
         latitude = location.getLatitude();
         longitude = location.getLongitude();
         timeTaken = location.getTime();
      }
      // first we have to check internet availability and inform activity about it \
      if (checkInternet()) sendInfoToServer(); // this is the last action for service job \
      // creation and two puts are made with one line here \
      Intent intentToReturn = new Intent()
            .putExtra(GlobalKeys.GPS_LATITUDE, latitude)
            .putExtra(GlobalKeys.GPS_LONGITUDE, longitude)
            .putExtra(GlobalKeys.GPS_TAKING_TIME, timeTaken);
      sendIntentToActivity(intentToReturn, GlobalKeys.P_I_CODE_DATA_FROM_GPS); // 100
   }

   // universal point to send info to MainActivity \
   private void sendIntentToActivity(Intent intent, int code) {
      try {
         pendingIntent.send(this, code, intent);
      } catch (PendingIntent.CanceledException e) {
         e.printStackTrace();
      }
   }

   private boolean checkInternet() {

      NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
      try {
         if (networkInfo != null) {
            if (networkInfo.isConnected()) {
               pendingIntent.send(GlobalKeys.P_I_CONNECTION_ON);
               return true;
            } else {
               pendingIntent.send(GlobalKeys.P_I_CONNECTION_OFF);
               return false;
            }
         } else {
            pendingIntent.send(GlobalKeys.P_I_CONNECTION_OFF);
            return false;
         }
      } catch (PendingIntent.CanceledException e) {
         e.printStackTrace();
         return false;
      }
   }

   // my Retrofit usage to send tracking data to the server \
   public void sendInfoToServer() {
      MyLog.v("sendInfoToServer = started");

      // at first creating object to send to the server \
      DriverData driverData = new DriverData(latitude, longitude, timeTaken);

      Gson gson = new GsonBuilder().create();

      String jsonToSend = gson.toJson(driverData, DriverData.class);
      MyLog.v("jsonToSend: " + jsonToSend);

      MediaType JSON = MediaType.parse("application/json; charset=utf-8");

      RequestBody body = RequestBody.create(JSON, jsonToSend);

      try {
         Request request = new Request.Builder()
               .url(qrFromActivity)
//               .cacheControl(new CacheControl.Builder().noCache().build()) // no effect \
               .post(body)
               .build();

         OkHttpClient okHttpClient = new OkHttpClient();

         okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
               MyLog.d("onFailure: " + call.request().method());
//            MyLog.d("onFailure: " + call.request().toString());
               MyLog.d("onFailure: " + call.request().body().contentType().toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
               MyLog.d("onResponse: " + response.message());
               if (response.isSuccessful()) {
                  MyLog.d("onResponse: successfull - OkHTTP");
                  MyLog.d("onResponse: " + response.body().string());
                  MyLog.d("onResponse: " + response.body().contentType().toString());
               } else {
                  MyLog.d("onResponse: " + call.request().body().contentType().toString());
                  MyLog.d("sFromResponse: " + response.message());
               }
            }
         });
      } catch (IllegalArgumentException iae) {
         iae.printStackTrace();
         // we must inform activity about wrong
         Intent intent = new Intent().putExtra(GlobalKeys.QR_KEY_INVALID, qrFromActivity);
         sendIntentToActivity(intent, GlobalKeys.P_I_CODE_QR_KEY_INVALID);
      }
   } // end of sendInfoToServer-method \\
}