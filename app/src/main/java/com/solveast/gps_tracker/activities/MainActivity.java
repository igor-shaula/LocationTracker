package com.solveast.gps_tracker.activities;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.solveast.gps_tracker.GlobalKeys;
import com.solveast.gps_tracker.MyLog;
import com.solveast.gps_tracker.R;
import com.solveast.gps_tracker.service.MainService;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

   private AppCompatButton acbScanQR;
   private SwitchCompat scTrackingStatus;
   private AppCompatTextView actvGpsStatus;
   private AppCompatTextView actvInetStatus;
   private AppCompatTextView actvGpsData;
   private AppCompatTextView actvGpsTime;

   private String qrFromSP;
   private int whiteColor;
   private int primaryDarkColor;
   private int primaryTextColor;
   private int accentColor;
   private boolean trackingIsOn = false;

   private Vibrator vibrator;
   private LocationManager locationManager;
   private ConnectivityManager connectivityManager;

// LIFECYCLE \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);

      acbScanQR = (AppCompatButton) findViewById(R.id.acb_ScanQR);
      scTrackingStatus = (SwitchCompat) findViewById(R.id.sc_TrackingStatus);

      actvGpsStatus = (AppCompatTextView) findViewById(R.id.actv_GpsStatus);
      actvInetStatus = (AppCompatTextView) findViewById(R.id.actv_InetStatus);
      actvGpsData = (AppCompatTextView) findViewById(R.id.actv_GpsData);
      actvGpsTime = (AppCompatTextView) findViewById(R.id.actv_GpsTime);

      vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

      scTrackingStatus.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {

               @Override
               public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                  // getting some touch feedback about start/stop action \
                  if (isChecked) {
                     startTracking();
                     if (isGpsEnabled() && isInetEnabled()) {
                        actvGpsData.setText(getString(R.string.gpsDataOn));
                        actvGpsTime.setText(getString(R.string.gpsTimeOn));
                     }
                  } else {
                     stopTracking();
                     actvGpsData.setText(getString(R.string.gpsDataOff));
                     actvGpsTime.setText(getString(R.string.gpsTimeOff));
                  }
                  updateGpsData(null);
               }
            }
      );
/*
       setting the initial state of the buttons depending on QR availlability
       and whether service running state in background at the start of activity \
*/
      whiteColor = ContextCompat.getColor(this, android.R.color.white);
      primaryDarkColor = ContextCompat.getColor(this, R.color.primary_dark);
      primaryTextColor = ContextCompat.getColor(this, R.color.primary_text);
      accentColor = ContextCompat.getColor(this, R.color.accent);

      // 0 = setting QR-code and its view \
      qrFromSP = getSharedPreferences(GlobalKeys.S_P_NAME, MODE_PRIVATE)
            .getString(GlobalKeys.S_P_QR_KEY, "");
//      qrFromSP = getPreferences(MODE_PRIVATE).getString(GlobalKeys.S_P_QR_KEY, "");
      setScanQrButtonStatus();

      // 1 = checking if the service has already being running at the start of this activity \
      if (isMyServiceRunning(MainService.class)) trackingIsOn = true;
      setTrackingSwitchStatus(trackingIsOn);

      // 2 = checking the state of GPS - only to inform user \
      locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
      isGpsEnabled();

      // 3 = checking the state of internet - only to inform user \
      connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
      isInetEnabled();

      // 4 = this check is necessary for correct application relaunch \
      updateGpsData(null);
   } // end of onCreate-method \

