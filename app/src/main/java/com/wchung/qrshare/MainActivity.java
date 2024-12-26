package com.wchung.qrshare;

import android.content.Context;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
//import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
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

        ////// Intent captures //////
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        String data = getString(R.string.qr_instructions);
        String unsupported_mimetype = getString(R.string.unsupported_mimetype);
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text".equals(type.split("/")[0])) {
                data = handleSendText(intent); // Handle text being sent
            } else {
                data = type.split("/")[0];
                Toast.makeText(getApplicationContext(), unsupported_mimetype + data, Toast.LENGTH_LONG).show();
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                data = type;
                Toast.makeText(getApplicationContext(), unsupported_mimetype + data, Toast.LENGTH_LONG).show();
                //handleSendMultipleImages(intent); // Handle multiple images being sent
            }
        } else {
            if (type != null){
                data = type;
            }
            String default_start_string = getString(R.string.default_start_string);
            Toast.makeText(getApplicationContext(), default_start_string + data, Toast.LENGTH_SHORT).show();
            // Handle other intents, such as being started from the home screen
        }

        /*
         * Maybe... Convert non-text data to text with base64 encoding
         * Then pipe it into the QR code with the following layout:
         *       data:<mime-type>;base64,<base64>
         * eg:   data:text/plain;base64,SGVsbG8gV29ybGQ=
         *       data:image/png;base64,iVBORw0KGgo
         *
         * The only drawback is the limitation of file size
         * 2950 bytes or 2.95 Kb max for QR codes
         * */

        ////// QR CODE GENERATION //////
        float screenPercentage = 0.8f;
        int screenWidth = (int) (getScreenWidth() * screenPercentage);
        int screenHeight = (int) (getScreenWidth() * screenPercentage);
        BitMatrix bitMatrix = generateQRCode(data, screenWidth, screenHeight);
        ImageView iv = findViewById(R.id.imageViewQRCode);
        iv.setOnClickListener(this::generate_QR);
        if (bitMatrix != null) {
            setImageQR(bitMatrix);
        }

        ////// TEXT GENERATION //////
        TextView tv = findViewById(R.id.qr_subtitle);
        TextInputLayout tvh = findViewById(R.id.qr_subtitle_hint);
        if (type != null) {
            tvh.setHint(type);
        }
        tv.setText(data);
        /* TODO:
        - Max size is around 1307? So about 1.307kB?
         */
        /*
        tv.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //Log.i("QR test", "onText changed: " + s.toString());
                return;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                return;
            }

            @Override
            public void afterTextChanged(Editable s) {
                // yeah this sucks for performance. Lets not do this
                BitMatrix bitMatrix = generateQRCode(s.toString(), screenWidth, screenHeight);
                if (bitMatrix != null) {
                    setImageQR(bitMatrix);
                }
                //Log.i("QR test", "afterText changed: " + s.toString());
                return;
            }
        });
         */
    }

    public void generate_QR(View view) {
        /*
        Onclick handler for hiding the keyboard and generating the QR code
         */
        TextView tv = findViewById(R.id.qr_subtitle);
        tv.clearFocus();
        String tvText = tv.getText().toString();
        final InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(tv.getWindowToken(), 0);

        float screenPercentage = 0.8f;
        int screenWidth = (int) (getScreenWidth() * screenPercentage);
        int screenHeight = (int) (getScreenWidth() * screenPercentage);
        if (tvText.isEmpty()){
            Toast.makeText(getApplicationContext(), R.string.default_start_string, Toast.LENGTH_LONG).show();
            tvText = getString(R.string.qr_instructions);
        }
        BitMatrix bitMatrix = generateQRCode(tvText, screenWidth, screenHeight);
        if (bitMatrix != null) {
            setImageQR(bitMatrix);
        }
        Log.i("QR test", "onClick" + view.getId() + " " + R.id.imageViewQRCode);
    }

    private void setImageQR(BitMatrix bitMatrix) {
        /*
        Generates the QR code image from the BitMatrix
         */
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        ImageView iv = findViewById(R.id.imageViewQRCode);
        iv.setImageBitmap(bitmap);
        return;
    }

    private String handleSendText(Intent intent) {
        /*
        Handles text being shared
         */
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            return sharedText;
            // Update UI to reflect text being shared
        }
        else {
            Bundle dataUris = intent.getExtras();
            // intent.getParcelableExtra("android.intent.extra.STREAM") gets the file URI
            //Log.w("QR test", intent.getParcelableExtra("android.intent.extra.STREAM").toString());

            ContentResolver contentResolver = getContentResolver();
            try {
                InputStream inputStream = contentResolver.openInputStream(Objects.requireNonNull(intent.getParcelableExtra("android.intent.extra.STREAM")));
                assert inputStream != null;
                // Returns the file size in bytes
                //Log.i("QR test", "File Size: " + inputStream.available());
                if (inputStream.available() > 1307) {
                    //Log.w("QR test", "Data too large to share");
                    Toast.makeText(getApplicationContext(), "Data too large to share", Toast.LENGTH_LONG).show();
                }
                BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder total = new StringBuilder();
                for (String line; (line = r.readLine()) != null; ) {
                    total.append(line).append('\n');
                }
                return total.toString();

            } catch (IOException e) {
                // Handle exceptions
                Log.e("StreamProcessing", "Error accessing stream data", e);
            }

            if (dataUris != null) {
                sharedText = dataUris + " text";
                // Update UI to reflect multiple images being shared
            }
            Toast.makeText(getApplicationContext(), "Unable to parse " + intent.getType() + " yet", Toast.LENGTH_LONG).show();
            return sharedText + ":" + dataUris;
        }
    }

    private BitMatrix generateQRCode(String data, int width, int height) {
        /*
        Generates the bit matrix for the QR code
         */
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            return new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, width, height, hints);
        } catch (WriterException ex) {
            Log.e("QRCodeGenerator", "Error generating QR code", ex);
            return null; // Or throw an exception
        }
    }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public void onPause(){
        super.onPause();
        finish();
    }
}