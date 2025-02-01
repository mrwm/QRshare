package com.wchung.qrshare;

import android.app.assist.AssistContent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


public class MainActivity extends AppCompatActivity {
    private Bitmap qr_bitmap;
    private TextView tv;
    private ImageView iv;
    private File cacheFile;
    private String stringType;
    private String stringForQRcode;
    private boolean dataTooLarge;

    private TextView subtitleHint;
    private AutoTransition autoTransition;
    private ViewGroup rootView;

    private int dp16;
    private int dp2;

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

        dp16 = (int) convertDpToPixel(16, this);
        dp2 = (int) convertDpToPixel(2, this);

        // Define the cache file location
        cacheFile = new File(getCacheDir(), "QR_image.jpg");

        // Do something about intent captures
        // Handles intent captures and returns the text values in a string
        // The function will also need to handle onNewIntent() as well
        stringForQRcode = getStringFromIntent(getIntent());
        //Log.i("onCreate", "stringForQRcode: " + stringForQRcode);

        // Convert the string to a QR code
        qr_bitmap = stringToQRcode(stringForQRcode);
        // Create a function that takes a string and creates a QR code to cacheFile
        //saveBitmapToCache(qr_bitmap);

        iv = findViewById(R.id.imageViewQRCode);
        // Set the image view to the QR code
        iv.setImageBitmap(qr_bitmap);
        // Set the margins of the image view
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) iv.getLayoutParams();
        lp.setMargins(dp16*2, dp16*2, dp16*2, dp16*2);
        iv.setLayoutParams(lp);

        // Clear the focus when the image view is tapped. Just a pretty touch effect
        iv.setOnClickListener(view -> tv.clearFocus());

        // Open a menu when long pressing the image
        this.registerForContextMenu(iv);


        // Set the text view to the stringForQRcode
        tv = findViewById(R.id.qr_subtitle);
        tv.setText(stringForQRcode);
        //Log.i("onCreate", "stringType: " + stringType);

        // The wild mess to programmatically create a TextView :)
        subtitleHint = new TextView(this);
        subtitleHint.setId(View.generateViewId());
        subtitleHint.setText(getString(R.string.qr_instructions));
        TypedValue windowBackground = new TypedValue();
        this.getTheme().resolveAttribute(android.R.attr.windowBackground, windowBackground, true);
        subtitleHint.setBackground(ContextCompat.getDrawable(this, windowBackground.resourceId));
        TypedValue colorPrimary = new TypedValue();
        this.getTheme().resolveAttribute(android.R.attr.windowBackground, colorPrimary, true);
        subtitleHint.setBackground(ContextCompat.getDrawable(this, colorPrimary.resourceId));
        setViewMargins(subtitleHint, dp16, dp16, dp16, dp16);
        subtitleHint.setGravity(Gravity.TOP | Gravity.START);
        subtitleHint.setPadding(dp16, dp16, dp16, 0);
        subtitleHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

        // Update the QR code when the text is changed
        tv.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                //Log.i("afterTextChanged", "string: " + s.toString());
                stringForQRcode = s.toString();
                qr_bitmap = stringToQRcode(stringForQRcode);
                iv.setImageBitmap(qr_bitmap);
                subtitleHint.setText(stringType);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {}

        });

        // Animate layout change for the hint when the textbox is selected (or not)
        tv.setOnFocusChangeListener((v, hasFocus) -> {
            Log.i("onFocusChange", "hasFocus: " + hasFocus);
            if (tv.getText() == null || tv.getText().length() == 0) {
                subtitleHint.setText(getString(R.string.app_name));
            }
            if(hasFocus) {
                TransitionManager.beginDelayedTransition(rootView, autoTransition);
                setViewMargins(subtitleHint, dp16, -dp16-dp2, dp16/2, dp16/2);
            } else if (tv.getText() == null || tv.getText().length() == 0) {
                subtitleHint.setText(getString(R.string.qr_instructions));
                TransitionManager.beginDelayedTransition(rootView, autoTransition);
                setViewMargins(subtitleHint, dp16, dp16, dp16, dp16);
            }
        });

        // Subtitle hint for the text type
        // Get the root view and create a transition.
        rootView = findViewById(R.id.frame_layout);
        autoTransition = new AutoTransition();
        autoTransition.setDuration(75);

        if (stringForQRcode != null) {
            TransitionManager.beginDelayedTransition(rootView, autoTransition);
            setViewMargins(subtitleHint, dp16, -dp16-dp2, dp16/2, dp16/2);
            subtitleHint.setText(stringType);
        }
        TransitionManager.beginDelayedTransition(rootView, autoTransition);
        rootView.addView(subtitleHint);

    }

    private static void setViewMargins(View view, int left, int top, int right, int bottom) {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        view.setLayoutParams(layoutParams);
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        marginLayoutParams.setMargins(left, top, right, bottom);
        view.setLayoutParams(marginLayoutParams);
    }

    public static float convertDpToPixel(float dp, @NonNull Context context){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi /
                DisplayMetrics.DENSITY_DEFAULT);
    }

    private Bitmap stringToQRcode(String stringForQRcode) {
        String no_data = getString(R.string.no_data);
        int qrSize;
        qrSize = Math.min(Resources.getSystem().getDisplayMetrics().widthPixels,
                            Resources.getSystem().getDisplayMetrics().heightPixels);
        BitMatrix bitMatrix;
        Bitmap bitmap_image;

        if (stringForQRcode != null) {
            int stringLength = stringForQRcode.length();
            //Log.i("stringToQRcode", "stringLength: " + stringLength);
            // 1273 chars is the largest possible string length
            dataTooLarge = (stringLength > 1272);
        }
        if (dataTooLarge) {
            stringForQRcode = getString(R.string.data_too_large);
            Log.i("stringToQRcode", "stringForQRcode is too large");
        }
        if (stringForQRcode == null){
            Log.i("stringToQRcode", "stringForQRcode is null");
            stringForQRcode = no_data;
        }
        if (stringForQRcode.isEmpty() || stringForQRcode.isBlank()) {
            Log.i("stringToQRcode", "stringForQRcode is empty");
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
            Toast.makeText(getApplicationContext(), "Error generating QR code", Toast.LENGTH_LONG).show();
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

    private void saveBitmapToCache(@NonNull Bitmap bitmap_image) {
        // Convert the bitmap to a byte array
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap_image.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] bytearray = byteArrayOutputStream.toByteArray();

        // Save the file to the cache
        FileOutputStream fileOutputStream;
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
        stringForQRcode = getStringFromIntent(intent);
        tv.setText(stringForQRcode);

        qr_bitmap = stringToQRcode(stringForQRcode);
        iv.setImageBitmap(qr_bitmap);
        // Set the margins of the image view
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) iv.getLayoutParams();
        lp.setMargins(dp16*2, dp16*2, dp16*2, dp16*2);
        iv.setLayoutParams(lp);

        //subtitleHint
        setViewMargins(subtitleHint, dp16, -dp16-dp2, dp16/2, dp16/2);
    }

    private String getStringFromIntent(Intent intent) {
        dataTooLarge = false;
        String intentAction = intent.getAction();
        String intentType = intent.getType();
        String unsupported_mimetype = getString(R.string.unsupported_mimetype);
        Log.i("getStringFromIntent", "intentAction: " + intentAction);
        Log.i("getStringFromIntent", "intentType: " + intentType);

        String intentText = intent.getStringExtra(Intent.EXTRA_TEXT);
        Log.i("getStringFromIntent", "intentText: " + intentText);
        stringType = intentType;
        if (Intent.ACTION_SEND.equals(intentAction) && intentType != null) {
            //Log.i("getStringFromIntent", "intentAction and intentType are not null");
            if ("text".equals(intentType.split("/")[0])) {
                Bundle dataUris = intent.getExtras();
                intentText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (intentText == null && dataUris != null) {
                    // intent.getParcelableExtra("android.intent.extra.STREAM") gets the file URI

                    ContentResolver contentResolver = getContentResolver();
                    try {
                        InputStream inputStream = contentResolver.openInputStream(
                                Objects.requireNonNull(intent.getParcelableExtra(
                                        "android.intent.extra.STREAM")));
                        assert inputStream != null;
                        // Returns the file size in bytes
                        //Log.i("getStringFromIntent", "File Size: " + inputStream.available());
                        if (inputStream.available() > 1307) {
                            //Log.w("getStringFromIntent", "Data too large to share");
                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.data_too_large), Toast.LENGTH_LONG).show();
                        }
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
                    Toast.makeText(getApplicationContext(), "Unable to parse " +
                            intent.getType() + " yet", Toast.LENGTH_LONG).show();
                    return null;
                }
            } else {
                intentType = intentType.split("/")[0];
                Toast.makeText(getApplicationContext(), unsupported_mimetype + intentType,
                        Toast.LENGTH_LONG).show();
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(intentAction) && intentType != null) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.multi_share_not_supported), Toast.LENGTH_LONG).show();
        } else {
            stringType = getString(R.string.app_name);
        }
        return intentText;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        //menu.setHeaderTitle("Context Menu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_list, menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        saveBitmapToCache(qr_bitmap);
        Uri uriForFile = FileProvider.getUriForFile(this,
                this.getApplicationContext().getPackageName() + ".provider", cacheFile);
        int itemId = item.getItemId();
        if (itemId == R.id.copy) {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newUri(getContentResolver(), "Image", uriForFile);
            clipboard.setPrimaryClip(clip);

            //Toast.makeText(this, R.string.menu_copy, Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.edit) {
            // Kinda funny, I used this menu option to test the animations for the text view.
            // I didn't expect this that I would have a use for this menu item :)
            tv.requestFocus();
            //Toast.makeText(this, R.string.menu_edit, Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.share) {
            //Toast.makeText(this, R.string.menu_share, Toast.LENGTH_SHORT).show();
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
            sendIntent.setType("image/jpg");
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent shareIntent = Intent.createChooser(sendIntent, "Share QR code using");
            startActivity(shareIntent);
        } else if (itemId == R.id.wear) {
            Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
            requestTranscription(null);
        } else {
            Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    // ref: https://developer.android.com/guide/components/activities/recents
    @Override
    public void onProvideAssistContent(AssistContent outContent) {
        super.onProvideAssistContent(outContent);
        if (stringForQRcode == null) {
            stringForQRcode = getString(R.string.no_data);
        }
        outContent.setWebUri(Uri.parse(stringForQRcode));
    }

    // Wear capability
    private static final String COUNT_KEY = "com.example.key.count";
    private DataClient dataClient;
    private int count = 0;
    // Create a data map and put data in it
    private void increaseCounter() {
        dataClient = Wearable.getDataClient(this);
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/count");
        putDataMapReq.getDataMap().putInt(COUNT_KEY, count++);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        Task<DataItem> putDataTask = dataClient.putDataItem(putDataReq);
    }
}