// METHODS TO SWITCH STATES OF ALL VIEW ELEMENTS \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

   // crazy simple magic method - it finds my service among others \
   private boolean isMyServiceRunning(Class<?> serviceClass) {
      ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
      for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
         if (serviceClass.getName().equals(service.service.getClassName())) return true;
      }
      return false;
   }

   private void setScanQrButtonStatus() {

      // setting appearance optimal to show user the state of data \
      if (!qrFromSP.equals("")) {
         acbScanQR.setText(getString(R.string.textForPresentScan));
         acbScanQR.setTextColor(primaryDarkColor);
         acbScanQR.setBackgroundResource(R.drawable.my_rounded_button_shape);
         scTrackingStatus.setVisibility(View.VISIBLE);
      } else {
         acbScanQR.setText(getString(R.string.textForNewScan));
         acbScanQR.setTextColor(whiteColor);
         acbScanQR.setBackgroundResource(R.drawable.my_rounded_button_shape_dark);
         scTrackingStatus.setVisibility(View.INVISIBLE);
      }
   }

   private void setTrackingSwitchStatus(boolean statusOn) {
      if (statusOn) {
         scTrackingStatus.setText(getString(R.string.textForTrackingSwitchedOn));
         scTrackingStatus.setTextColor(primaryDarkColor);
         scTrackingStatus.setBackgroundResource(R.drawable.my_rounded_button_shape);
         scTrackingStatus.setChecked(true);
         trackingIsOn = true;
         // informing user about this \
         vibrator.vibrate(100);
      } else {
         scTrackingStatus.setText(getString(R.string.textForTrackingSwitchedOff));
         scTrackingStatus.setTextColor(whiteColor);
         scTrackingStatus.setBackgroundResource(R.drawable.my_rounded_button_shape_dark);
         scTrackingStatus.setChecked(false);
         trackingIsOn = false;
      }
   }

   private boolean isGpsEnabled() { // also changes appearance of GPS text view \
      if (locationManager != null) {
         if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            actvGpsStatus.setText(getString(R.string.gpsEnabled));
            actvGpsStatus.setTextColor(primaryDarkColor);
            return true;
         } else {
            actvGpsStatus.setText(getString(R.string.gpsDisabled));
            actvGpsStatus.setTextColor(primaryTextColor);
            return false;
         }
      } else {
         actvGpsStatus.setText(getString(R.string.gpsDisabled));
         actvGpsStatus.setTextColor(primaryTextColor);
         MyLog.v("locationManager is null");
         return false;
      }
   }

   public boolean isInetEnabled() { // also changes appearance of inet info view \
      NetworkInfo networkInfo;
      if (connectivityManager != null)
         networkInfo = connectivityManager.getActiveNetworkInfo();
      else return false;
      if (networkInfo != null) {
         if (networkInfo.isConnected()) {
            actvInetStatus.setText(getString(R.string.inetConnected));
            actvInetStatus.setTextColor(primaryDarkColor);
            return true;
         } else {
            actvInetStatus.setText(getString(R.string.inetDisconnected));
            actvInetStatus.setTextColor(primaryTextColor);
            return false;
         }
      } else {
         actvInetStatus.setText(getString(R.string.inetDisconnected));
         actvInetStatus.setTextColor(primaryTextColor);
         MyLog.v("connectivityManager is null");
         return false;
      }
   }

