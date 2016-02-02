package com.mol.drivergps.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.zxing.Result;
import com.mol.drivergps.GlobalKeys;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QrActivity extends Activity implements ZXingScannerView.ResultHandler {

    public static final String TAG = "QrActivity";

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
        // Do something with the result here

        String resultString = rawResult.getText();

        Log.v(TAG, resultString);
        Log.v(TAG, rawResult.getBarcodeFormat().toString());

        Intent resultIntent = new Intent();
        resultIntent.putExtra(GlobalKeys.EXTRA_QR_RESULT, resultString);
        setResult(RESULT_OK, resultIntent);
//        setResult(GlobalKeys.CODE_FOR_QR_ACTIVITY_SCANNER, resultIntent);

        finish();
    }
}
