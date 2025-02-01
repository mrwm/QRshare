package com.wchung.qrshare;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private ImageView iv;
    private Bitmap bm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iv = findViewById(R.id.imageViewQRCode);

        Intent intent = getIntent();
        if (intent.hasExtra("EXTRA_BITMAP")) {
            byte[] byteArray = intent.getByteArrayExtra("EXTRA_BITMAP");
            bm = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            iv.setImageBitmap(bm);
        }
    }

    public Bitmap loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        // Convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream;
        try {
            assetInputStream = Tasks.await(Wearable.getDataClient(this).getFdForAsset(asset))
                    .getInputStream();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        // Decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }

}
