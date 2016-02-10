package com.solveast.gps_tracker.activities;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.solveast.gps_tracker.GlobalKeys;
import com.solveast.gps_tracker.R;
import com.solveast.gps_tracker.service.MuleteerService;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

   private AppCompatTextView actvGpsStatus;
   private AppCompatTextView actvInetStatus;
   private AppCompatTextView actvGpsData;
   private AppCompatTextView actvGpsTime;
   
   private AppCompatButton acbScanQR;
   private SwitchCompat scTrackingStatus;

   private String qrFromSP;
   
   private int whiteColor;
   private int primaryDarkColor;
   private int primaryTextColor;
   private int accentColor;
   
   private LocalBroadcastManager localBroadcastManager;
   private BroadcastReceiver broadcastReceiver;
   
   private LocationManager locationManager;
   private ConnectivityManager connectivityManager;

   Vibrator vibrator;
   
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      
      Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);
      
      actvGpsStatus = (AppCompatTextView) findViewById(R.id.actv_GpsStatus);
      actvInetStatus = (AppCompatTextView) findViewById(R.id.actv_InetStatus);
      actvGpsData = (AppCompatTextView) findViewById(R.id.actv_GpsData);
      actvGpsTime = (AppCompatTextView) findViewById(R.id.actv_GpsTime);
      
      acbScanQR = (AppCompatButton) findViewById(R.id.acb_ScanQR);
      scTrackingStatus = (SwitchCompat) findViewById(R.id.sc_TrackingStatus);

      vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
      scTrackingStatus.setOnCheckedChangeListener(
               new CompoundButton.OnCheckedChangeListener() {
                  
                  @Override
                  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                     // getting some touch feedback about start/stop action \
                     vibrator.vibrate(100);
                     // TODO: 08.02.2016 avoid changing data for null coordinates and time \
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
       and whether service was running successfully in background at the start of activity \
*/
      // TODO: 07.02.2016 find out how to set colors directly from values/colors.xml \
      whiteColor = Color.parseColor("#ffffff");
      primaryDarkColor = Color.parseColor("#388E3C");
      primaryTextColor = Color.parseColor("#212121");
      accentColor = Color.parseColor("#00BCD4");
      
      // 0 = setting QR-code and its view \
      qrFromSP = getSharedPreferences(GlobalKeys.SHARED_PREFERENCES_NAME, MODE_PRIVATE)
               .getString(GlobalKeys.SHARED_PREFERENCES_QR_KEY, "");
//      qrFromSP = getPreferences(MODE_PRIVATE).getString(GlobalKeys.SHARED_PREFERENCES_QR_KEY, "");
      setAcbScanQr();
      
      // 1 = checking if the service has already being running at the start of this activity \
      localBroadcastManager = LocalBroadcastManager.getInstance(this);
      // trying to use the intent that launched this activity - not a new object \
      Intent checkServicestateIntent = getIntent();
      checkServicestateIntent.putExtra(GlobalKeys.START_SERVICE_CHECK, false);
      localBroadcastManager.sendBroadcast(checkServicestateIntent);
      // setting the state of our switch depending on the service on/off result \
      if (checkIfServiceAlive() || isMyServiceRunning(MuleteerService.class))
         setScTrackingStatusOn();
      else setScTrackingStatusOff();
      
      // 2 = checking the state of GPS - only to inform user \
      locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
      isGpsEnabled();
      
      // 3 = checking the state of internet - only to inform user \
      connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
      isInetEnabled();

      // 4 = this check is necessary for correct application relaunch \
      updateGpsData(null);
   } // end of onCreate-method \
   
   // METHODS TO SWITCH STATES OF ALL VIEW ELEMENTS \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
   
   private void setAcbScanQr() {
      
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
//         actv_QR_Status.setText(qrFromSP);
      }
   }
   
   // TODO: 09.02.2016 find out why local broadcast is not working \
   private boolean checkIfServiceAlive() { // currently makes no effect - why ???
      // by default we assue service to be dead \
      final boolean[] result = {false};
      
      // preparing listener to receive the answer from the service \
      broadcastReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
            // here received value has to be changed to true if service is OK \
            if (intent.getBooleanExtra(GlobalKeys.START_SERVICE_CHECK, false))
               result[0] = true;
         }
      };
      IntentFilter intentFilter = new IntentFilter(GlobalKeys.LOCAL_BROADCAST_SERVICE_CHECK);
      localBroadcastManager.registerReceiver(broadcastReceiver, intentFilter);

      return result[0];
   }
   
   // crazy simple magic method - it finds my service among others \
   private boolean isMyServiceRunning(Class<?> serviceClass) {
      ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
      for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
         if (serviceClass.getName().equals(service.service.getClassName())) {
            return true;
         }
      }
      return false;
   }
   
   @Override
   protected void onStop() {
      super.onStop();
      localBroadcastManager.unregisterReceiver(broadcastReceiver);
   }
   
   private void setScTrackingStatusOn() {
      scTrackingStatus.setText(getString(R.string.textForTrackingSwitchedOn));
      scTrackingStatus.setTextColor(primaryDarkColor);
      scTrackingStatus.setBackgroundResource(R.drawable.my_rounded_button_shape);
      scTrackingStatus.setChecked(true);
   }
   
   private void setScTrackingStatusOff() {
      scTrackingStatus.setText(getString(R.string.textForTrackingSwitchedOff));
      scTrackingStatus.setTextColor(whiteColor);
      scTrackingStatus.setBackgroundResource(R.drawable.my_rounded_button_shape_dark);
      scTrackingStatus.setChecked(false);
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
         Log.d("locationManager", "is null");
         return false;
      }
   }
   
   // TODO: 08.02.2016 is it possible to simplify these constructions - and how ? \
   
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
         Log.d("connectivityManager", "is null");
         return false;
      }
   }
   
   // MAIN SET OF METHODS \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
   
   public void startTracking() {
      PendingIntent pendingIntent = createPendingResult(1, new Intent(), 0);
      Intent intentServiceGps = new Intent(this, MuleteerService.class);
      intentServiceGps.putExtra(GlobalKeys.QR_KEY, qrFromSP);
      intentServiceGps.putExtra(GlobalKeys.PENDING_INTENT_KEY, pendingIntent);
      
      startService(intentServiceGps);
      setScTrackingStatusOn();
   }
   
   public void stopTracking() {
      stopService(new Intent(this, MuleteerService.class));
      setScTrackingStatusOff();
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
      
      Log.v("onActivityResult", "requestCode: " + String.valueOf(requestCode));
      Log.v("onActivityResult", "resultCode: " + String.valueOf(resultCode));
      
      // recognizing what has come by contents of resultCode \
      switch (resultCode) {
         // result from QrActivity \
         case GlobalKeys.QR_ACTIVITY_KEY: {
            Log.v("resultCode", "GlobalKeys.QR_ACTIVITY_KEY");
            if (data != null) {
               Log.v("onActivityResult", "data != null");
               String newQrCode = data.getStringExtra(GlobalKeys.EXTRA_QR_RESULT);
               // if a new code is acquired \
               if (!qrFromSP.equals(newQrCode)) {
                  // updating our QR-code for the next scan \
                  qrFromSP = newQrCode;
                  // fixing the changes \
                  saveQrToSharedPrefs(newQrCode);
                  // changing the view of scanning button \
                  setAcbScanQr();
                  // location service needs to be stopped after new code is taken - to avoid wrong data \
                  stopTracking();
                  // setting my own toast \
                  Toast toast = Toast.makeText(this, getString(R.string.newQR_CodeIsSet), Toast.LENGTH_SHORT);
/* // java.lang.RuntimeException: This Toast was not created with Toast.makeText()
                  Toast toast = new Toast(this);
                  toast.setDuration(Toast.LENGTH_SHORT);
                  toast.setGravity(Gravity.BOTTOM, 0, 24);
                  View toastView = View.inflate(this, R.layout.my_toast, null);
                  AppCompatTextView toastText = (AppCompatTextView) findViewById(R.id.actv_MyToast);
                  toastText.setText(getString(R.string.newQR_CodeIsSet));
                  toast.setView(toastView);
                  toast.setText(getString(R.string.newQR_CodeIsSet));
*/
                  toast.show();
                  // TODO: 08.02.2016 learn to set my own style of Toast with my own text \
               } else
                  Toast.makeText(this, getString(R.string.oldQR_CodeIsKept), Toast.LENGTH_SHORT).show();
               // the only point to enable start of the tracking \
               scTrackingStatus.setVisibility(View.VISIBLE);
            } else {
               Log.v("onActivityResult", "data is null");
            }
            break;
         }
         // result about GPS from service - incoming intent available \
         case GlobalKeys.P_I_CODE_DATA_FROM_GPS: {
            Log.v("resultCode", "GlobalKeys.P_I_CODE_DATA_FROM_GPS");
            updateGpsData(data); // data from GPS is obtained and the service is running \
            break;
         }
         // result about the state of connection from service - just to update \
         case GlobalKeys.P_I_CONNECTION_OFF:
         case GlobalKeys.P_I_CONNECTION_ON: {
            Log.v("resultCode", "GlobalKeys.P_I_CONNECTION_ON/OFF");
            isInetEnabled();
            // fixing the bug when inet status updated but GPS - not \
            isGpsEnabled();
            break;
         }
      } // end of switch-statement \\
   } // end of onActivityResult-method \\
   
   private void updateGpsData(Intent data) {
      double latitude = 0.0, longitude = 0.0;
      long timeOfTakingCoordinates = 0;
      if (data != null) {
         latitude = data.getDoubleExtra(GlobalKeys.GPS_LATITUDE, 0.0);
         longitude = data.getDoubleExtra(GlobalKeys.GPS_LONGITUDE, 0.0);
         timeOfTakingCoordinates = data.getLongExtra(GlobalKeys.GPS_TAKING_TIME, 0);
      }
      String coordinates = String.valueOf("Lat. " + latitude + " / Long. " + longitude);
      actvGpsData.setText(coordinates);
      if (latitude != 0.0 && longitude != 0.0)
         actvGpsData.setTextColor(accentColor);
      else {
         actvGpsData.setTextColor(primaryTextColor);
         if (isMyServiceRunning(MuleteerService.class))
            actvGpsData.setText(getString(R.string.gpsDataOn));
         else actvGpsData.setText(getString(R.string.gpsDataOff));
      }

      // TODO: 09.02.2016 optimize these two statements to a faster one \

      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(timeOfTakingCoordinates);
      String stringTime = calendar.getTime().toString();
      actvGpsTime.setText(String.valueOf("Time: " + stringTime));
      if (timeOfTakingCoordinates != 0)
         actvGpsTime.setTextColor(accentColor);
      else {
         actvGpsTime.setTextColor(primaryTextColor);
         if (isMyServiceRunning(MuleteerService.class))
            actvGpsTime.setText(getString(R.string.gpsTimeOn));
         else actvGpsTime.setText(getString(R.string.gpsTimeOff));
      }
   } // end of updateGpsData-method \\
   
   private void saveQrToSharedPrefs(String qrFromActivityResult) {
      SharedPreferences sharedPreferences = getSharedPreferences(GlobalKeys.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
//      SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
      sharedPreferences.edit().clear().
               putString(GlobalKeys.SHARED_PREFERENCES_QR_KEY, qrFromActivityResult).apply();
      // informing the user about change in qr-code \
      vibrator.vibrate(100);
   }
}