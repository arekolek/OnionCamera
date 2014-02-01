
package com.arekolek.onioncamera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;

public class MainActivity extends Activity implements Callback, PreviewCallback {

    static final String TAG = "OnionCamera";

    private Camera camera;

    private SurfaceView preview;

    private SurfaceHolder holder;

    private ImageView processed;

    private Bitmap bitmap;

    private Filter filter;

    private Object busyLock = new Object();

    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        processed = (ImageView) findViewById(R.id.processed);
        preview = (SurfaceView) findViewById(R.id.preview);
        holder = preview.getHolder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        holder.addCallback(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        holder.removeCallback(this);
    }

    public void showRaw(View view) {
        processed.setVisibility(View.GONE);
    }

    public void showProcessed(View view) {
        processed.setVisibility(View.VISIBLE);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            CameraWizard wizard = CameraWizard.getBackFacingCamera();
            wizard.setOrientation(getWindowManager().getDefaultDisplay());
            Size size = wizard.setPreviewSize(preview.getWidth(), preview.getHeight());
            filter = new Filter(this, size.width, size.height);
            bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888);
            processed.setImageBitmap(bitmap);
            wizard.setBuffers(filter);
            camera = wizard.getCamera();
            camera.setPreviewCallbackWithBuffer(this);
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            camera.stopPreview();
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        synchronized (busyLock) {
            if (busy) {
                camera.addCallbackBuffer(data);
                return;
            }
            busy = true;
        }
        try {
            filter.run(data, bitmap);
            processed.invalidate();
        } catch (Exception e) {
        }
        camera.addCallbackBuffer(data);
        busy = false;
    }

}
