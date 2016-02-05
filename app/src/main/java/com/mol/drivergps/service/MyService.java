package com.mol.drivergps.service;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.mol.drivergps.GlobalKeys;
import com.mol.drivergps.entity_description.DriverData;
import com.mol.drivergps.rest_connection.MyRetrofitInterface;
import com.mol.drivergps.rest_connection.MyServiceGenerator;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyService extends Service {

   private LocationManager locationManager;

   private final static long MIN_PERIOD_MILLISECONDS = 5 * 1000;
   private final static float MIN_DISTANCE_IN_METERS = 25;

   private PendingIntent pendingIntent;
   private String qrFromDB, coordinates, timeTaken;

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
   }

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
      Log.d("onStartCommand", "MyService started");
      /*
      if (trackGps())
//         pendingIntent = intent.getParcelableExtra(GlobalKeys.PENDING_INTENT_KEY);
         qrFromDB = intent.getStringExtra(GlobalKeys.QR_KEY);
      else
         Log.d("trackGps", "is false!!!");
      */
      pendingIntent = intent.getParcelableExtra(GlobalKeys.PENDING_INTENT_KEY);
      qrFromDB = intent.getStringExtra(GlobalKeys.QR_KEY);
      Log.d("getStringExtra", qrFromDB);
      // service should not work without QR supplied \
      if (qrFromDB == null) {
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
   }

// service lifecycle ended \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

   public void trackGps() {

      locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

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
   }

   // definition of special object for listener \
   private LocationListener locationListener = new LocationListener() {

      @Override
      public void onProviderDisabled(String provider) {
         Log.d("onProviderDisabled", "happened");
         try {
            pendingIntent.send(GlobalKeys.P_I_CODE_PROVIDER_DISABLED);
         } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
         }
      }

      @Override
      public void onProviderEnabled(String provider) {
         Log.d("onProviderEnabled", "started");

         // this check is required by IDE \
         if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                  != PackageManager.PERMISSION_GRANTED
                  &&
                  ActivityCompat.checkSelfPermission(getApplicationContext(),
                           Manifest.permission.ACCESS_COARSE_LOCATION)
                           != PackageManager.PERMISSION_GRANTED) {
            return;
         }
         showLocation(locationManager.getLastKnownLocation(provider));
         try {
            pendingIntent.send(GlobalKeys.P_I_CODE_PROVIDER_ENABLED);
         } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
         }
      }

      @Override
      public void onLocationChanged(Location location) {
         Log.d("onLocationChanged", "started");
         showLocation(location);
         try {
            pendingIntent.send(GlobalKeys.P_I_CODE_LOCATION_CHANGED);
         } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
         }
      }

      @Override
      public void onStatusChanged(String provider, int status, Bundle extras) {
         Log.d("onStatusChanged", "started");
         try {
            pendingIntent.send(GlobalKeys.P_I_CODE_STATUS_CHANGED);
         } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
         }
      }

   }; // new LocationListener nameless class description ended \

   private void showLocation(Location location) {
      if (location == null)
         return;

      coordinates = String.valueOf("Latitude: " + location.getLatitude() + " Longitude: " + location.getLongitude());
      timeTaken = String.valueOf("Time: " + location.getTime());

      // this is the last action for service job \
      sendInfoToServer();
      // creation and two puts are made with one line here \
      Intent intentToReturn = new Intent()
               .putExtra(GlobalKeys.GPS_COORDINATES, coordinates)
               .putExtra(GlobalKeys.GPS_TAKING_TIME, timeTaken);
      try {
         pendingIntent.send(MyService.this, GlobalKeys.P_I_CODE_GPS_DATA, intentToReturn);
      } catch (PendingIntent.CanceledException e) {
         e.printStackTrace();
      }
   }

   // my Retrofit usage to send tracking data to the server \
   public void sendInfoToServer() {

      Log.d("sendInfoToServer", "started");

      // at first creating object to send to the server \
      DriverData driverData = new DriverData(qrFromDB, coordinates, timeTaken);

      // using our service class for creation of interface object \
      MyRetrofitInterface myRetrofitInterface = MyServiceGenerator.createService(MyRetrofitInterface.class);

      // preparing the network access object - the call \
      Call<DriverData> driverDataCall = myRetrofitInterface.makeDriverDataCall(driverData);

      // performing the network connection itself \
      driverDataCall.enqueue(new Callback<DriverData>() {

         @Override
         public void onResponse(Response<DriverData> response) {
            Log.d("onResponse", response.toString());

            if (response.isSuccess()) {
               Log.d("onResponse", "is successfull");
            } else {
               Log.d("onResponse", "is not successfull");
            }
         }

         @Override
         public void onFailure(Throwable t) {
            Log.d("onFailure", t.getMessage());
         }
      });
   } // end of sendInfoToServer-method \\
}