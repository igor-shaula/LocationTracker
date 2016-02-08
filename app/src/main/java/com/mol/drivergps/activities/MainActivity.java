package com.mol.drivergps.activities;

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

import com.mol.drivergps.GlobalKeys;
import com.mol.drivergps.R;
import com.mol.drivergps.service.MyService;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

   private final String SHARED_PREFERENCES_QR_KEY = "shared preferences key for QR-code";

   private AppCompatTextView actv_GpsStatus;
   private AppCompatTextView actv_InetStatus;
   private AppCompatTextView actv_GpsData;
   private AppCompatTextView actv_GpsTime;

   private AppCompatButton acb_ScanQR;
   private SwitchCompat sc_TrackingStatus;

   private boolean qr_OK, gps_OK, inet_OK;
   private boolean trackingLaunched = false;

   private String qrFromSP;

   private int myWhiteColor;
   private int primaryDarkColor;
   private int primaryTextColor;
   private int accentColor;

   private LocalBroadcastManager localBroadcastManager;
   private BroadcastReceiver broadcastReceiver;

   private LocationManager locationManager;
   private ConnectivityManager connectivityManager;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);

//      actv_QR_Status = (AppCompatTextView) findViewById(R.id.actv_QR_Status);
      actv_GpsStatus = (AppCompatTextView) findViewById(R.id.actv_GpsStatus);
      actv_InetStatus = (AppCompatTextView) findViewById(R.id.actv_InetStatus);
      actv_GpsData = (AppCompatTextView) findViewById(R.id.actv_GpsData);
      actv_GpsTime = (AppCompatTextView) findViewById(R.id.actv_GpsTime);

      acb_ScanQR = (AppCompatButton) findViewById(R.id.acb_ScanQR);
      sc_TrackingStatus = (SwitchCompat) findViewById(R.id.sc_TrackingStatus);

      sc_TrackingStatus.setOnCheckedChangeListener(
               new CompoundButton.OnCheckedChangeListener() {

                  @Override
                  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                     set_actv_GpsStatus();
                     set_actv_InetStatus();
                     // TODO: 08.02.2016 avoid changing data for null coordinates and time \
                     if (isChecked) {
                        startTracking();
                        if (gps_OK && inet_OK) {
                           trackingLaunched = true;
                           actv_GpsData.setText(getString(R.string.coordinatesOn));
                           actv_GpsTime.setText(getString(R.string.nanoTimeOn));
                        }
                     } else {
                        stopTracking();
                        trackingLaunched = false;
                        actv_GpsData.setText(getString(R.string.coordinatesOff));
                        actv_GpsTime.setText(getString(R.string.nanoTimeOff));
                     }
                     update_GPS_Data(null);
                  }
               }
      );
