package com.wchung.qrshare;

// android.*
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
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
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

// androidx.*
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// *.xzing.*
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

// java.*
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private Bitmap qr_bitmap;
    private TextView tv;
    private ImageView iv;
    private File cacheFile;
    private String stringForQRcode;

    private TextView subtitleHint;
    private AutoTransition autoTransition;
    private ViewGroup rootView;

    private int dp16;
    private int dp2;

    public static float convertDpToPixel(float dp, @NonNull Context context){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi /
                DisplayMetrics.DENSITY_DEFAULT);
    }

    private static void setViewMargins(View view, int left, int top, int right, int bottom) {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        view.setLayoutParams(layoutParams);
        ViewGroup.MarginLayoutParams marginLayoutParams =
                (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        marginLayoutParams.setMargins(left, top, right, bottom);
        view.setLayoutParams(marginLayoutParams);
    }

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

        // prep for android 15
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectUnsafeIntentLaunch()
                    .build()
            );
        }

        dp16 = (int) convertDpToPixel(16, this);
        dp2 = (int) convertDpToPixel(2, this);

        // Define the cache file location
        cacheFile = new File(getCacheDir(), "QR_image.jpg");

        // Do something about intent captures
        // Handles intent captures and returns the text values in a string
        // The function will also need to handle onNewIntent() as well
        stringForQRcode = new StringUtil().getStringFromIntent(getIntent());

        // Get the string type from the intent
        String finalStringType = new StringUtil().getStringType(getIntent());

        // Convert the string to a QR code
        qr_bitmap = new StringUtil().stringToQRcode(stringForQRcode);
        // Create a function that takes a string and creates a QR code to cacheFile
        //saveBitmapToCache(qr_bitmap);

        // Set the image view to the QR code
        iv = findViewById(R.id.imageViewQRCode);
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

        // Set the background color of the hint
        TypedValue windowBackground = new TypedValue();
        this.getTheme().resolveAttribute(
                android.R.attr.windowBackground, windowBackground, true);
        subtitleHint.setBackground(
                ContextCompat.getDrawable(this, windowBackground.resourceId));

        setViewMargins(subtitleHint, dp16, dp16, dp16, dp16);
        subtitleHint.setGravity(Gravity.TOP | Gravity.START);
        subtitleHint.setPadding(dp16-(dp2*3), dp16, dp16-(dp2*3), 0);

        // Set the text of the hint
        subtitleHint.setText(getString(R.string.qr_instructions));
        subtitleHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

        // Update the QR code when the text is changed
        tv.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                //Log.i("afterTextChanged", "string: " + s.toString());
                stringForQRcode = s.toString();
                qr_bitmap = new StringUtil().stringToQRcode(stringForQRcode);
                iv.setImageBitmap(qr_bitmap);
                subtitleHint.setText(finalStringType);

                if (tv.getText() == null || tv.getText().toString().isEmpty()) {
                    subtitleHint.setText(getString(R.string.qr_instructions));
                    TransitionManager.beginDelayedTransition(rootView, autoTransition);
                    setViewMargins(subtitleHint, dp16, dp16, dp16, dp16);
                    subtitleHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                TransitionManager.beginDelayedTransition(rootView, autoTransition);
                setViewMargins(subtitleHint, dp16, -dp16-dp2, dp16/2, dp16/2);
                subtitleHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {}

        });

        // Animate layout change for the hint when the textbox is selected (or not)
        tv.setOnFocusChangeListener((v, hasFocus) -> {
            Log.i("onFocusChange", "hasFocus: " + hasFocus);
            if (tv.getText() == null || tv.getText().toString().isEmpty()) {
                subtitleHint.setText(getString(R.string.app_name));
            }
            TransitionManager.beginDelayedTransition(rootView, autoTransition);
            if(hasFocus) {
                setViewMargins(subtitleHint, dp16, -dp16-dp2, dp16/2, dp16/2);
                subtitleHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            } else if (tv.getText() == null || tv.getText().toString().isEmpty()) {
                subtitleHint.setText(getString(R.string.qr_instructions));
                setViewMargins(subtitleHint, dp16, dp16, dp16, dp16);
                subtitleHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            }
        });

        // Get the root view and create a transition.
        rootView = findViewById(R.id.frame_layout);
        autoTransition = new AutoTransition();
        autoTransition.setDuration(75); // 50 works too, but is slightly too fast to notice
        rootView.addView(subtitleHint);

        if (stringForQRcode != null) {
            TransitionManager.beginDelayedTransition(rootView, autoTransition);
            setViewMargins(subtitleHint, dp16, -dp16-dp2, dp16/2, dp16/2);
            subtitleHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            subtitleHint.setText(finalStringType);
        }

    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);

        // Grab the text from the new intent and update the textedit
        stringForQRcode = new StringUtil().getStringFromIntent(intent);
        tv.setText(stringForQRcode);

        // Then update the QR code with the corresponding text
        qr_bitmap = new StringUtil().stringToQRcode(stringForQRcode);
        iv.setImageBitmap(qr_bitmap);

        // Move the text type hint out of the way of the text if there's a given text
        setViewMargins(subtitleHint, dp16, -dp16-dp2, dp16/2, dp16/2);
        subtitleHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        if (tv.getText().toString().isEmpty()) {
            // Don't move the text type hint if there's no text
            setViewMargins(subtitleHint, dp16, dp16, dp16, dp16);
            subtitleHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        //menu.setHeaderTitle("Context Menu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_list, menu);
        if (new StringUtil().stringToQRcode(stringForQRcode) == null) {
            menu.findItem(R.id.copy).setEnabled(false);
            menu.findItem(R.id.share).setEnabled(false);
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        Uri uriForFile = null;
        if (new StringUtil().stringToQRcode(stringForQRcode) != null) {
            // Save the image to the cache
            saveBitmapToCache(qr_bitmap);
            uriForFile = FileProvider.getUriForFile(this,
                    this.getPackageName() + ".provider", cacheFile);
        }

        // Handle the menu item clicks
        int itemId = item.getItemId();
        if (itemId == R.id.copy) {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newUri(getContentResolver(), "Image", uriForFile);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, R.string.menu_copy, Toast.LENGTH_SHORT).show();
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
        } else {
            Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
        }
        return true;
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

    // ref: https://developer.android.com/guide/components/activities/recents
    @Override
    public void onProvideAssistContent(AssistContent outContent) {
        super.onProvideAssistContent(outContent);
        if (stringForQRcode == null) {
            stringForQRcode = getString(R.string.no_data);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            outContent.setWebUri(Uri.parse(stringForQRcode));
        }
    }
}
