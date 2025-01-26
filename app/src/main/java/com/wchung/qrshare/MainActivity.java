package com.wchung.qrshare;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.Objects;


public class MainActivity extends AppCompatActivity {
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
        setContentView(R.layout.activity_main);
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
        iv.setOnClickListener(this::clear_focus);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        Log.i("onNewIntent", intent.toString());
        Log.i("onNewIntent", Objects.requireNonNull(intent.getAction()));
        stringForQRcode = getStringFromIntent(getIntent());

        // create a new QR code from the string
        // and set the string for the text edit
    }

    private void clear_focus(View view){
        //String tvText = tv.getText().toString();
        //Log.i("QR test: clear_focus", tvText);
        tv.clearFocus();
    }

    private String getStringFromIntent(Intent intent) {
        String intentAction = intent.getAction();
        String intentType = intent.getType();
        Log.i("getStringFromIntent", "intentAction: " + intentAction);
        Log.i("getStringFromIntent", "intentType: " + intentType);
        if (intentAction != null && intentType != null) {
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

}
