package com.wchung.qrshare;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

public class MessageService extends WearableListenerService {
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("MessageService", "onCreate");
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        Log.i("MessageService", "onMessageReceived");
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.i("MessageService", "onDataChanged");
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED &&
                event.getDataItem().getUri().getPath().equals("/image")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Asset qrImageAsset = dataMapItem.getDataMap().getAsset("QR_IMG");
                Bitmap bitmap = loadBitmapFromAsset(qrImageAsset);
                sendBitmapToActivity(bitmap);
            }
        }
    }


    private void sendBitmapToActivity(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("EXTRA_BITMAP", byteArray);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
