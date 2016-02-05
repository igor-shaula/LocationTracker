package com.mol.drivergps.activities;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity {
   
   //   private final int TASK_TRACKING_REQUEST_CODE = 1;
   private final int CODE_FOR_QR_ACTIVITY = 12;

   //   public static final String SAVED_QR = "qr_saved";
   private final String GPS_STARTED = "GPS-tracking started";
   private final String GPS_STOPPED = "GPS-tracking stopped";
   private final String SCAN_CODE = "Please, at first scan QR code";
   private final String QR_CODE_IS_TAKEN = "QR-code is taken successfully";
//   private final String QR_IS_SAVED = "...and saved!";

   private final String TRACKING_SWITCHED_ON = "Tracking launched\nTouch again to stop it";
   private final String TRACKING_SWITCHED_OFF = "Tracking stopped\nTouch again to start it";

   private final String SHARED_PREFERENCES_QR_KEY = "shared preferences key for QR-code";

   private AppCompatTextView actv_QR_Status;
   private AppCompatTextView actv_GpsStatus;
   private AppCompatTextView actv_GpsData;
   private AppCompatTextView actv_GpsTime;

   private AppCompatButton acb_ScanQR;
   private SwitchCompat sc_TrackingStatus;

   private String qrFromSP;

   private int textColorDefault;
   private int textColorChanged;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);

      actv_QR_Status = (AppCompatTextView) findViewById(R.id.actv_QR_Status);
      actv_GpsStatus = (AppCompatTextView) findViewById(R.id.actv_GpsStatus);
      actv_GpsData = (AppCompatTextView) findViewById(R.id.actv_GpsData);
      actv_GpsTime = (AppCompatTextView) findViewById(R.id.actv_GpsTime);

      acb_ScanQR = (AppCompatButton) findViewById(R.id.acb_ScanQR);
      sc_TrackingStatus = (SwitchCompat) findViewById(R.id.sc_TrackingStatus);

      textColorDefault = Color.parseColor("#ffffff");
      textColorChanged = Color.parseColor("#388E3C");

      // TODO: 04.02.2016 receive information about service - is it running in background \

      sc_TrackingStatus.setText(TRACKING_SWITCHED_OFF);

//      qrFromSP = loadQrFromDb();
      qrFromSP = readQR_FromSP();

      sc_TrackingStatus.setOnCheckedChangeListener(
               new CompoundButton.OnCheckedChangeListener() {

                  @Override
                  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                     if (isChecked) {
                        startTracking();
                     } else {
                        stopTracking();
                     }
                  }
               }
      );
   } // end of onCreate-method \

   public void startTracking() {

/*
      if (loadQrFromDb().equals("")) {
         actv_QR_Status.setText(SCAN_CODE);
         Log.d("startTracking", "QR-code is absent!");
      } else {
         Log.d("startTracking", "QR-code is present...");
*/

      Intent intentForTest = new Intent();
      PendingIntent pendingIntent = createPendingResult(GlobalKeys.TASK_TRACKING_REQUEST_CODE, intentForTest, 0);

      Intent intentServiceGps = new Intent(this, MyService.class);
      intentServiceGps.putExtra(GlobalKeys.QR_KEY, qrFromSP);
      intentServiceGps.putExtra(GlobalKeys.PENDING_INTENT_KEY, pendingIntent);

      startService(intentServiceGps);

      sc_TrackingStatus.setTextColor(textColorChanged);
      sc_TrackingStatus.setBackgroundResource(R.drawable.my_rounded_button_shape);
      sc_TrackingStatus.setText(TRACKING_SWITCHED_ON);
      actv_GpsStatus.setText(GPS_STARTED);
//      }
   }

   public void stopTracking() {

      stopService(new Intent(this, MyService.class));

      sc_TrackingStatus.setTextColor(textColorDefault);
      sc_TrackingStatus.setBackgroundResource(R.drawable.my_rounded_button_shape_colored);
      sc_TrackingStatus.setText(TRACKING_SWITCHED_OFF);
      actv_GpsStatus.setText(GPS_STOPPED);
   }

   // button pressed = get QR code \
   public void qrCodeReading(View view) {
      Intent intent = new Intent(MainActivity.this, QrActivity.class);
      startActivityForResult(intent, CODE_FOR_QR_ACTIVITY);
   }

   // returning point to this activity \
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);

      Log.v("onActivityResult", "requestCode: " + String.valueOf(requestCode));
      Log.v("onActivityResult", "resultCode: " + String.valueOf(resultCode));

      if (requestCode == CODE_FOR_QR_ACTIVITY) {
         Log.v("onActivityResult", "onActivityResult in requestCode == CODE_FOR_QR_ACTIVITY");
      }

      if (resultCode == RESULT_OK) {
         Log.v("onActivityResult", "resultCode == RESULT_OK");
         if (data != null) {
            Log.v("onActivityResult", "data != null");
            saveQR_ToSP(data.getStringExtra(GlobalKeys.EXTRA_QR_RESULT));
//            saveToTheDb(data.getStringExtra(GlobalKeys.EXTRA_QR_RESULT));
            actv_QR_Status.setText(data.getStringExtra(GlobalKeys.EXTRA_QR_RESULT));
            // the only point to enable start of the tracking \
            sc_TrackingStatus.setVisibility(View.VISIBLE);
         } else {
            Log.v("onActivityResult", "data is null");
            actv_QR_Status.setText(SCAN_CODE);
         }
      }

      // TODO: 05.02.2016 continue here \
/*
      // getting data from service \
      PendingIntent pendingIntent = data.getStringExtra(GlobalKeys.P_I_CODE_GPS_DATA);
      actv_GpsData.setText(pendingIntent.);
*/

   }

   private void saveQR_ToSP(String qrFromActivityResult) {
      SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
      sharedPreferences.edit().clear().putString(SHARED_PREFERENCES_QR_KEY, qrFromActivityResult).apply();
      Toast.makeText(this, QR_CODE_IS_TAKEN, Toast.LENGTH_SHORT).show();
   }

   private String readQR_FromSP() {
      String readQR_FromSP = getPreferences(MODE_PRIVATE).getString(SHARED_PREFERENCES_QR_KEY, "");

      // setting appearance optimal to show user the state of data \
      if (readQR_FromSP.equals("")) {
         acb_ScanQR.setTextColor(textColorDefault);
         acb_ScanQR.setBackgroundResource(R.drawable.my_rounded_button_shape_colored);
         sc_TrackingStatus.setVisibility(View.INVISIBLE);
      } else {
         acb_ScanQR.setTextColor(textColorChanged);
         acb_ScanQR.setBackgroundResource(R.drawable.my_rounded_button_shape);
         actv_QR_Status.setText(readQR_FromSP);
      }
      return readQR_FromSP;
   }
}