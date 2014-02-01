
package com.arekolek.onioncamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.ImageView;

import java.io.IOException;

public class Preview extends SurfaceView implements Callback, PreviewCallback {

    private SurfaceHolder holder;
    private Filter filter;
    private Camera camera;
    private ImageView processed;
    private Bitmap bitmap;
    private Object busyLock = new Object();
    private boolean busy;

    public Preview(Context context) {
        super(context);
        init();
    }

    public Preview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Preview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        holder = getHolder();
    }

    public void resume() {
        holder.addCallback(this);
    }

    public void pause() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        holder.removeCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            Activity activity = (Activity) getContext();
            CameraWizard wizard = CameraWizard.getBackFacingCamera();
            wizard.setOrientation(activity.getWindowManager().getDefaultDisplay());
            Size size = wizard.setPreviewSize(getWidth(), getHeight());
            filter = new Filter(activity, size.width, size.height);
            wizard.setBuffers(filter);
            wizard.setAutofocus();
            camera = wizard.getCamera();
            camera.setPreviewCallbackWithBuffer(this);
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
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
