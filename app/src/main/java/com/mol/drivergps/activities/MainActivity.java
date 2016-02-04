package com.mol.drivergps.activities;

import android.app.PendingIntent;
import android.content.Intent;
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
import com.mol.drivergps.db.HelpFactory;
import com.mol.drivergps.entity_description.DriverData;
import com.mol.drivergps.rest_connection.MyRetrofitInterface;
import com.mol.drivergps.rest_connection.MyServiceGenerator;
import com.mol.drivergps.service.MyService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

   public final int TASK_TRACKING_REQUEST_CODE = 1;
   public static final int CODE_FOR_QR_ACTIVITY = 2;

   //   public static final String SAVED_QR = "qr_saved";
   public static final String GPS_STARTED = "GPS-tracking started";
   public static final String GPS_STOPPED = "GPS-tracking stopped";
   public static final String SCAN_CODE = "Please, at first scan QR code";
   public static final String QR_IS_SAVED = "...and saved!";

   public static final String TRACKING_SWITCHED_ON = "Tracking launched\nTouch again to stop it";
   public static final String TRACKING_SWITCHED_OFF = "Tracking stopped\nTouch again to start it";

   private AppCompatTextView actv_QR_Result;
   private AppCompatTextView actv_GpsStatus;

   private AppCompatButton acb_ScanQR;
   private SwitchCompat sc_TrackingStatus;

   private String qrFromDB;

   private int textColorDefault;
   private int textColorChanged;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);

      actv_QR_Result = (AppCompatTextView) findViewById(R.id.actv_QR_Status);
      actv_GpsStatus = (AppCompatTextView) findViewById(R.id.actv_GpsStatus);

      acb_ScanQR = (AppCompatButton) findViewById(R.id.acb_ScanQR);
      sc_TrackingStatus = (SwitchCompat) findViewById(R.id.sc_TrackingStatus);

      textColorDefault = Color.parseColor("#ffffff");
      textColorChanged = Color.parseColor("#388E3C");

      // TODO: 04.02.2016 receive information about service - is it running in background \

      sc_TrackingStatus.setText(TRACKING_SWITCHED_OFF);

      qrFromDB = loadQrFromDb();
      if (qrFromDB.equals("")) {
         // setting appearance optimal to show user the state of data \
         acb_ScanQR.setTextColor(textColorDefault);
         acb_ScanQR.setBackgroundResource(R.drawable.my_rounded_button_shape_colored);
         sc_TrackingStatus.setVisibility(View.INVISIBLE);
      }
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

      actv_QR_Result.setText(qrFromDB);
   }

   public void startTracking() {

      if (loadQrFromDb().equals("")) {
         actv_QR_Result.setText(SCAN_CODE);
         Log.d("startTracking", "QR-code is absent!");
      } else {

         Log.d("startTracking", "QR-code is present...");

         Intent intentForTest = new Intent();
         PendingIntent pendingIntent = createPendingResult(TASK_TRACKING_REQUEST_CODE, intentForTest, 0);

         Intent intentServiceGps = new Intent(this, MyService.class);
         intentForTest.putExtra(GlobalKeys.PENDING_INTENT_KEY, pendingIntent);

         sc_TrackingStatus.setTextColor(textColorChanged);
         sc_TrackingStatus.setBackgroundResource(R.drawable.my_rounded_button_shape);

         startService(intentServiceGps);

         actv_GpsStatus.setText(GPS_STARTED);
         sc_TrackingStatus.setText(TRACKING_SWITCHED_ON);
      }
   }

   public void stopTracking() {

      sc_TrackingStatus.setTextColor(textColorDefault);
      sc_TrackingStatus.setBackgroundResource(R.drawable.my_rounded_button_shape_colored);

      stopService(new Intent(this, MyService.class));

      actv_GpsStatus.setText(GPS_STOPPED);
      sc_TrackingStatus.setText(TRACKING_SWITCHED_OFF);
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
            saveToTheDb(data.getStringExtra(GlobalKeys.EXTRA_QR_RESULT));
            actv_QR_Result.setText(data.getStringExtra(GlobalKeys.EXTRA_QR_RESULT));
            // the only point to enable start of the tracking \
            sc_TrackingStatus.setVisibility(View.VISIBLE);
         } else {
            Log.v("onActivityResult", "data is null");
            actv_QR_Result.setText(SCAN_CODE);
         }
      }
   }

   private void saveToTheDb(String qrCode) {
      DriverData driverData = new DriverData();
      driverData.setQr(qrCode);
      HelpFactory.getDatabaseHelper().getDriverDao().addNewDriverData(driverData);
      Log.d("saveToTheDb", "worked");
      Toast.makeText(this, QR_IS_SAVED, Toast.LENGTH_SHORT).show();
   }

   private String loadQrFromDb() {
      DriverData driverData = HelpFactory.getDatabaseHelper().getDriverDao().getDriverData();
      if (driverData == null) {
         Log.d("loadQrFromDb", "driverData is null");
         return "";
      }
      acb_ScanQR.setTextColor(textColorChanged);
      acb_ScanQR.setBackgroundResource(R.drawable.my_rounded_button_shape);
      return HelpFactory.getDatabaseHelper().getDriverDao().getDriverData().getQr();
   }

   // my Retrofit usage to send tracking data to the server \
   public void sendInfoToServer(View view) {

      // creating a plug with mock data \
      String qrCode = loadQrFromDb();
      String location = "290047, 290837209";
      String time = "00-00";
      DriverData driverData = new DriverData(qrCode, location, time);

      // using our service class for creation of interface object \
      MyRetrofitInterface myRetrofitInterface = MyServiceGenerator.createService(MyRetrofitInterface.class);

      // preparing the network access object - the call \
      Call<DriverData> driverDataCall = myRetrofitInterface.makeDriverDataCall(driverData);

      // performing the network connection itself \
      driverDataCall.enqueue(new Callback<DriverData>() {

         @Override
         public void onResponse(Response<DriverData> response) {
            Log.d("onResponse", response.toString());
            if (response.isSuccess()) Log.d("onResponse", "is successfull");
            else Log.d("onResponse", "is not successfull");

         }

         @Override
         public void onFailure(Throwable t) {
            Log.d("onFailure", t.getMessage());
         }
      });
   } // end of sendInfoToServer-method \\
}