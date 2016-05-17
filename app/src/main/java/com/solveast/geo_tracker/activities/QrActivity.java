package com.solveast.geo_tracker.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.zxing.Result;
import com.solveast.geo_tracker.GlobalKeys;
import com.solveast.geo_tracker.MyLog;

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
      MyLog.v("handleResult: " + resultString);
      MyLog.v("handleResult: " + rawResult.getBarcodeFormat().toString());

      Intent resultIntent = new Intent();
      resultIntent.putExtra(GlobalKeys.QR_RESULT, resultString);
      setResult(GlobalKeys.REQUEST_CODE_QR_ACTIVITY, resultIntent);
      finish();
   }
}