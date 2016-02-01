package com.mol.drivergps.activities;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mol.drivergps.GlobalKeys;
import com.mol.drivergps.R;
import com.mol.drivergps.db.HelpFactory;
import com.mol.drivergps.entity_description.Driver;
import com.mol.drivergps.service.MyService;

public class MainActivity extends AppCompatActivity {

   private TextView tvLocationGPS;
   private TextView tvGpsTime;
   private TextView status;

   public final int TASK1_CODE = 1;

   public static final String SAVED_QR = "qr_saved";
   public static final String STATUS_START = "Tracking...";
   public static final String STATUS_STOP = "Stoped tracking";
   public static final String SCAN_CODE = "Please, scan QR code";
   public static final String CODE_PRESENT = "Code is present";

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      tvLocationGPS = (TextView) findViewById(R.id.tvGpsData);
      tvGpsTime = (TextView) findViewById(R.id.tvGpsTime);
      status = (TextView) findViewById(R.id.tvStatus);

   }

   // first button pressed = get QR code \ OK \
   public void qrCodeReading(View view) {
      Intent intent = new Intent(MainActivity.this, QrActivity.class);
      startActivityForResult(intent, GlobalKeys.CODE_FOR_QR_ACTIVITY_SCANNER);
   }

   // second button pressed = start tracking \
//   public void startTracking() {
   public void startTracking(View view) {

      if (loadQrFromDb().equals("")) {
         Toast.makeText(this, SCAN_CODE, Toast.LENGTH_SHORT).show();
      } else {
         Toast.makeText(this, CODE_PRESENT, Toast.LENGTH_SHORT).show();

         Intent intentForTest = new Intent();
         PendingIntent pendingIntent = createPendingResult(TASK1_CODE, intentForTest, 0);

         Intent intentServiceGps = new Intent(this, MyService.class);
         intentForTest.putExtra(GlobalKeys.PARAM_PINTENT, pendingIntent);

         status.setText(STATUS_START);

         startService(intentServiceGps);

/*
         bindService(intentServiceGps, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
               Log.v("onServiceConnected", "worked");

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
               Log.v("onServiceDisconnected", "worked");

            }
         }, BIND_IMPORTANT);
*/
      }
   }

   // third button pressed = stop tracking \
   public void stopTracking(View view) {
      status.setText(STATUS_STOP);
      stopService(new Intent(this, MyService.class));
   }

   // fourth button pressed = show QR code \
   public void showQr(View view) {
      Toast.makeText(this, loadQrFromDb(), Toast.LENGTH_SHORT).show();
   }

   // returning point to this activity \
/*
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
      super.onActivityResult(requestCode, resultCode, resultIntent);

      Log.d("onActivityResult", "worked");
      if (resultCode == RESULT_OK) {
         Log.d("if - resultCode", "RESULT_OK = " + resultCode);

         tvLocationGPS.setText(resultIntent.getStringExtra(GlobalKeys.GPS_LOCATION));
         tvGpsTime.setText(resultIntent.getStringExtra(GlobalKeys.GPS_TIME));

         if (requestCode == GlobalKeys.CODE_FOR_QR_ACTIVITY_SCANNER) {
            String resultString = resultIntent.getStringExtra(GlobalKeys.EXTRA_QR_RESULT);
            Log.d("if - requestCode", resultString);
            saveToTheDb(resultIntent.getStringExtra(GlobalKeys.EXTRA_QR_RESULT));
         }
      }
   }
*/

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);

      Log.v(GlobalKeys.TAG, "requestCode: " + String.valueOf(requestCode));
      Log.v(GlobalKeys.TAG, "resultCode: " + String.valueOf(resultCode));

      if (requestCode == GlobalKeys.CODE_FOR_QR_ACTIVITY_SCANNER) {
         saveToTheDb(data.getStringExtra(GlobalKeys.EXTRA_QR_RESULT));
         Log.v(GlobalKeys.TAG, "onActivityResult in requestCode == CODE_FOR_QR_ACTIVITY_SCANNER");
      }

      if (resultCode == GlobalKeys.INTENT_CODE_GPS) {
         tvLocationGPS.setText(data.getStringExtra(GlobalKeys.GPS_LOCATION));
         tvGpsTime.setText(data.getStringExtra(GlobalKeys.GPS_TIME));
         Log.v(GlobalKeys.TAG, "onActivityResult in requestCode == INTENT_CODE_GPS");
      }
   }
// working with database \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

   private void saveToTheDb(String qr) {
      Driver driver = new Driver();
      driver.setQr(qr);
      HelpFactory.getDatabaseHelper().getDriverDao().add(driver);
   }

   private String loadQrFromDb() {
      if (HelpFactory.getDatabaseHelper().getDriverDao().getDriver() == null) {
         return "";
      }
      return HelpFactory.getDatabaseHelper().getDriverDao().getDriver().getQr();
   }

   // Preferences don't work

//    private void saveQrInPreferences(String qr){
//        sPref = getPreferences(MODE_PRIVATE);
//        SharedPreferences.Editor ed = sPref.edit();
//        ed.clear();
//        ed.putString(SAVED_QR, qr);
//        ed.commit();
//    }
//
//    private String loadQrFromPreferences(){
//        sPref = getPreferences(MODE_PRIVATE);
//        //Toast.makeText(this, savedQr, Toast.LENGTH_SHORT).show();
//        return sPref.getString(SAVED_QR, "");
//    }
}