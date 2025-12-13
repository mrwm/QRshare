package com.wchung.qrshare;

// android.*
import android.app.assist.AssistContent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

// androidx.*
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.node.WeakReference;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// java.*
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Google material color
//import com.google.android.material.color.DynamicColors;


public class MainActivity extends AppCompatActivity {
    private Bitmap qr_bitmap;
    private static TextView tv;
    private ImageView iv;
    private File cacheFile;
    private static String stringForQRcode;

    private TextView subtitleHint;
    private AutoTransition autoTransition;
    private ViewGroup rootView;

    private int dp16;
    private int dp2;

    public static float convertDpToPixel(float dp, @NonNull Context context){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi /
                DisplayMetrics.DENSITY_DEFAULT);
    }

    private static RoundedBitmapDrawable roundifyImage(@NonNull ImageView imageView, Bitmap bitmapImage, int cornerRadius, @NonNull Context context) {
        RoundedBitmapDrawable dr =
                RoundedBitmapDrawableFactory.create(imageView.getResources(), bitmapImage);
        dr.setCornerRadius(convertDpToPixel(cornerRadius, context));
        return dr;
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

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //DynamicColors.applyToActivitiesIfAvailable(this.getApplication());
        EdgeToEdge.enable(MainActivity.this);
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

        dp16 = (int) convertDpToPixel(16, MainActivity.this);
        dp2 = (int) convertDpToPixel(2, MainActivity.this);

        // Define the cache file location
        cacheFile = new File(getCacheDir(), "QR_image.jpg");

        // Do something about intent captures
        // Handles intent captures and returns the text values in a string
        // The function will also need to handle onNewIntent() as well
        Object obj = new StringUtil().getStringFromIntent(MainActivity.this, getIntent());
        Log.i("onCreate", "obj: " + obj);
        if (obj == null) {
            stringForQRcode = null;
        } else if (obj instanceof String) {
            stringForQRcode = obj.toString();
        } else {
            String intentType = new StringUtil().getStringType(getIntent());
            ExecutorService executor = Executors.newSingleThreadExecutor();

            handler.post(() -> {
                // UI Thread work here
                stringForQRcode = App.getRes().getString(R.string.encoding);
                tv.setText(stringForQRcode);
            });
            executor.execute(() -> {
                // Background work here
                stringForQRcode = Base64.encodeToString((byte[]) obj,Base64.DEFAULT);
                //Log.i("oncreate", "stringForQRcode: " + stringForQRcode + "background");

                Message msg = handler.obtainMessage();
                msg.obj = "data:" + intentType + ";base64," + stringForQRcode;
                handler.sendMessage(msg);
            });
            executor.shutdown();
            //stringForQRcode = "data:" + new StringUtil().getStringType(intent) + ";base64," + Base64.encodeToString((byte[]) obj,Base64.DEFAULT);
            //stringForQRcode = App.getRes().getString(R.string.encoding);
        }
        //stringForQRcode = new StringUtil().getStringFromIntent(MainActivity.this, getIntent()).toString();

        // Get the string type from the intent
        String finalStringType = new StringUtil().getStringType(getIntent());

        // Convert the string to a QR code
        qr_bitmap = new StringUtil().stringToQRcode(MainActivity.this, stringForQRcode);
        // Create a function that takes a string and creates a QR code to cacheFile
        //saveBitmapToCache(qr_bitmap);

        // Set the image view to the QR code
        iv = findViewById(R.id.image_view_qr);
        iv.setImageDrawable(roundifyImage(iv, qr_bitmap, dp16/2, MainActivity.this));

        // Make the image corners round
        iv.setClipToOutline(true);

        // Setup for the image frame
        FrameLayout ifv = findViewById(R.id.image_frame);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) ifv.getLayoutParams();

        // Set the linear layout to the correct orientation using code instead of xml
        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        LinearLayout screenOrientation = findViewById(R.id.main);
        if (screenWidth > screenHeight) {
            screenOrientation.setOrientation(LinearLayout.HORIZONTAL);
            lp.setMargins(dp16*4, dp16, dp16*3, dp16);
        } else {
            screenOrientation.setOrientation(LinearLayout.VERTICAL);
            lp.setMargins(dp16, dp16, dp16, dp16);
        }

        // Set the text view to the stringForQRcode
        tv = findViewById(R.id.qr_subtitle);
        tv.setText(stringForQRcode);

        // Clear the focus when the image view is tapped. Just a pretty touch effect
        iv.setOnClickListener(view -> tv.clearFocus());

        // Open a menu when long pressing the image
        MainActivity.this.registerForContextMenu(iv);

        // The wild mess to programmatically create a TextView :)
        subtitleHint = new TextView(MainActivity.this);
        subtitleHint.setId(View.generateViewId());

        // Set the background color of the hint
        TypedValue windowBackground = new TypedValue();
        MainActivity.this.getTheme().resolveAttribute(
                android.R.attr.windowBackground, windowBackground, true);
        subtitleHint.setBackground(
                ContextCompat.getDrawable(MainActivity.this, windowBackground.resourceId));

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
                qr_bitmap = new StringUtil().stringToQRcode(MainActivity.this, stringForQRcode); // crashes the app if null
                iv.setImageDrawable(roundifyImage(iv, qr_bitmap, dp16/2, MainActivity.this));
                subtitleHint.setText(finalStringType);
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
            //Log.i("onFocusChange", "hasFocus: " + hasFocus);
            TransitionManager.beginDelayedTransition(rootView, autoTransition);
            if(hasFocus) {
                if (tv.getText() == null || tv.getText().toString().isEmpty())
                    subtitleHint.setText(getString(R.string.app_name));
                setViewMargins(subtitleHint, dp16, -dp16-dp2, dp16/2, dp16/2);
                subtitleHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            } else {
                hideKeyboard(tv);
                if (tv.getText() == null || tv.getText().toString().isEmpty()) {
                    subtitleHint.setText(getString(R.string.qr_instructions));
                    setViewMargins(subtitleHint, dp16, dp16, dp16, dp16);
                    subtitleHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                }
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

    static Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
//            final MainActivity activity = new MainActivity();
            //Log.i("handleMessage", "stringForQRcode: " + stringForQRcode + msg.obj.toString() + "handler" + activity);
            stringForQRcode = msg.obj.toString();
//            TextView tv = findViewById(R.id.qr_subtitle);
            tv.setText(stringForQRcode);
        }
    };

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);

        String intentType = new StringUtil().getStringType(intent);
        // Grab the data from the new intent and update the textedit
        Object obj = new StringUtil().getStringFromIntent(this, intent);
        if (obj == null) {
            stringForQRcode = null;
        } else if (obj instanceof String) {
            stringForQRcode = obj.toString();
        } else {

            ExecutorService executor = Executors.newSingleThreadExecutor();

            executor.execute(() -> {
                // Background work here
                stringForQRcode = Base64.encodeToString((byte[]) obj,Base64.DEFAULT);
//                Log.i("onNewIntent", "stringForQRcode: " + stringForQRcode + "background");

                Message msg = handler.obtainMessage();
                msg.obj = "data:" + intentType + ";base64," + stringForQRcode;
                handler.sendMessage(msg);
            });
            executor.shutdown();


            //stringForQRcode = "data:" + new StringUtil().getStringType(intent) + ";base64," + Base64.encodeToString((byte[]) obj,Base64.DEFAULT);
            stringForQRcode = App.getRes().getString(R.string.encoding);
            //return "data:" + getStringType(intent) + ";base64," + intentText;
        }
        //stringForQRcode = Base64.encodeToString(stringForQRcode.getBytes(),Base64.DEFAULT);
