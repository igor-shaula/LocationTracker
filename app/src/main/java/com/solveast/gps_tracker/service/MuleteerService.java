package com.solveast.gps_tracker.service;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.solveast.gps_tracker.GlobalKeys;
import com.solveast.gps_tracker.entity_description.DriverData;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MuleteerService extends Service {

   private final static long MIN_PERIOD_MILLISECONDS = 5 * 1000;
   private final static float MIN_DISTANCE_IN_METERS = 25;

   private LocationManager locationManager;
   private ConnectivityManager connectivityManager;

   private PendingIntent pendingIntent;
   private String qrFromActivity;
   private double latitude, longitude;
   private long timeTaken;

   private LocalBroadcastManager localBroadcastManager;
   private BroadcastReceiver broadcastReceiver;

// service lifecycle started \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

   @Nullable
   @Override
   public IBinder onBind(Intent intent) {
      return null;
   }

   @Override
   public void onCreate() {
      super.onCreate();
      Log.d("onCreate", "worked = service is born");
      localBroadcastManager = LocalBroadcastManager.getInstance(this);
   }

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
      Log.d("onStartCommand", "MuleteerService started");

      // first we have to answer the calling activity if the service is started \
      broadcastReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(GlobalKeys.START_SERVICE_CHECK, false)) {
               intent.putExtra(GlobalKeys.START_SERVICE_CHECK, true);
               localBroadcastManager.sendBroadcast(intent);
            }
         }
      };
      IntentFilter intentFilter = new IntentFilter(GlobalKeys.LOCAL_BROADCAST_SERVICE_CHECK);
      localBroadcastManager.registerReceiver(broadcastReceiver, intentFilter);

      // now starting service as from zero \
      pendingIntent = intent.getParcelableExtra(GlobalKeys.PENDING_INTENT_KEY);
      qrFromActivity = intent.getStringExtra(GlobalKeys.QR_KEY);
      Log.d("getStringExtra", qrFromActivity);
      // service should not work without QR supplied \
      if (qrFromActivity == null) {
         stopSelf();
      }
      // main job for the service \
      trackGps();
      return Service.START_REDELIVER_INTENT;
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
      Log.d("onDestroy", "worked = service is dead");
      localBroadcastManager.unregisterReceiver(broadcastReceiver);
   }

