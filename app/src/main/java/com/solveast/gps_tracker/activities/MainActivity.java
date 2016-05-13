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
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.solveast.gps_tracker.GlobalKeys;
import com.solveast.gps_tracker.MyLog;
import com.solveast.gps_tracker.R;
import com.solveast.gps_tracker.entity.ContinuousMode;
import com.solveast.gps_tracker.service.MainService;

import java.io.IOException;
import java.util.Calendar;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

   private AppCompatButton acbScanQR, acbSetContinuous;
   private SwitchCompat scTrackingStatus;
   private AppCompatTextView actvGpsStatus;
   private AppCompatTextView actvInetStatus;
   private AppCompatTextView actvGpsData;
   private AppCompatTextView actvGpsTime;
   private AppCompatTextView actvDistance;

   private String mQrFromSP;
   private int mWhiteColor;
   private int mPrimaryDarkColor;
   private int mPrimaryTextColor;
   private int mAccentColor;
   private boolean mTrackingIsOn = false;

   private Vibrator mVibrator;
   private LocationManager mLocationManager;
   private ConnectivityManager mConnectivityManager;

// LIFECYCLE =======================================================================================

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
      actvDistance = (AppCompatTextView) findViewById(R.id.actvDistance);

      mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

      scTrackingStatus.setOnCheckedChangeListener(
               new CompoundButton.OnCheckedChangeListener() {

                  @Override
                  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                     // getting some touch feedback about start/stop action \
                     if (isChecked) {
                        if (isGpsEnabled()) {
//                        if (isGpsEnabled() && isInetEnabled()) {
                           startTracking();
                           actvGpsData.setText(getString(R.string.gpsDataOn));
                           actvGpsTime.setText(getString(R.string.gpsTimeOn));
                        } else {
                           showSystemScreenForGps();
                           setTrackingSwitchStatus(false);
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

      acbSetContinuous = (AppCompatButton) findViewById(R.id.acb_SetContinuous);
      acbSetContinuous.setOnLongClickListener(new View.OnLongClickListener() {
         @Override
         public boolean onLongClick(View v) {
            // here we just updating visual state of the button \
            if (!v.isActivated()) {
               MyLog.i("onLongClick \\ state = activated");
               acbSetContinuous.setText(getString(R.string.textForContinuousOn));
               acbSetContinuous.setTextColor(mPrimaryDarkColor);
               acbSetContinuous.setBackgroundResource(R.drawable.my_rounded_button_shape);
               // making next switching state available \
               acbSetContinuous.setActivated(true);
            } else {
               MyLog.i("onLongClick \\ state = not activated");
               acbSetContinuous.setText(getString(R.string.textForContinuousOff));
               acbSetContinuous.setTextColor(mWhiteColor);
               acbSetContinuous.setBackgroundResource(R.drawable.my_rounded_button_shape_dark);
               // making next switching state available \
               acbSetContinuous.setActivated(false);
            }
            // here network job is done along with data preparations \
            switchContinuousMode();
            return false;
         }
      });
/*
       setting the initial state of the buttons depending on QR availlability
       and whether service running state in background at the start of activity \
*/
      mWhiteColor = ContextCompat.getColor(this, android.R.color.white);
      mPrimaryDarkColor = ContextCompat.getColor(this, R.color.primary_dark);
      mPrimaryTextColor = ContextCompat.getColor(this, R.color.primary_text);
      mAccentColor = ContextCompat.getColor(this, R.color.accent);

      // 0 = setting QR-code and its view \
      mQrFromSP = getSharedPreferences(GlobalKeys.S_P_NAME, MODE_PRIVATE)
               .getString(GlobalKeys.S_P_QR_KEY, "");
      MyLog.v("getSharedPreferences: " + mQrFromSP);
//      mQrFromSP = getPreferences(MODE_PRIVATE).getString(GlobalKeys.S_P_QR_KEY, "");
      setScanQrButtonStatus();

      // 1 = checking if the service has already being running at the start of this activity \
      if (isMyServiceRunning(MainService.class)) mTrackingIsOn = true;
      setTrackingSwitchStatus(mTrackingIsOn);

      // 2 = checking the state of GPS - only to inform user \
      mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
      isGpsEnabled();

      // 3 = checking the state of internet - only to inform user \
      mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
      isInetEnabled();

      // 4 = this check is necessary for correct application relaunch \
      updateGpsData(null);

   } // end of onCreate-method \

// CHECKERS & VIEW STATE SWITCHERS =================================================================

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
      if (!mQrFromSP.equals("")) {
         acbScanQR.setText(getString(R.string.textForPresentScan));
         acbScanQR.setTextColor(mPrimaryDarkColor);
         acbScanQR.setBackgroundResource(R.drawable.my_rounded_button_shape);
         scTrackingStatus.setVisibility(View.VISIBLE);
      } else {
         acbScanQR.setText(getString(R.string.textForNewScan));
         acbScanQR.setTextColor(mWhiteColor);
         acbScanQR.setBackgroundResource(R.drawable.my_rounded_button_shape_dark);
         scTrackingStatus.setVisibility(View.INVISIBLE);
      }
   }

   private void setTrackingSwitchStatus(boolean statusOn) {
      if (statusOn) {
         scTrackingStatus.setText(getString(R.string.textForTrackingSwitchedOn));
         scTrackingStatus.setTextColor(mPrimaryDarkColor);
         scTrackingStatus.setBackgroundResource(R.drawable.my_rounded_button_shape);
         scTrackingStatus.setChecked(true);
         mTrackingIsOn = true;
         // informing user about this \
         mVibrator.vibrate(100);
      } else {
         scTrackingStatus.setText(getString(R.string.textForTrackingSwitchedOff));
         scTrackingStatus.setTextColor(mWhiteColor);
         scTrackingStatus.setBackgroundResource(R.drawable.my_rounded_button_shape_dark);
         scTrackingStatus.setChecked(false);
         mTrackingIsOn = false;
      }
   }

   private boolean isGpsEnabled() { // also changes appearance of GPS text view \
      if (mLocationManager != null) {
         if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            actvGpsStatus.setText(getString(R.string.gpsEnabled));
            actvGpsStatus.setTextColor(mPrimaryDarkColor);
            return true;
         } else {
            actvGpsStatus.setText(getString(R.string.gpsDisabled));
            actvGpsStatus.setTextColor(mPrimaryTextColor);
            return false;
         }
      } else {
         actvGpsStatus.setText(getString(R.string.gpsDisabled));
         actvGpsStatus.setTextColor(mPrimaryTextColor);
         MyLog.v("mLocationManager is null");
         return false;
      }
   }

   public boolean isInetEnabled() { // also changes appearance of inet info view \
      NetworkInfo networkInfo;
      if (mConnectivityManager != null)
         networkInfo = mConnectivityManager.getActiveNetworkInfo();
      else return false;
      if (networkInfo != null) {
         if (networkInfo.isConnected()) {
            actvInetStatus.setText(getString(R.string.inetConnected));
            actvInetStatus.setTextColor(mPrimaryDarkColor);
            return true;
         } else {
            actvInetStatus.setText(getString(R.string.inetDisconnected));
            actvInetStatus.setTextColor(mPrimaryTextColor);
            return false;
         }
      } else {
         actvInetStatus.setText(getString(R.string.inetDisconnected));
         actvInetStatus.setTextColor(mPrimaryTextColor);
         MyLog.v("mConnectivityManager is null");
         return false;
      }
   }

// MAIN SET OF METHODS =============================================================================

   private void showSystemScreenForGps() {
      Toast.makeText(MainActivity.this, "Please switch the GPS on", Toast.LENGTH_SHORT).show();
      // opening window with system settings for GPS \
      startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
   }

   public void startTracking() {
      // preparing intent for qr-code sending service \
      PendingIntent pendingIntent = createPendingResult(1, new Intent(), 0);
      Intent intentServiceGps = new Intent(this, MainService.class);
      intentServiceGps.putExtra(GlobalKeys.QR_KEY, mQrFromSP);
      intentServiceGps.putExtra(GlobalKeys.P_I_KEY, pendingIntent);

      startService(intentServiceGps);
      setTrackingSwitchStatus(true);
   }

   public void stopTracking() {
      // here we have to switch service off completely \
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
               if (!mQrFromSP.equals(newQrCode)) {
                  // updating our QR-code for the next scan \
                  mQrFromSP = newQrCode;
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
            if (mTrackingIsOn)
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
            if (mQrFromSP.equals(invalidQR)) mQrFromSP = "";
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

         int distance = data.getIntExtra(GlobalKeys.DISTANCE, 0);
         actvDistance.setText(String.valueOf("Passed distance: " + distance + " m"));

         actvGpsData.setTextColor(mAccentColor);
         actvGpsTime.setTextColor(mAccentColor);
      } else {
         // data is absent - we have nothing to show \
         actvGpsData.setTextColor(mPrimaryTextColor);
         actvGpsTime.setTextColor(mPrimaryTextColor);

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
      mVibrator.vibrate(100);
   }

   // my OkHTTP usage to send tracking data to the server - Retrofit didn't work \
   private void switchContinuousMode() {
      MyLog.v("sendInfoToServer = started for continuous mode");

      // 1 - determining id from QR-code \
      int id;
      int counter = mQrFromSP.length();
      for (int i = counter - 1; i > 0; i--)
         if (mQrFromSP.charAt(i) == '-') {
            counter = i + 1;
            break;
         }
      id = Integer.decode(mQrFromSP.substring(counter, mQrFromSP.length()));
      MyLog.i("id from QR = " + id);

      // 2 - retreiving time of switching state \
      long switchingTime = System.currentTimeMillis();
      MyLog.i("switchingTime = " + switchingTime);

      // 3 - setting the state of switcher \
      String state = acbSetContinuous.isActivated() ? "ON" : "Off";

      // 4 - now creating object to send to the server \
      ContinuousMode continuousMode = new ContinuousMode(id, switchingTime, state);
      // currently speed will not be transmitted to the server \

      Gson gson = new GsonBuilder().create();

      String jsonToSend = gson.toJson(continuousMode, ContinuousMode.class);
      MyLog.v("jsonToSend: " + jsonToSend);

      MediaType JSON = MediaType.parse("application/json; charset=utf-8");

      RequestBody body = RequestBody.create(JSON, jsonToSend);

      try {
         Request request = new Request.Builder()
                  .url("http://muleteer.herokuapp.com/admin/period/mule-" + id)
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
         Toast.makeText(MainActivity.this, "IllegalArgumentException", Toast.LENGTH_SHORT).show();
      }
   } // end of sendInfoToServer-method \\
}