package com.mol.drivergps.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.Result;
import com.mol.drivergps.GlobalKeys;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QrActivity extends Activity implements ZXingScannerView.ResultHandler {

   private final static String QR_CODE_IS_TAKEN = "QR-code is taken successfully";

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

      Toast.makeText(this, QR_CODE_IS_TAKEN, Toast.LENGTH_SHORT).show();

      Log.v("QrActivity", resultString);
      Log.v("QrActivity", rawResult.getBarcodeFormat().toString());

      Intent resultIntent = new Intent();
      resultIntent.putExtra(GlobalKeys.EXTRA_QR_RESULT, resultString);
      setResult(RESULT_OK, resultIntent);

      finish();
   }
}