//        TextView tv = findViewById(R.id.qr_subtitle);
        tv.setText(stringForQRcode);

        // Then update the QR code with the corresponding text
        qr_bitmap = new StringUtil().stringToQRcode(MainActivity.this, stringForQRcode);
        iv.setImageDrawable(roundifyImage(iv, qr_bitmap, dp16/2, MainActivity.this));

        // Move the text type hint out of the way of the text if there's a given text
        setViewMargins(subtitleHint, dp16, -dp16-dp2, dp16/2, dp16/2);
        subtitleHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        subtitleHint.setText(intentType);
        if (tv.getText() != null && !tv.getText().toString().isEmpty()) {
            // Don't move the text type hint if there's no text
            setViewMargins(subtitleHint, dp16, dp16, dp16, dp16);
            subtitleHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            subtitleHint.setText(getString(R.string.qr_instructions));

        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        //menu.setHeaderTitle("Context Menu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_list, menu);
        if (new StringUtil().stringToQRcode(MainActivity.this, stringForQRcode) == null) {
            menu.findItem(R.id.copy).setEnabled(false);
            menu.findItem(R.id.share).setEnabled(false);
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        Uri uriForFile = null;
        if (new StringUtil().stringToQRcode(MainActivity.this, stringForQRcode) != null) {
            // Save the image to the cache
            saveBitmapToCache(qr_bitmap);
            uriForFile = FileProvider.getUriForFile(MainActivity.this,
                    MainActivity.this.getPackageName() + ".provider", cacheFile);
        }

        // Handle the menu item clicks
        int itemId = item.getItemId();
        if (itemId == R.id.copy) {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newUri(getContentResolver(), "Image", uriForFile);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(MainActivity.this, R.string.menu_copy, Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.edit) {
            // Kinda funny, I used this menu option to test the animations for the text view.
            // I didn't expect this that I would have a use for this menu item :)
//            TextView tv = findViewById(R.id.qr_subtitle);
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
            Toast.makeText(MainActivity.this, item.getTitle(), Toast.LENGTH_SHORT).show();
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