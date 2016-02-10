package com.solveast.gps_tracker.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.zxing.Result;
import com.solveast.gps_tracker.GlobalKeys;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QrActivity extends Activity implements ZXingScannerView.ResultHandler {

   private ZXingScannerView mScannerView;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      mScannerView = new ZXingScannerView(this);
      setContentView(mScannerView);
   }

   @Override
   public void onResume() {
      super.onResume();
      mScannerView.setResultHandler(this);
      mScannerView.startCamera();
   }

   @Override
   public void onPause() {
      super.onPause();
      mScannerView.stopCamera();
   }

   @Override
   public void handleResult(Result rawResult) {

      String resultString = rawResult.getText();
      Log.v("handleResult", "" + resultString);
      Log.v("handleResult", "" + rawResult.getBarcodeFormat().toString());

      Intent resultIntent = new Intent();
      resultIntent.putExtra(GlobalKeys.EXTRA_QR_RESULT, resultString);
      setResult(GlobalKeys.QR_ACTIVITY_KEY, resultIntent);
      finish();
   }
}