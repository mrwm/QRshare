package com.wchung.qrshare;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;

public class PopupActivity extends AppCompatActivity {    private ImageView qrImageView;
    private String sharedText;
    private Bitmap qrBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set dialog appearance without title
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.popup);
        
        // Set popup window size to be smaller than screen
        WindowManager.LayoutParams params = getWindow().getAttributes();
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        params.width = (int)(size.x * 0.85); // 85% of screen width
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.dimAmount = 0.5f; // Background dimming
        getWindow().setAttributes(params);
        
        // Enable outside touch to dismiss
        setFinishOnTouchOutside(true);        // Initialize views
        qrImageView = findViewById(R.id.popup_qr_image);
        FloatingActionButton editButton = findViewById(R.id.popup_edit_button);
        
        // Get shared text from intent
        sharedText = new StringUtil().getStringFromIntent(this, getIntent());
        
        // Generate QR code
        if (sharedText != null && !sharedText.isEmpty()) {
            qrBitmap = new StringUtil().stringToQRcode(sharedText);
            qrImageView.setImageBitmap(qrBitmap);
        }
        
        // Set up edit button click listener
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch MainActivity with the shared text for editing
                Intent editIntent = new Intent(PopupActivity.this, MainActivity.class);
                editIntent.setAction(Intent.ACTION_SEND);
                editIntent.putExtra(Intent.EXTRA_TEXT, sharedText);
                editIntent.setType("text/plain");
                startActivity(editIntent);
                
                // Close the popup
                finish();
            }
        });
    }
}
