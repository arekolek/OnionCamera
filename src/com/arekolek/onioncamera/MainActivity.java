
package com.arekolek.onioncamera;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.renderscript.RenderScript;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends Activity
        implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {

    private static final String TAG = "OnionCamera";

    private TextureView preview;
    private SurfaceTexture surface;
    private int surfaceWidth;
    private int surfaceHeight;

    private CameraWizard wizard;
    private Camera camera;
    private Size cameraSize;

    private TextureView overlay;

    private static final int STATE_OFF = 0;
    private static final int STATE_PREVIEW = 1;
    private static final int STATE_NO_CALLBACKS = 2;
    private int state = STATE_OFF;
    private boolean isProcessing = false;

    private RsYuv filter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preview = (TextureView) findViewById(R.id.preview);
        overlay = (TextureView) findViewById(R.id.overlay);

        preview.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int visibility = overlay.getVisibility() == View.VISIBLE ? View.INVISIBLE
                        : View.VISIBLE;
                overlay.setVisibility(visibility);
            }
        });

        preview.setSurfaceTextureListener(this);

        filter = new RsYuv(RenderScript.create(this));
        overlay.setSurfaceTextureListener(filter);
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        shutdownCamera();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture s,
            int width, int height) {
        surface = s;
        surfaceWidth = width;
        surfaceHeight = height;
        if (camera != null) {
            startPreview();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame
    }

    private void setUpCamera() {
        shutdownCamera();

        wizard = CameraWizard.getBackFacingCamera();
        wizard.setOrientation(getWindowManager().getDefaultDisplay());
        wizard.setAutofocus();
        wizard.setFps();

        camera = wizard.getCamera();

        if (surface != null) {
            startPreview();
        }
    }

    private void shutdownCamera() {
        if (camera != null) {
            camera.setPreviewCallbackWithBuffer(null);
            camera.stopPreview();
            camera.release();
            camera = null;
            state = STATE_OFF;
        }
    }

    private void startPreview() {
        if (state != STATE_OFF) {
            // Stop for a while to drain callbacks
            camera.setPreviewCallbackWithBuffer(null);
            camera.stopPreview();
            state = STATE_OFF;
            Handler h = new Handler();
            Runnable mDelayedPreview = new Runnable() {
                @Override
                public void run() {
                    startPreview();
                }
            };
            h.postDelayed(mDelayedPreview, 300);
            return;
        }
        state = STATE_PREVIEW;

        cameraSize = wizard.setPreviewSize(surfaceWidth, surfaceHeight);

        Matrix transform = new Matrix();
        float widthRatio = cameraSize.width / (float) surfaceWidth;
        float heightRatio = cameraSize.height / (float) surfaceHeight;

        transform.setScale(1, heightRatio / widthRatio);
        transform.postTranslate(0,
                surfaceHeight * (1 - heightRatio / widthRatio) / 2);

        preview.setTransform(transform);
        overlay.setTransform(transform);

        camera.setPreviewCallbackWithBuffer(this);
        int expectedBytes = cameraSize.width * cameraSize.height *
                ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8;
        for (int i = 0; i < 4; i++) {
            camera.addCallbackBuffer(new byte[expectedBytes]);
        }

        try {
            camera.setPreviewTexture(surface);
            camera.startPreview();
        } catch (Exception e) {
            // Something bad happened
            Log.e(TAG, "Unable to start up preview");
        }

    }

    private class ProcessPreviewDataTask extends AsyncTask<byte[], Void, Boolean> {
        @Override
        protected Boolean doInBackground(byte[]... datas) {
            byte[] data = datas[0];
            filter.execute(data);
            camera.addCallbackBuffer(data);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            isProcessing = false;
            overlay.invalidate();
        }

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera c) {
        if (isProcessing || state != STATE_PREVIEW) {
            camera.addCallbackBuffer(data);
            return;
        }
        if (data == null) {
            return;
        }
        isProcessing = true;

        if (filter == null
                || cameraSize.width != filter.getWidth()
                || cameraSize.height != filter.getHeight()) {

            filter.reset(cameraSize.width, cameraSize.height);
        }
        new ProcessPreviewDataTask().execute(data);
    }

}
