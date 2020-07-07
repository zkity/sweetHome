package com.example.smarthome;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class ScanActivity extends AppCompatActivity {
    private CaptureManager capture;
    private ImageButton buttonLed;
    private DecoratedBarcodeView barcodeScannerView;
    private boolean bTorch = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        barcodeScannerView = initializeContent();
        buttonLed = findViewById(R.id.button_led);
        /*根据闪光灯状态设置imagebutton*/
        barcodeScannerView.setTorchListener(new DecoratedBarcodeView.TorchListener() {
            @Override
            public void onTorchOn() {
                buttonLed.setBackground(getResources().getDrawable(R.drawable.image_bg_on));
                bTorch = true;
            }

            @Override
            public void onTorchOff() {
                buttonLed.setBackground(getResources().getDrawable(R.drawable.image_bg_off));
                bTorch = false;
            }
        });

        /*开关闪光灯*/
        buttonLed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bTorch){
                    barcodeScannerView.setTorchOff();
                } else {
                    barcodeScannerView.setTorchOn();
                }

            }
        });

        capture = new CaptureManager(this, barcodeScannerView);

        capture.initializeFromIntent(getIntent(), savedInstanceState);

        capture.decode();
    }




    /**
     * Override to use a different layout.
     *
     * @return the DecoratedBarcodeView
     */
    protected DecoratedBarcodeView initializeContent() {
        setContentView(R.layout.activity_scan);
        return (DecoratedBarcodeView)findViewById(R.id.dbv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
        barcodeScannerView.setTorchOff();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        capture.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
}