/*
       setting the initial state of the buttons depending on QR availlability
       and whether service was running successfully in background at the start of activity \
*/
      // TODO: 07.02.2016 figure out how to set colors directly from values/colors.xml \
      myWhiteColor = Color.parseColor("#ffffff");
      primaryDarkColor = Color.parseColor("#388E3C");
      primaryTextColor = Color.parseColor("#212121");
      accentColor = Color.parseColor("#00BCD4");

      // 0 = setting QR-code and its view \
      qrFromSP = getPreferences(MODE_PRIVATE).getString(SHARED_PREFERENCES_QR_KEY, "");
      //      qrFromSP = loadQrFromDb();
      set_acb_ScanQR();

      // 1 = checking if the service has already being running at the start of this activity \
      localBroadcastManager = LocalBroadcastManager.getInstance(this);
      // trying to use the intent that launched this activity - not a new object \
      Intent checkServicestateIntent = getIntent();
      checkServicestateIntent.putExtra(GlobalKeys.START_SERVICE_CHECK, false);
      localBroadcastManager.sendBroadcast(checkServicestateIntent);
      // setting the state of our switch depending on the service on/off result \
      if (checkIfServiceAlive() || isMyServiceRunning(MyService.class))
         set_scTrackingStatus_ON();
      else set_scTrackingStatus_OFF();

      // 2 = checking the state of GPS - only to inform user \
      locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
      set_actv_GpsStatus();

      // 3 = checking the state of internet - only to inform user \
      connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
      set_actv_InetStatus();
   } // end of onCreate-method \

   // METHODS TO SWITCH STATES OF ALL VIEW ELEMENTS \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

   private void set_acb_ScanQR() {

      // setting appearance optimal to show user the state of data \
      if (!qrFromSP.equals("")) {
         qr_OK = true;
         acb_ScanQR.setText(getString(R.string.textForPresentScan));
         acb_ScanQR.setTextColor(primaryDarkColor);
         acb_ScanQR.setBackgroundResource(R.drawable.my_rounded_button_shape);
         sc_TrackingStatus.setVisibility(View.VISIBLE);
      } else {
         qr_OK = false;
         acb_ScanQR.setText(getString(R.string.textForNewScan));
         acb_ScanQR.setTextColor(myWhiteColor);
         acb_ScanQR.setBackgroundResource(R.drawable.my_rounded_button_shape_dark);
         sc_TrackingStatus.setVisibility(View.INVISIBLE);
//         actv_QR_Status.setText(qrFromSP);
      }
   }

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

   private void set_scTrackingStatus_ON() {
      sc_TrackingStatus.setText(getString(R.string.textForTrackingSwitchedOn));
      sc_TrackingStatus.setTextColor(primaryDarkColor);
      sc_TrackingStatus.setBackgroundResource(R.drawable.my_rounded_button_shape);
      sc_TrackingStatus.setChecked(true);
   }

   private void set_scTrackingStatus_OFF() {
      sc_TrackingStatus.setText(getString(R.string.textForTrackingSwitchedOff));
      sc_TrackingStatus.setTextColor(myWhiteColor);
      sc_TrackingStatus.setBackgroundResource(R.drawable.my_rounded_button_shape_dark);
      sc_TrackingStatus.setChecked(false);
   }

   private void set_actv_GpsStatus() {
      if (locationManager != null) {
         if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gps_OK = true;
            actv_GpsStatus.setText(getString(R.string.gpsStatusEnabled));
            actv_GpsStatus.setTextColor(primaryDarkColor);
         } else {
            gps_OK = false;
            actv_GpsStatus.setText(getString(R.string.gpsStatusDisabled));
            actv_GpsStatus.setTextColor(primaryTextColor);
         }
         Log.d("locationManager", locationManager.toString());
      } else {
         gps_OK = false;
         actv_GpsStatus.setText(getString(R.string.gpsStatusDisabled));
         actv_GpsStatus.setTextColor(primaryTextColor);
         Log.d("locationManager", "is null");
      }
   }

   // TODO: 08.02.2016 is it possible to simplify this construction - and how ? \
   public void set_actv_InetStatus() {
      NetworkInfo networkInfo = null;
      if (connectivityManager != null)
         networkInfo = connectivityManager.getActiveNetworkInfo();
      if (networkInfo != null) {
         if (networkInfo.isConnected()) {
            inet_OK = true;
            actv_InetStatus.setText(getString(R.string.internetIsOn));
            actv_InetStatus.setTextColor(primaryDarkColor);
         } else {
            inet_OK = false;
            actv_InetStatus.setText(getString(R.string.internetIsOff));
            actv_InetStatus.setTextColor(primaryTextColor);
         }
         Log.d("connectivityManager", connectivityManager.toString());
      } else {
         actv_InetStatus.setText(getString(R.string.internetIsOff));
         actv_InetStatus.setTextColor(primaryTextColor);
         Log.d("connectivityManager", "is null");
      }
   }

   // MAIN SET OF METHODS \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

   public void startTracking() {
/*
      if (loadQrFromDb().equals("")) {
         actv_QR_Status.setText(SCAN_CODE);
         Log.d("startTracking", "QR-code is absent!");
      } else {
         Log.d("startTracking", "QR-code is present...");
*/
      PendingIntent pendingIntent = createPendingResult(1, new Intent(), 0);

      Intent intentServiceGps = new Intent(this, MyService.class);
      intentServiceGps.putExtra(GlobalKeys.QR_KEY, qrFromSP);
      intentServiceGps.putExtra(GlobalKeys.PENDING_INTENT_KEY, pendingIntent);

      startService(intentServiceGps);

      set_scTrackingStatus_ON();

//      actv_GpsStatus.setText(getString(R.string.gpsServiceLaunched));
//      }
   }

   public void stopTracking() {

      stopService(new Intent(this, MyService.class));

      set_scTrackingStatus_OFF();

//      actv_GpsStatus.setText(getString(R.string.gpsServiceStopped));
   }

   // button pressed = get QR code \
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

