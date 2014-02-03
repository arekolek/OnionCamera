
package com.arekolek.onioncamera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;

import java.io.IOException;

public class Preview extends FrameLayout implements Callback, PreviewCallback, OnTouchListener {

    private SurfaceHolder holder;
    private Filter filter;
    private Camera camera;
    private SurfaceView surface;

    public Preview(Context context) {
        super(context);
    }

    public Preview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Preview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void resume() {
        surface = (SurfaceView) findViewById(R.id.surface);
        holder = surface.getHolder();
        holder.addCallback(this);
        setOnTouchListener(this);
    }

    public void pause() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        holder.removeCallback(this);
        setOnTouchListener(null);
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
            wizard.setFps();
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
        camera.addCallbackBuffer(data);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        Log.d("OnionCamera", "TACZI TACZI");
        return false;
    }

}