// service lifecycle ended \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

   public void trackGps() {

      locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
      connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

      // this check is required by IDE \
      if (ActivityCompat.checkSelfPermission(this,
               Manifest.permission.ACCESS_COARSE_LOCATION)
               != PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
         Log.d("permissions", "are not set well");
      } else {
         locationManager.requestLocationUpdates(
                  LocationManager.GPS_PROVIDER,
                  MIN_PERIOD_MILLISECONDS,
                  MIN_DISTANCE_IN_METERS,
                  locationListener);
      }
//      sendInfoToServer();
   }

   // definition of special object for listener \
   private LocationListener locationListener = new LocationListener() {

      @Override
      public void onProviderDisabled(String provider) {
         Log.d("onProviderDisabled", "happened");
         checkInternet();

         // this check is required by IDE \
         if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                  Manifest.permission.ACCESS_FINE_LOCATION)
                  != PackageManager.PERMISSION_GRANTED &&
                  ActivityCompat.checkSelfPermission(getApplicationContext(),
                           Manifest.permission.ACCESS_COARSE_LOCATION)
                           != PackageManager.PERMISSION_GRANTED) {
            return;
         }
         showLocation(locationManager.getLastKnownLocation(provider)); // 100
      }

      @Override
      public void onProviderEnabled(String provider) {
         Log.d("onProviderEnabled", "started");
         checkInternet();

         // this check is required by IDE \
         if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                  Manifest.permission.ACCESS_FINE_LOCATION)
                  != PackageManager.PERMISSION_GRANTED &&
                  ActivityCompat.checkSelfPermission(getApplicationContext(),
                           Manifest.permission.ACCESS_COARSE_LOCATION)
                           != PackageManager.PERMISSION_GRANTED) {
            return;
         }
         showLocation(locationManager.getLastKnownLocation(provider)); // 100
      }

      @Override
      public void onStatusChanged(String provider, int status, Bundle extras) {
         Log.d("onStatusChanged", "started");
         checkInternet();

         // this check is required by IDE \
         if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                  Manifest.permission.ACCESS_FINE_LOCATION)
                  != PackageManager.PERMISSION_GRANTED &&
                  ActivityCompat.checkSelfPermission(getApplicationContext(),
                           Manifest.permission.ACCESS_COARSE_LOCATION)
                           != PackageManager.PERMISSION_GRANTED) {
            return;
         }
         showLocation(locationManager.getLastKnownLocation(provider)); // 100
      }

      @Override
      public void onLocationChanged(Location location) {
         Log.d("onLocationChanged", "started");
         checkInternet();
         showLocation(location);
      }
   }; // new LocationListener nameless class description ended \

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

   private void showLocation(Location location) {
      if (location == null)
         return;

      latitude = location.getLatitude();
      longitude = location.getLongitude();
      timeTaken = location.getTime();

      // first we have to check internet availability and inform activity about it \
      if (checkInternet()) sendInfoToServer(); // this is the last action for service job \
      // creation and two puts are made with one line here \
      Intent intentToReturn = new Intent()
               .putExtra(GlobalKeys.GPS_LATITUDE, latitude)
               .putExtra(GlobalKeys.GPS_LONGITUDE, longitude)
               .putExtra(GlobalKeys.GPS_TAKING_TIME, timeTaken);
      try {
         pendingIntent.send(this, GlobalKeys.P_I_CODE_DATA_FROM_GPS, intentToReturn); // -100
      } catch (PendingIntent.CanceledException e) {
         e.printStackTrace();
      }
   }

   // my Retrofit usage to send tracking data to the server \
   public void sendInfoToServer() {
      Log.d("sendInfoToServer", "started");

      // at first creating object to send to the server \
      DriverData driverData = new DriverData(latitude, longitude, timeTaken);

      Gson gson = new GsonBuilder().create();

      String jsonToSend = gson.toJson(driverData, DriverData.class);
      Log.d("jsonToSend", jsonToSend);

      MediaType JSON = MediaType.parse("application/json; charset=utf-8");

      RequestBody body = RequestBody.create(JSON, jsonToSend);

      Request request = new Request.Builder()
               .url("http://" + qrFromActivity)
               .post(body)
               .build();

      OkHttpClient okHttpClient = new OkHttpClient();

      okHttpClient.newCall(request).enqueue(new Callback() {
         @Override
         public void onFailure(Call call, IOException e) {
            Log.d("onFailure", e.getMessage());
         }

         @Override
         public void onResponse(Call call, Response response) throws IOException {
            Log.d("onResponse", response.message());
            if (response.isSuccessful()) Log.d("onResponse", "successfull - OkHTTP");
            else {
               Log.d("onResponse", call.request().toString());
               Log.d("onResponse", call.request().body().contentType().toString());
            }
         }
/*
         @Override
         public void onFailure(Request request, IOException e) {
            Log.d("onFailure", request.toString());
         }

         @Override
         public void onResponse(okhttp3.Response response) throws IOException {
            Log.d("onResponse", response.message());
            if (response.isSuccessful()) Log.d("onResponse", "successfull - OkHTTP");
            else Log.d("onResponse", "is not successfull - OkHTTP");
         }*/
      });

// Retrofit actually doesn't work and is successfully replaced by OkHTTP ///////////////////////////
/*
      // using our service class for creation of interface object \
      MyRetrofitInterface myRetrofitInterface = MyServiceGenerator.createService(MyRetrofitInterface.class);

      // retrofit requires the base URL to be separated - let's do it \
      int dividerPosition = 0;
      for (int i = 2; i < qrFromActivity.length(); i++) {
         if (qrFromActivity.charAt(i) == '/') {
            dividerPosition = i;
            break;
         }
      }
      String uriForCall = qrFromActivity.substring(dividerPosition);
      Log.d("uriForCall", uriForCall);

      // preparing the network access object - the call \
      Call<DriverData> driverDataCall = myRetrofitInterface.makeDriverDataCall(uriForCall, driverData);

      // performing the network connection itself \
      driverDataCall.enqueue(new Callback<DriverData>() {

         @Override
         public void onResponse(Response<DriverData> response) {
            Log.d("onResponse", response.message());
            if (response.isSuccess()) {
               Log.d("onResponse", "is successfull - Retrofit");
            } else {
               Log.d("onResponse", "is not successfull - Retrofit");
               Log.d("onResponse", "" + response.code());
               Log.d("onResponse", response.message());
            }
         }

         @Override
         public void onFailure(Throwable t) {
            Log.d("onFailure", "happened - Retrofit");
            t.printStackTrace();
         }
      });
*/
   } // end of sendInfoToServer-method \\
}