//      if (resultCode == RESULT_OK) {
/*      if (requestCode == CODE_FOR_QR_ACTIVITY) {
         Log.v("onActivityResult", "onActivityResult in requestCode == CODE_FOR_QR_ACTIVITY");
      }*/
      // recognizing what has come by contents of resultCode \
      switch (resultCode) {
//         switch (requestCode) {
         case GlobalKeys.QR_ACTIVITY_KEY: {
//         case RESULT_OK: {
            Log.v("onActivityResult", "resultCode == RESULT_OK");
            if (data != null) {
               Log.v("onActivityResult", "data != null");
               saveQR_ToSP(data.getStringExtra(GlobalKeys.EXTRA_QR_RESULT));
//            saveToTheDb(data.getStringExtra(GlobalKeys.EXTRA_QR_RESULT));
//                  actv_QR_Status.setText(data.getStringExtra(GlobalKeys.EXTRA_QR_RESULT));
               // the only point to enable start of the tracking \
               sc_TrackingStatus.setVisibility(View.VISIBLE);
            } else {
               Log.v("onActivityResult", "data is null");
//                  actv_QR_Status.setText(SCAN_CODE);
            }
            break;
         }
         case GlobalKeys.P_I_CODE_DATA_FROM_GPS: { // -100
//         case GlobalKeys.P_I_LOCATION_CHANGED: { // 102
            // getting data from service \
            update_GPS_Data(data); // data from GPS is obtained and the service is running \

            break;
         }
//         case GlobalKeys.P_I_CODE_CONNECTION_OK: { // -200
         // data from GPS is sent successfully to the server \
//            break;
//         }
//         case GlobalKeys.P_I_PROVIDER_DISABLED: // 100
//         case GlobalKeys.P_I_PROVIDER_ENABLED: { // 101
//            set_actv_GpsStatus();
//            break;
//         }
//         case GlobalKeys.P_I_STATUS_CHANGED: { // 103
//            break;
//         }
         case GlobalKeys.P_I_CONNECTION_OFF: // 200
         case GlobalKeys.P_I_CONNECTION_ON: { // 201
            set_actv_InetStatus();
            break;
         }
         default: {
         }
      } // end of switch-statement \\
   } // end of onActivityResult-method \\

   private void update_GPS_Data(Intent data) {
      double latitude = 0.0, longitude = 0.0;
      long timeOfTakingCoordinates = 0;
      if (data != null) {
         latitude = data.getDoubleExtra(GlobalKeys.GPS_LATITUDE, 0.0);
         longitude = data.getDoubleExtra(GlobalKeys.GPS_LONGITUDE, 0.0);
         timeOfTakingCoordinates = data.getLongExtra(GlobalKeys.GPS_TAKING_TIME, 0);
      }
      String coordinates = String.valueOf("Lat. " + latitude + " / Long. " + longitude);
      actv_GpsData.setText(coordinates);
      if (latitude != 0.0 && longitude != 0.0)
         actv_GpsData.setTextColor(accentColor);
      else actv_GpsData.setTextColor(primaryTextColor);

      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(timeOfTakingCoordinates);
      String stringTime = calendar.getTime().toString();
      actv_GpsTime.setText(String.valueOf("Time: " + stringTime));
      if (timeOfTakingCoordinates != 0)
         actv_GpsTime.setTextColor(accentColor);
      else actv_GpsTime.setTextColor(primaryTextColor);
   }

   private void saveQR_ToSP(String qrFromActivityResult) {
      SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
      sharedPreferences.edit().clear().putString(SHARED_PREFERENCES_QR_KEY, qrFromActivityResult).apply();
      // setting my own toast \
      Toast toast = Toast.makeText(this, getString(R.string.newQR_CodeIsSet), Toast.LENGTH_SHORT);
//      Toast toast = new Toast(this);
//      toast.setDuration(Toast.LENGTH_SHORT);
//      toast.setGravity(Gravity.BOTTOM, 0, 24);
//      View toastView = View.inflate(this, R.layout.my_toast, null);
//      AppCompatTextView toastText = (AppCompatTextView) findViewById(R.id.actv_MyToast);
//      toastText.setText(getString(R.string.newQR_CodeIsSet));
//      toast.setView(toastView);
//      toast.setText(getString(R.string.newQR_CodeIsSet)); // java.lang.RuntimeException: This Toast was not created with Toast.makeText()
      toast.show();
      // TODO: 08.02.2016 learn to set my own style of Toast with my own text \
   }
}