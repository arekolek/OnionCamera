
package com.arekolek.onioncamera;

import android.graphics.SurfaceTexture;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Script.LaunchOptions;
import android.renderscript.ScriptIntrinsicHistogram;
import android.renderscript.Type;
import android.view.TextureView;

public class Filter implements TextureView.SurfaceTextureListener {
    private int mWidth;
    private int mHeight;
    private int mSize;
    private RenderScript mRS;
    private Allocation mAllocationIn;
    private Allocation mAllocationMagnitude;
    private Allocation mAllocationBlurred;
    private Allocation mAllocationDirection;
    private Allocation mAllocationEdge;
    private Allocation mAllocationOut;
    private ScriptC_effects mEffects;
    private boolean mHaveSurface;
    private SurfaceTexture mSurface;
    private LaunchOptions sc;
    private ScriptIntrinsicHistogram mHistogram;
    private Allocation mAllocationHistogram;
    private int[] histo;

    public Filter(RenderScript rs) {
        mRS = rs;
        mEffects = new ScriptC_effects(mRS);
        mHistogram = ScriptIntrinsicHistogram.create(mRS, Element.U8(mRS));
    }

    private void setupSurface() {
        if (mSurface != null) {
            if (mAllocationOut != null) {
                // hidden API
                //mAllocationOut.setSurfaceTexture(mSurface);
                setSurfaceTexture(mAllocationOut, mSurface);
            }
            mHaveSurface = true;
        } else {
            mHaveSurface = false;
        }
    }

    public void reset(int width, int height) {
        if (mAllocationOut != null) {
            mAllocationOut.destroy();
        }

        mWidth = width;
        mHeight = height;
        mSize = width * height;

        Type.Builder tb;

        tb = new Type.Builder(mRS, Element.U8(mRS)).setX(mWidth).setY(mHeight);
        mAllocationIn = Allocation.createTyped(mRS, tb.create(), Allocation.USAGE_SCRIPT);

        tb = new Type.Builder(mRS, Element.F32(mRS)).setX(mWidth).setY(mHeight);
        mAllocationBlurred = Allocation.createTyped(mRS, tb.create(), Allocation.USAGE_SCRIPT);
        mAllocationMagnitude = Allocation.createTyped(mRS, tb.create(), Allocation.USAGE_SCRIPT);

        tb = new Type.Builder(mRS, Element.I32(mRS)).setX(mWidth).setY(mHeight);
        mAllocationDirection = Allocation.createTyped(mRS, tb.create(), Allocation.USAGE_SCRIPT);
        mAllocationEdge = Allocation.createTyped(mRS, tb.create(), Allocation.USAGE_SCRIPT);

        tb = new Type.Builder(mRS, Element.I32(mRS)).setX(256);
        mAllocationHistogram = Allocation.createTyped(mRS, tb.create(), Allocation.USAGE_SCRIPT);

        tb = new Type.Builder(mRS, Element.RGBA_8888(mRS)).setX(mWidth).setY(mHeight);
        mAllocationOut = Allocation.createTyped(mRS, tb.create(), Allocation.USAGE_SCRIPT |
                Allocation.USAGE_IO_OUTPUT);

        setupSurface();

        mHistogram.setOutput(mAllocationHistogram);
        mEffects.invoke_set_histogram(mAllocationHistogram);
        mEffects.invoke_set_blur_input(mAllocationIn);
        mEffects.invoke_set_compute_gradient_input(mAllocationBlurred);
        mEffects.invoke_set_suppress_input(mAllocationMagnitude, mAllocationDirection);
        mEffects.invoke_set_hysteresis_input(mAllocationEdge);
        mEffects.invoke_set_thresholds(0.2f, 0.6f);

        sc = new LaunchOptions();
        sc.setX(2, mWidth - 3);
        sc.setY(2, mHeight - 3);

        histo = new int[256];
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void execute(byte[] yuv) {
        if (mHaveSurface) {
            mAllocationIn.copy1DRangeFrom(0, mSize, yuv);
            mHistogram.forEach_Dot(mAllocationIn);
            mAllocationHistogram.copyTo(histo);
            setThresholds();
            mEffects.forEach_blur(mAllocationBlurred, sc);
            mEffects.forEach_compute_gradient(mAllocationMagnitude, sc);
            mEffects.forEach_suppress(mAllocationEdge, sc);
            mEffects.forEach_hysteresis(mAllocationOut, sc);
            //            mEffects.forEach_addhisto(mAllocationIn, mAllocationOut);

            // hidden API
            //mAllocationOut.ioSendOutput();
            ioSendOutput(mAllocationOut);
        }
    }

    private static final float THRESHOLD_MULT_LOW = 0.66f * 0.00390625f;
    private static final float THRESHOLD_MULT_HIGH = 1.33f * 0.00390625f;

    private void setThresholds() {
        int median = mSize / 2;
        for (int i = 0; i < 256; ++i) {
            median -= histo[i];
            if (median < 1) {
                mEffects.invoke_set_thresholds(i * THRESHOLD_MULT_LOW, i * THRESHOLD_MULT_HIGH);
                break;
            }
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mSurface = surface;
        setupSurface();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        mSurface = surface;
        setupSurface();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mSurface = null;
        setupSurface();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    private static void setSurfaceTexture(Allocation allocation, SurfaceTexture surface) {
        try {
            Allocation.class.getMethod("setSurfaceTexture",
                    SurfaceTexture.class).invoke(allocation, surface);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    private static void ioSendOutput(Allocation allocation) {
        try {
            Allocation.class.getMethod("ioSendOutput").invoke(allocation);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}
