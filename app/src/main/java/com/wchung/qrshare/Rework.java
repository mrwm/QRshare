package com.wchung.qrshare;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.zxing.common.BitMatrix;

import java.io.File;


public class Rework extends AppCompatActivity {
    private BitMatrix bitMatrix = null;
    private Bitmap bitmap = null;
    private TextView tv = null;
    private ImageView iv = null;
    private File cacheFile;
    private String stringForQRcode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Do something about intent captures
        // Create a function that handles intent captures and returns the text values in a string
        // The function will also need to handle onNewIntent() as well
        stringForQRcode = getStringFromIntent(getIntent());

        // Do something that converts the string to a QR code
        // Create a function that takes a string and creates a QR code to cacheFile

        // Then create a function that handles text changes and updates the QR code


        tv = findViewById(R.id.qr_subtitle);
        iv = findViewById(R.id.imageViewQRCode);
        iv.setOnClickListener(this::hide_keyboard);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        stringForQRcode = getStringFromIntent(getIntent());

        // create a new QR code from the string
        // and set the string for the text edit
    }

    private void hide_keyboard(View view){
        tv.clearFocus();
    }

    private String getStringFromIntent(Intent intent) {
        String intentAction = intent.getAction();
        String intentType = intent.getType();
        if (intent != null && Intent.ACTION_SEND == intentAction && intentType != null) {
            if ("text".equals(intentType.split("/")[0])) {
                String intentText = intent.getStringExtra(Intent.EXTRA_TEXT);
                //Log.i("QR test: getStringFromIntent", "intentText: " + intentText);
                return intentText;
            } else {
                Toast.makeText(getApplicationContext(), R.string.unsupported_mimetype
                        + intentType, Toast.LENGTH_LONG).show();
            }
        }
        return "";
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.i("onPause", "leaving... bye~!");
        finish();
    }

}
