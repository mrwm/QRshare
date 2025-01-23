package com.wchung.qrshare;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
//import androidx.appcompat.widget.TooltipCompat;

import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
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

import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.widget.PopupWindow;

public class MainActivity extends AppCompatActivity {
    private BitMatrix bitMatrix = null;
    private Bitmap bitmap = null;
    private TextView tv = null;
    private ImageView iv = null;
    private File cacheFile = new File(getApplicationContext().getCacheDir(), "QR_image.jpg");


    @SuppressLint("WrongThread")
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
        bitMatrix = generateQRCode(data, screenWidth, screenHeight);
        iv = findViewById(R.id.imageViewQRCode);
        iv.setOnClickListener(this::generate_QR);
        // This section takes care of creating and saving the QR code image
        boolean deleted = cacheFile.delete();
        //Log.i("QR test:", "File deleted: " + deleted);
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(cacheFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] bytearray = byteArrayOutputStream.toByteArray();
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

        iv.setImageBitmap(bitmap);
//        if (bitMatrix != null) {
//            setImageQR(bitMatrix);
//        }


        ////// LONG CLICK //////
        this.registerForContextMenu(iv);

        ////// TEXT GENERATION //////
        tv = findViewById(R.id.qr_subtitle);
        TextInputLayout tvh = findViewById(R.id.qr_subtitle_hint);
        if (type != null) {
            tvh.setHint(type);
        }
        tv.setText(data);
        /* TODO:
        - Max size is around 1307? So about 1.307kB?
         */

        //View tp = findViewById(R.id.tool_tip);
        //TooltipCompat.setTooltipText(tv, getString(R.string.tool_tip));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        /*
        Creates the context menu
         */
        super.onCreateContextMenu(menu, view, menuInfo);

        //menu.setHeaderTitle("Context Menu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_list, menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        /*
        Handles context menu clicks
         */

//        // This section takes care of creating and saving the QR code image
//        boolean deleted = cacheFile.delete();
//        //Log.i("QR test:", "File deleted: " + deleted);
//        FileOutputStream fileOutputStream;
//        try {
//            fileOutputStream = new FileOutputStream(cacheFile);
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
//        byte[] bytearray = byteArrayOutputStream.toByteArray();
//        try {
//            fileOutputStream.write(bytearray);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        try {
//            fileOutputStream.flush();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        try {
//            fileOutputStream.close();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        Uri uriForFile = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", cacheFile);
        //Log.i("QR test: Context copy", "onClick uriForFile: " + uriForFile);

        // Now this is the part that handles the context menu clicks
        int itemId = item.getItemId();
        if (itemId == R.id.copy) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newUri(getContentResolver(), "Image", uriForFile);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, R.string.menu_copy, Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.edit) {
            tv.requestFocus();
            showPopupWindow();
            //Toast.makeText(this, R.string.menu_edit, Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.share) {
            Toast.makeText(this, R.string.menu_share, Toast.LENGTH_SHORT).show();
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
            sendIntent.setType("image/jpg");
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent shareIntent = Intent.createChooser(sendIntent, null);
            startActivity(shareIntent);
        } else {
            Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    public void generate_QR(View view) {
        /*
        Onclick handler for hiding the keyboard and generating the QR code
         */
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
        bitMatrix = generateQRCode(tvText, screenWidth, screenHeight);
        if (bitMatrix != null) {
            setImageQR(bitMatrix);
        }
        //Log.i("QR test: generate_QR", "onClick" + view.getId() + " " + R.id.imageViewQRCode);
    }

    private void setImageQR(BitMatrix bitMatrix) {
        /*
        Generates the QR code image from the BitMatrix
         */
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        //bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap = BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
        //bitmap.compress(Bitmap.CompressFormat.PNG, 100, file_output_location);
        //for (int x = 0; x < width; x++) {
        //    for (int y = 0; y < height; y++) {
        //        bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
        //    }
        //}
        iv.setImageBitmap(bitmap);
    }

    private String handleSendText(Intent intent) {
        /*
        Handles text being shared
         */
        if (intent.getStringExtra(Intent.EXTRA_TEXT) == null) {
            Log.w("QR test: handleSendText", "Intent.EXTRA_TEXT is null");
            Log.i("QR test: handleSendText", "Intent: " + intent.toString());
        }
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
            //return new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, width, height, hints);
            return new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, width, height, hints);
        } catch (WriterException ex) {
            Log.e("QRCodeGenerator", "Error generating QR code", ex);
            return null;
        }
    }

    public void showPopupWindow() {
        /*
        Shows the tooltip popup window
         */
        //Log.i("showPopupWindow", view.toString());

        // Create a View object yourself through inflater
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_tooltip, null);

        // Specify the length and width through constants
        int width = WRAP_CONTENT;
        int height = WRAP_CONTENT;

        // Make Inactive Items Outside Of PopupWindow
        boolean focusable = true;

        // Create a window with our parameters
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // Set the popup to underneath the QR code
        popupWindow.showAsDropDown(iv, 0, -30);

        // Handler for clicking on the inactive zone of the window
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                // Close the window when clicked
                popupWindow.dismiss();
                return true;
            }
        });
    }

    public static int getScreenWidth() {
        // maybe... make the QR code the same width as max width of the text box?
        // It might look more cohesive, but looses the appeal of maxing out the screen width
        //int sizeInDp = 488;
        //float scale = Resources.getSystem().getDisplayMetrics().density;
        //int dpAsPixels = (int) (sizeInDp*scale + 0.5f);
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.i("onPause", "leaving... bye~!");
        finish();
    }

}
