package com.wchung.qrshare;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StringUtil extends AppCompatActivity {

    public String getStringType(Intent intent) {
        String stringType;
        stringType = intent.getType();
        if (stringType == null) {
            stringType = "QR share";
        }
        return stringType;
    }

    public Object getStringFromIntent(Context context, Intent intent) {
        String intentAction = intent.getAction();
        //Log.i("getStringFromIntent", "intentAction: " + intentAction);

        String intentText = intent.getStringExtra(Intent.EXTRA_TEXT);
        //Log.i("getStringFromIntent", "intentText: " + intentText);
        // Return immediately if there's text from the intent, not from the included content
        if (intentText != null) {
            return intentText;
        }

        // Handle content that came with the intent
        Bundle extras = intent.getExtras();
        // Exit if there's no content
        if (extras == null) {
            return null;
        }
        if (Intent.ACTION_SEND.equals(intentAction)) {
            //Log.d("getStringFromIntent", "ITS A FILE!");
            Uri singleFile;
            // singleFile = extras.getParcelable(Intent.EXTRA_STREAM, Uri.class); for API >= 33
            singleFile = extras.getParcelable(Intent.EXTRA_STREAM);
            //Log.i("getStringFromIntent", "singleFile: " + singleFile);

            ContentResolver contentResolver = context.getContentResolver();
            try {
                assert singleFile != null;
                InputStream inputStream = contentResolver.openInputStream(singleFile);
                assert inputStream != null;
                // Returns the file size in bytes
                //Log.i("getStringFromIntent", "File Size: " + inputStream.available());
                if (inputStream.available() > 1307) {
                    Log.w("getStringFromIntent", "Data too large to share");
                    Toast.makeText(context,
                            App.getRes().getString(R.string.data_too_large), Toast.LENGTH_LONG).show();
                }

                // Encode anything not text to Base64
                if (!getStringType(intent).startsWith("text/")) {
                    Log.d("getStringFromIntent", "ITS NOT A TEXT FILE!");
                    try {
                        byte[] bytes = getBytes(inputStream);
                        return bytes;
                        //intentText = Base64.encodeToString(bytes,Base64.DEFAULT);
                        //return "data:" + getStringType(intent) + ";base64," + intentText;
                    } catch (Exception e) {
                        Log.e("StreamProcessing", "Error accessing stream data", e);
                    }
                }

                // Otherwise, just read the file
                BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder total = new StringBuilder();
                for (String line; (line = r.readLine()) != null; ) {
                    total.append(line).append('\n');
                }
                inputStream.close();
                intentText = total.toString();
                //Log.i("getStringFromIntent", "intentText: " + intentText);
                return intentText;

            } catch (IOException e) {
                // Handle exceptions
                Log.e("StreamProcessing", "Error accessing stream data", e);
            }
            Toast.makeText(context, "Unable to parse the data", Toast.LENGTH_LONG).show();
            Log.wtf("getStringFromIntent", "Intent.ACTION_SEND: how did you get here?");
            return null;
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(intentAction)) {
            ArrayList<Uri> uris;
            //uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri.class); for API >= 33
            uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            Log.d("getStringFromIntent", "uris: " + uris);
            Toast.makeText(context,
                    App.getRes().getString(R.string.multi_share_not_supported), Toast.LENGTH_LONG).show();
        }
        Log.e("getStringFromIntent", "You somehow reached the end...");
        return null;
    }

    public Bitmap stringToQRcode(Context context, String stringForQRcode) {
        String no_data = App.getRes().getString(R.string.no_data);
        int qrSize;
        qrSize = Math.min(Resources.getSystem().getDisplayMetrics().widthPixels,
                Resources.getSystem().getDisplayMetrics().heightPixels);
        BitMatrix bitMatrix;
        Bitmap bitmap_image;

        if (stringForQRcode == null || stringForQRcode.isEmpty() ){
            // Not using .isBlank(), as we also want to create a QR code for white space/tabs/etc
            // Not sure if people actually use it though, but wouldn't want to block that use case
            //Log.i("stringToQRcode", "stringForQRcode is null");
            stringForQRcode = no_data;
        }
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            bitMatrix = new QRCodeWriter().encode(stringForQRcode,
                    BarcodeFormat.QR_CODE, qrSize, qrSize, hints);
        } catch (WriterException ex) {
            Log.e("QRCodeGenerator", "Error generating QR code", ex);
            Toast.makeText(context, "Error generating QR code", Toast.LENGTH_LONG).show();
            return null;
        }

        bitmap_image = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.RGB_565);
        int[] rowPixels = new int[qrSize];
        BitArray row = new BitArray(qrSize);
        for (int y = 0; y < qrSize; y++) {
            row = bitMatrix.getRow(y, row);
            for (int x = 0; x < qrSize; x++) {
                rowPixels[x] = row.get(x) ? Color.BLACK : Color.WHITE;
            }
            bitmap_image.setPixels(rowPixels, 0, qrSize, 0, y, qrSize, 1);
        }

        return bitmap_image;
    }

    public static byte[] getBytes(InputStream inputStream) throws IOException {
        int bufferSize = 1024;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] byteArray = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(byteArray)) != -1) {
            byteArrayOutputStream.write(byteArray, 0, len);
        }
        return byteArrayOutputStream.toByteArray();
    }

}