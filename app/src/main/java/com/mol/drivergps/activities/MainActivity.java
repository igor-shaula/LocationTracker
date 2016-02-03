package com.mol.drivergps.activities;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.View;
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

   private AppCompatTextView actv_QR_Result;
//   private AppCompatTextView actv_GpsData;
//   private AppCompatTextView actv_GpsTime;
   private AppCompatTextView actv_GpsStatus;

   public final int TASK1_CODE = 1;
   public static final String TAG = "MainActivity";

   //   public static final String SAVED_QR = "qr_saved";
   public static final String TRACKING_STARTED = "Tracking...";
   public static final String TRACKING_STOPED = "Stoped tracking";
   public static final String SCAN_CODE = "Please, scan QR code";
   public static final String CODE_PRESENT = "Code is present";

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      actv_QR_Result = (AppCompatTextView) findViewById(R.id.actv_QR_Result);
//      actv_GpsData = (AppCompatTextView) findViewById(R.id.actv_GpsData);
//      actv_GpsTime = (AppCompatTextView) findViewById(R.id.actv_GpsTime);
      actv_GpsStatus = (AppCompatTextView) findViewById(R.id.actv_GpsStatus);
   }

   // button pressed = get QR code \ OK \
   public void qrCodeReading(View view) {
      Intent intent = new Intent(MainActivity.this, QrActivity.class);
      startActivityForResult(intent, GlobalKeys.CODE_FOR_QR_ACTIVITY_SCANNER);
   }

   // button pressed = show QR code \
   public void showQr(View view) {
      Toast.makeText(this, loadQrFromDb(), Toast.LENGTH_SHORT).show();
   }

   // returning point to this activity \
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);

      Log.v(TAG, "requestCode: " + String.valueOf(requestCode));
      Log.v(TAG, "resultCode: " + String.valueOf(resultCode));

      if (requestCode == GlobalKeys.CODE_FOR_QR_ACTIVITY_SCANNER) { // requestCode = 1

         Log.v(TAG, "onActivityResult in requestCode == CODE_FOR_QR_ACTIVITY_SCANNER");
      }

      if (resultCode == RESULT_OK) {
         if (data != null) {
            saveToTheDb(data.getStringExtra(GlobalKeys.EXTRA_QR_RESULT));
            actv_QR_Result.setText(data.getStringExtra(GlobalKeys.QR_SCAN_RESULT));

            Log.v(TAG, "onActivityResult in requestCode == INTENT_CODE_GPS");
         } else {
            actv_QR_Result.setText(SCAN_CODE);
         }
      }
   }

   // button pressed = start tracking \
   public void startTracking(View view) {

      if (loadQrFromDb().equals("")) {
         actv_QR_Result.setText(SCAN_CODE);
         Toast.makeText(this, SCAN_CODE, Toast.LENGTH_SHORT).show();
      } else {
         Toast.makeText(this, CODE_PRESENT, Toast.LENGTH_SHORT).show();

         Intent intentForTest = new Intent();
         PendingIntent pendingIntent = createPendingResult(TASK1_CODE, intentForTest, 0);

         Intent intentServiceGps = new Intent(this, MyService.class);
         intentForTest.putExtra(GlobalKeys.PARAM_PINTENT, pendingIntent);

         actv_GpsStatus.setText(TRACKING_STARTED);

         startService(intentServiceGps);
      }
   }

   // button pressed = stop tracking \
   public void stopTracking(View view) {
      actv_GpsStatus.setText(TRACKING_STOPED);
      stopService(new Intent(this, MyService.class));
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

   }

// working with database \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

   private void saveToTheDb(String qr) {
      DriverData driverData = new DriverData();
      driverData.setQr(qr);
      HelpFactory.getDatabaseHelper().getDriverDao().addNewDriverData(driverData);
      Log.d("saveToTheDb", "worked");
   }

   private String loadQrFromDb() {
      DriverData driverData = HelpFactory.getDatabaseHelper().getDriverDao().getDriverData();
      if (driverData == null) {
         Log.d("loadQrFromDb", "driverData is null");
         return "";
      }
      return HelpFactory.getDatabaseHelper().getDriverDao().getDriverData().getQr();
   }
}