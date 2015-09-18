package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class BarcodeScannerActivity extends Activity implements ZXingScannerView.ResultHandler {
    private static final String TAG = BarcodeScannerActivity.class.getSimpleName();

    public static final String EXTRA_EAN = "it.jaschke.alexandria.extra_ean";
    

    private ZXingScannerView mScannerView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mScannerView = new ZXingScannerView(this);   // Programmatically initialize the scanner view
        setContentView(mScannerView);                // Set the scanner view as the content view
    }

    @Override
    public void onResume() {
        super.onResume();
        List<BarcodeFormat> barcodeFormats = new ArrayList<>(1);
        barcodeFormats.add(BarcodeFormat.EAN_13);
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.setFlash(true);
        mScannerView.setAutoFocus(true);
        mScannerView.setFormats(barcodeFormats);
        mScannerView.startCamera();          // Start camera on resume
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
    }

    @Override
    public void handleResult(Result rawResult) {

        String ean = rawResult.getText();
        Log.v(TAG, ean); // Prints scan results
        Log.v(TAG, rawResult.getBarcodeFormat().toString()); // Prints the scan format (qrcode, pdf417 etc.)

        if (ean.length() == 13) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_EAN, ean);
            setResult(RESULT_OK, resultIntent);
        }
        finish();
    }
}
