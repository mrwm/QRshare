package com.wchung.qrshare;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {
    private Bitmap qr_bitmap = null;
    private TextView tv = null;
    private ImageView iv = null;
    private File cacheFile;
    private String stringForQRcode;

    private int smallestScreenDimension;

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

        // Get the smallest screen dimension
        smallestScreenDimension = getSmallestScreenDimension();

        // Define the cache file location
        cacheFile = new File(getCacheDir(), "QR_image.jpg");

        // Do something about intent captures
        // Create a function that handles intent captures and returns the text values in a string
        // The function will also need to handle onNewIntent() as well
        stringForQRcode = getStringFromIntent(getIntent());

        // Do something that converts the string to a QR code
        qr_bitmap = stringToQRcode(stringForQRcode);
        // Create a function that takes a string and creates a QR code to cacheFile
        //saveBitmapToCache(qr_bitmap);

        // Then create a function that handles text changes and updates the QR code
        tv = findViewById(R.id.qr_subtitle);
        iv = findViewById(R.id.imageViewQRCode);
        iv.setOnClickListener(this::clear_focus);
        tv.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
//                Log.i("afterTextChanged", "string: " + s.toString());
                stringForQRcode = s.toString();
                qr_bitmap = stringToQRcode(stringForQRcode);
                iv.setImageBitmap(qr_bitmap);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {}

        });
    }

    private Bitmap stringToQRcode(String stringForQRcode) {
        int qrSize  = 500;
        float multiplier = 0.25f;
        qrSize = smallestScreenDimension;
        qrSize = (int) (qrSize * multiplier);
        Log.i("stringToQRcode", "qrSize: " + qrSize);

        BitMatrix bitMatrix = null;
        Bitmap bitmap_image = null;
        if (stringForQRcode.isEmpty() || stringForQRcode.isBlank()) {
            Log.i("stringToQRcode", "stringForQRcode is empty");
            Drawable d;
            d = AppCompatResources.getDrawable(this, R.drawable.ic_launcher_foreground);
            return new BitmapDrawable(getResources(), String.valueOf(d)).getBitmap();
        }
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            bitMatrix = new QRCodeWriter().encode(stringForQRcode,
                    BarcodeFormat.QR_CODE, qrSize, qrSize, hints);
        } catch (WriterException ex) {
            Log.e("QRCodeGenerator", "Error generating QR code", ex);
            return null;
        }

        bitmap_image = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.RGB_565);
        int[] rowPixels = new int[qrSize];
        BitArray row = new BitArray(qrSize);
        for (int y = 0; y < qrSize; y++) {
            row = bitMatrix.getRow(y, row);
//            Log.i("QR test: setImageQR", String.valueOf(row));
            for (int x = 0; x < qrSize; x++) {
                rowPixels[x] = row.get(x) ? Color.BLACK : Color.WHITE;
            }
            bitmap_image.setPixels(rowPixels, 0, qrSize, 0, y, qrSize, 1);
        }

        return bitmap_image;
    }

    private int getSmallestScreenDimension() {
        if (Resources.getSystem().getDisplayMetrics().widthPixels >
                Resources.getSystem().getDisplayMetrics().heightPixels) {
            smallestScreenDimension = Resources.getSystem().getDisplayMetrics().heightPixels;
        } else {
            smallestScreenDimension = Resources.getSystem().getDisplayMetrics().widthPixels;
        }
        return smallestScreenDimension;
    }

    private void saveBitmapToCache(Bitmap bitmap_image) {
        // Convert the bitmap to a byte array
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap_image.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream);
        byte[] bytearray = byteArrayOutputStream.toByteArray();

        // Save the file to the cache
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(cacheFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            fileOutputStream.write(bytearray);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            fileOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            fileOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