// MAIN SET OF METHODS \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

   public void startTracking() {
      // preparing intent for qr-code sending \
      PendingIntent pendingIntent = createPendingResult(1, new Intent(), 0);
      Intent intentServiceGps = new Intent(this, MainService.class);
      intentServiceGps.putExtra(GlobalKeys.QR_KEY, qrFromSP);
      intentServiceGps.putExtra(GlobalKeys.P_I_KEY, pendingIntent);

      startService(intentServiceGps);
      setTrackingSwitchStatus(true);
   }

   public void stopTracking() {
      stopService(new Intent(this, MainService.class));
      setTrackingSwitchStatus(false);
   }

   // button pressed listener = get QR code \
   public void qrCodeReading(View view) {
      Intent intent = new Intent(MainActivity.this, QrActivity.class);
      startActivityForResult(intent, GlobalKeys.QR_ACTIVITY_KEY);
   }

   // returning point to this activity \
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);

      MyLog.v("onActivityResult = requestCode: " + String.valueOf(requestCode));
      MyLog.v("onActivityResult = resultCode: " + String.valueOf(resultCode));

      // recognizing what has come by contents of resultCode \
      switch (resultCode) {
         // result from QrActivity \
         case GlobalKeys.QR_ACTIVITY_KEY: {
            MyLog.v("resultCode = GlobalKeys.QR_ACTIVITY_KEY");
            if (data != null) {
               MyLog.v("onActivityResult  = data != null");
               String newQrCode = data.getStringExtra(GlobalKeys.QR_RESULT);
               // if a new code is acquired \
               if (!qrFromSP.equals(newQrCode)) {
                  // updating our QR-code for the next scan \
                  qrFromSP = newQrCode;
                  // fixing the changes \
                  saveQrToSharedPrefs(newQrCode);
                  // changing the view of scanning button \
                  setScanQrButtonStatus();
                  // location service needs to be stopped after new code is taken - to avoid wrong data \
                  stopTracking();
                  // setting my own toast \
                  Toast.makeText(this, getString(R.string.newQR_CodeIsSet), Toast.LENGTH_SHORT).show();
               } else
                  Toast.makeText(this, getString(R.string.oldQR_CodeIsKept), Toast.LENGTH_SHORT).show();
               // the only point to enable start of the tracking \
               scTrackingStatus.setVisibility(View.VISIBLE);
            } else {
               MyLog.v("onActivityResult = data is null");
            }
            break;
         }
         // result about GPS from service - incoming intent available \
         case GlobalKeys.P_I_CODE_DATA_FROM_GPS: {
            MyLog.v("resultCode = GlobalKeys.P_I_CODE_DATA_FROM_GPS");
            if (trackingIsOn)
               updateGpsData(data); // data from GPS is obtained and the service is running \
            break;
         }
         // in this case QR-code is detected to be unusable - everything has to be stopped \
         case GlobalKeys.P_I_CODE_QR_KEY_INVALID: {
            Toast.makeText(this, getString(R.string.invalidQR_Code), Toast.LENGTH_SHORT).show();
            // immidiately stopping our service - but on Meizu it sent signal even dead :)
            stopTracking();
            // checking and deleting invalid code \
            String invalidQR = data.getStringExtra(GlobalKeys.QR_KEY_INVALID);
            saveQrToSharedPrefs(null);
            if (qrFromSP.equals(invalidQR)) qrFromSP = "";
            // updating the state of the QR-code button \
            setScanQrButtonStatus();
            break;
         }
         // result about the state of connection from service - just to update \
         case GlobalKeys.P_I_CONNECTION_OFF:
         case GlobalKeys.P_I_CONNECTION_ON: {
            MyLog.v("resultCode = GlobalKeys.P_I_CONNECTION_ON/OFF");
            // fixing the bug when inet status updated but GPS - not \
            isGpsEnabled();
            isInetEnabled();
            break;
         }
      } // end of switch-statement \\
   } // end of onActivityResult-method \\

   private void updateGpsData(Intent data) {
      if (data != null) {

         // preparing fields for location arguments \
         double latitude = data.getDoubleExtra(GlobalKeys.GPS_LATITUDE, 0.0);
         double longitude = data.getDoubleExtra(GlobalKeys.GPS_LONGITUDE, 0.0);
         String coordinates = String.valueOf("Lat. " + latitude + " / Long. " + longitude);
         actvGpsData.setText(coordinates);

         // preparing field for time data \
         long timeOfTakingCoordinates = data.getLongExtra(GlobalKeys.GPS_TAKING_TIME, 0);
         Calendar calendar = Calendar.getInstance();
         calendar.setTimeInMillis(timeOfTakingCoordinates);
         String stringTime = calendar.getTime().toString();
         actvGpsTime.setText(String.valueOf("Time: " + stringTime));

         actvGpsData.setTextColor(accentColor);
         actvGpsTime.setTextColor(accentColor);
      } else {
         // data is absent - we have nothing to show \
         actvGpsData.setTextColor(primaryTextColor);
         actvGpsTime.setTextColor(primaryTextColor);

         if (isMyServiceRunning(MainService.class)) {
            actvGpsData.setText(getString(R.string.gpsDataOn));
            actvGpsTime.setText(getString(R.string.gpsTimeOn));
         } else {
            actvGpsData.setText(getString(R.string.gpsDataOff));
            actvGpsTime.setText(getString(R.string.gpsTimeOff));
         }
      }
   } // end of updateGpsData-method \\

   private void saveQrToSharedPrefs(String qrFromActivityResult) {
      SharedPreferences sharedPreferences = getSharedPreferences(GlobalKeys.S_P_NAME, MODE_PRIVATE);
//      SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
      sharedPreferences.edit().clear().
            putString(GlobalKeys.S_P_QR_KEY, qrFromActivityResult).apply();
      // informing the user about change in qr-code \
      vibrator.vibrate(100);
   }
}