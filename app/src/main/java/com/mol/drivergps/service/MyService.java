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

public class MyService extends Service {

   private LocationManager locationManager;

   private final static long MIN_PERIOD_MILLISECONDS = 5 * 1000;
   private final static float MIN_DISTANCE_IN_METERS = 25;

   PendingIntent pendingIntent;

// service lifecycle started \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

   @Nullable
   @Override
   public IBinder onBind(Intent intent) {

      return null;
   }

   @Override
   public void onCreate() {
      super.onCreate();
   }

   public int onStartCommand(Intent intent, int flags, int startId) {

      Log.d("onStartCommand", "MyService started");

      // main job for the service \
      if (trackGps())
         pendingIntent = intent.getParcelableExtra(GlobalKeys.PARAM_PINTENT);
      else
         Log.d("trackGps", "is false!!!");

      return super.onStartCommand(intent, flags, startId);
//      return Service.START_STICKY;
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
      Log.d("onDestroy", "worked = service is dead");
   }

// service lifecycle ended \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

   public boolean trackGps() {

      locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
               != PackageManager.PERMISSION_GRANTED
               &&
               ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
         Log.d("permissions", "are not set well");
         return false;
      }
      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
               MIN_PERIOD_MILLISECONDS, MIN_DISTANCE_IN_METERS, locationListener);
      return true;
   }

   private LocationListener locationListener = new LocationListener() {

      @Override
      public void onLocationChanged(Location location) {
         Log.d("onLocationChanged", "started");
         showLocation(location);
      }

      @Override
      public void onProviderDisabled(String provider) {
         Log.d("onProviderDisabled", "started");
      }

      @Override
      public void onProviderEnabled(String provider) {
         Log.d("onProviderEnabled", "started");

         if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                  != PackageManager.PERMISSION_GRANTED
                  &&
                  ActivityCompat.checkSelfPermission(getApplicationContext(),
                  Manifest.permission.ACCESS_COARSE_LOCATION)
                           != PackageManager.PERMISSION_GRANTED) {
            return;
         }
         showLocation(locationManager.getLastKnownLocation(provider));
         Log.d("onProviderEnabled", "worked");
      }

      @Override
      public void onStatusChanged(String provider, int status, Bundle extras) {
         Log.d("onStatusChanged", "started");
      }
   };

   private void showLocation(Location location) {
      if (location == null)
         return;

      String coord = String.valueOf(location.getLatitude() + " " + location.getLongitude());
      String timeCoordTaken = String.valueOf("Time: " + location.getTime());

      Intent intentToRet = new Intent().putExtra(GlobalKeys.GPS_LOCATION, coord).putExtra(GlobalKeys.GPS_TIME, timeCoordTaken);
      try {
         pendingIntent.send(MyService.this, GlobalKeys.INTENT_CODE_GPS, intentToRet);
      } catch (PendingIntent.CanceledException e) {
         e.printStackTrace();
      }
   }
}