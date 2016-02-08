package com.mol.drivergps.service;

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
import com.mol.drivergps.GlobalKeys;
import com.mol.drivergps.entity_description.DriverData;
import com.mol.drivergps.rest_connection_settings.MyRetrofitInterface;
import com.mol.drivergps.rest_connection_settings.MyServiceGenerator;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyService extends Service {

   private final static long MIN_PERIOD_MILLISECONDS = 5 * 1000;
   private final static float MIN_DISTANCE_IN_METERS = 25;

   private LocationManager locationManager;
   private PendingIntent pendingIntent;
   private String qrFromActivity;
   private double latitude, longitude;
   private long timeTaken;
   private Gson gson = new GsonBuilder().create();

   private LocalBroadcastManager localBroadcastManager;
   private BroadcastReceiver broadcastReceiver;

   private ConnectivityManager connectivityManager;

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
      Log.d("onStartCommand", "MyService started");
      /*
      if (trackGps())
//         pendingIntent = intent.getParcelableExtra(GlobalKeys.PENDING_INTENT_KEY);
         qrFromActivity = intent.getStringExtra(GlobalKeys.QR_KEY);
      else
         Log.d("trackGps", "is false!!!");
      */

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
//      return super.onStartCommand(intent, flags, startId);
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
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
               != PackageManager.PERMISSION_GRANTED
               &&
               ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
         Log.d("permissions", "are not set well");
      } else {
         locationManager.requestLocationUpdates(
                  LocationManager.GPS_PROVIDER,
                  MIN_PERIOD_MILLISECONDS,
                  MIN_DISTANCE_IN_METERS,
                  locationListener);
      }
      // this is a plug only to test connection \
      sendInfoToServer();
   }

   // definition of special object for listener \
   private LocationListener locationListener = new LocationListener() {

      @Override
      public void onProviderDisabled(String provider) {
         Log.d("onProviderDisabled", "happened");
         checkInternet();

         // this check is required by IDE \
         if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                  != PackageManager.PERMISSION_GRANTED
                  &&
                  ActivityCompat.checkSelfPermission(getApplicationContext(),
                           Manifest.permission.ACCESS_COARSE_LOCATION)
                           != PackageManager.PERMISSION_GRANTED) {
            return;
         }
         showLocation(locationManager.getLastKnownLocation(provider)); // 100
/*
         try {
            pendingIntent.send(GlobalKeys.P_I_PROVIDER_DISABLED); // 100
         } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
         }
*/
      }

      @Override
      public void onProviderEnabled(String provider) {
         Log.d("onProviderEnabled", "started");
         checkInternet();

         // this check is required by IDE \
         if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                  != PackageManager.PERMISSION_GRANTED
                  &&
                  ActivityCompat.checkSelfPermission(getApplicationContext(),
                           Manifest.permission.ACCESS_COARSE_LOCATION)
                           != PackageManager.PERMISSION_GRANTED) {
            return;
         }
         showLocation(locationManager.getLastKnownLocation(provider)); // 100
/*
         try {
            pendingIntent.send(GlobalKeys.P_I_PROVIDER_ENABLED); // 101
         } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
         }
*/
      }

      @Override
      public void onLocationChanged(Location location) {
         Log.d("onLocationChanged", "started");
         checkInternet();

         showLocation(location);
/*
         try {
            pendingIntent.send(GlobalKeys.P_I_LOCATION_CHANGED); // 102
         } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
         }
*/
      }

      @Override
      public void onStatusChanged(String provider, int status, Bundle extras) {
         Log.d("onStatusChanged", "started");
         checkInternet();

         // this check is required by IDE \
         if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                  != PackageManager.PERMISSION_GRANTED
                  &&
                  ActivityCompat.checkSelfPermission(getApplicationContext(),
                           Manifest.permission.ACCESS_COARSE_LOCATION)
                           != PackageManager.PERMISSION_GRANTED) {
            return;
         }
         showLocation(locationManager.getLastKnownLocation(provider)); // 100
/*
         try {
            pendingIntent.send(GlobalKeys.P_I_STATUS_CHANGED); // 103
         } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
         }
*/
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
/*
   private boolean checkGps() { // this doesn't work for compilator \
      return (ActivityCompat.checkSelfPermission(getApplicationContext(),
               Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
               &&
               ActivityCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
   }
*/
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
      // preparing URI for muleteer.herokuapp.com/tracking/mule-1

      int dividerPosition = 0;
      for (int i = 2; i < qrFromActivity.length(); i++) {
         if (qrFromActivity.charAt(i) == '/') {
            dividerPosition = i;
            break;
         }
      }
//      String uriForCall = qrFromActivity.substring(dividerPosition + 1);
      String uriForCall = qrFromActivity.substring(dividerPosition);
      Log.d("uriForCall", uriForCall);
      // http://muleteer.herokuapp.com/tracking/mule-1 = from this app
      // muleteer.herokuapp.com/tracking/mule-1 = from QR-scanning

      // at first creating object to send to the server \
      DriverData driverData = new DriverData(latitude, longitude, timeTaken);

      // preparing JSON from our object \
      String stringToSend = gson.toJson(driverData, DriverData.class);
      Log.d("stringToSend", stringToSend);
      // {"lat":50.44757817,"lng":30.59523956,"date":1454881104000} - from this app
      // {"lat":50.48241842,"lng":30.48806705,"date":1454921224000}
      // { "lat": 77.77, "lng": 55.55, "date": ' + result + ' } - from sample

      // using our service class for creation of interface object \
      MyRetrofitInterface myRetrofitInterface = MyServiceGenerator.createService(MyRetrofitInterface.class);

      // preparing the network access object - the call \
      Call<DriverData> driverDataCall = myRetrofitInterface.makeDriverDataCall(uriForCall, driverData);
//      Call<DriverData> driverDataCall = myRetrofitInterface.makeDriverDataCall(uriForCall, stringToSend);

      // performing the network connection itself \
      driverDataCall.enqueue(new Callback<DriverData>() {

         @Override
         public void onResponse(Response<DriverData> response) {
            Log.d("onResponse", response.toString());

            if (response.isSuccess()) {
               try {
                  Intent intent = new Intent().putExtra(GlobalKeys.CONNECTION_RESULT, response.message());
                  pendingIntent.send(getApplicationContext(), GlobalKeys.P_I_CODE_CONNECTION_OK, intent);
               } catch (PendingIntent.CanceledException e) {
                  e.printStackTrace();
               }
               Log.d("onResponse", "is successfull");
            } else {
               Log.d("onResponse", "is not successfull");
               Log.d("onResponse", "" + response.code());
               Log.d("onResponse", response.message());
            }
         }

         @Override
         public void onFailure(Throwable t) {
            Log.d("onFailure", "happened");
            t.printStackTrace();
         }
      });
   } // end of sendInfoToServer-method \\
}