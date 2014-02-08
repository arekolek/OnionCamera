/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arekolek.onioncamera;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicColorMatrix;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.view.TextureView;

public class Filter implements TextureView.SurfaceTextureListener {
    private int mHeight;
    private int mWidth;
    private RenderScript mRS;
    private Allocation mAllocationIn;
    private Allocation mAllocationMagnitude;
    private Allocation mAllocationTmp2;
    private Allocation mAllocationDirection;
    private Allocation mAllocationEdge;
    private Allocation mAllocationOut;
    private ScriptIntrinsicYuvToRGB mYuv;
    private ScriptIntrinsicColorMatrix mGray;
    //    private ScriptC_vintage mVintage;
    private ScriptC_effects mEffects;
    //    private ScriptIntrinsicConvolve3x3 mConv3;
    //    private ScriptIntrinsicConvolve5x5 mConv5;
    private boolean mHaveSurface;
    private SurfaceTexture mSurface;

    //    private ScriptGroup mGroup;

    public Filter(RenderScript rs) {
        mRS = rs;
        mYuv = ScriptIntrinsicYuvToRGB.create(rs, Element.RGBA_8888(mRS));
        mGray = ScriptIntrinsicColorMatrix.create(mRS);
        //        mVintage = new ScriptC_vintage(mRS);
        mEffects = new ScriptC_effects(mRS);
        //        mConv3 = ScriptIntrinsicConvolve3x3.create(mRS, Element.U8_4(mRS));
        //        mConv5 = ScriptIntrinsicConvolve5x5.create(mRS, Element.U8_4(mRS));
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

        mHeight = height;
        mWidth = width;

        Type.Builder tb;

        tb = new Type.Builder(mRS, Element.createPixel(mRS, Element.DataType.UNSIGNED_8,
                Element.DataKind.PIXEL_YUV));
        tb.setX(mWidth);
        tb.setY(mHeight);
        tb.setYuvFormat(ImageFormat.NV21);
        mAllocationIn = Allocation.createTyped(mRS, tb.create(), Allocation.USAGE_SCRIPT);

        tb = new Type.Builder(mRS, Element.F32(mRS));
        tb.setX(mWidth);
        tb.setY(mHeight);
        mAllocationMagnitude = Allocation.createTyped(mRS, tb.create(), Allocation.USAGE_SCRIPT);
        mAllocationTmp2 = Allocation.createTyped(mRS, tb.create(), Allocation.USAGE_SCRIPT);

        tb = new Type.Builder(mRS, Element.I32(mRS));
        tb.setX(mWidth);
        tb.setY(mHeight);
        mAllocationDirection = Allocation.createTyped(mRS, tb.create(), Allocation.USAGE_SCRIPT);
        mAllocationEdge = Allocation.createTyped(mRS, tb.create(), Allocation.USAGE_SCRIPT);

        tb = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        tb.setX(mWidth);
        tb.setY(mHeight);
        mAllocationOut = Allocation.createTyped(mRS, tb.create(), Allocation.USAGE_SCRIPT |
                Allocation.USAGE_IO_OUTPUT);

        setupSurface();

        mYuv.setInput(mAllocationIn);
        mGray.setGreyscale();
        mEffects.invoke_set_buffers(mAllocationOut, mAllocationMagnitude, mAllocationTmp2,
                mAllocationDirection, mAllocationEdge);
        mEffects.invoke_set_thresholds(0.01f, 0.3f);
        //        mVintage.invoke_setSize(mWidth, mHeight);
        //        mBlur2.setInput(mAllocationTmp);
        //        mConv3.setInput(mAllocationBuf1);
        //        mConv3.setCoefficients(new float[] {
        //                0, 1, 0,
        //                1, -4, 1,
        //                0, 1, 0
        //        });
        //        mConv5.setInput(mAllocationBuf1);
        //        float[] coeffs = new float[] {
        //                2, 4, 5, 4, 2,
        //                4, 9, 12, 9, 4,
        //                5, 12, 15, 12, 5,
        //                4, 9, 12, 9, 4,
        //                2, 4, 5, 4, 2
        //        };
        //        for (int i = 0; i < 25; ++i) {
        //            coeffs[i] /= 115;
        //        }
        //        mConv5.setCoefficients(coeffs);

        //        ScriptGroup.Builder b = new ScriptGroup.Builder(mRS);
        //        b.addKernel(mYuv.getKernelID());
        //        b.addKernel(mVintage.getKernelID_root());
        //        b.addConnection(tb.create(), mYuv.getKernelID(), mVintage.getKernelID_root());
        //        b.addKernel(mBlur.getKernelID());
        //        b.addConnection(tb.create(), mYuv.getKernelID(), mBlur.getKernelID());
        //        mGroup = b.create();
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void execute(byte[] yuv) {
        if (mHaveSurface) {
            //            mGroup.setOutput(mVintage.getKernelID_root(), mAllocationOut);
            //            mGroup.setOutput(mBlur.getKernelID(), mAllocationOut);
            //            mGroup.execute();
            mAllocationIn.copyFrom(yuv);
            mYuv.forEach(mAllocationOut);
            mGray.forEach(mAllocationOut, mAllocationOut);
            mEffects.forEach_unpack(mAllocationOut, mAllocationMagnitude);
            mEffects.forEach_blur(mAllocationTmp2);
            mEffects.forEach_compute_gradient(mAllocationMagnitude);
            mEffects.forEach_suppress(mAllocationEdge);
            mEffects.forEach_hysteresis(mAllocationOut);
            //mEffects.forEach_pack(mAllocationTmp1, mAllocationOut);
            //            mConv5.forEach(mAllocationOut2);
            //            mVintage.forEach_root(mAllocationTmp, mAllocationOut);
            //            mEffects.forEach_edges(mAllocationOut2);
            //            mConv3.forEach(mAllocationOut);
            //            mConv5.forEach(mAllocationOut);

            // hidden API
            //mAllocationOut.ioSendOutput();
            ioSendOutput(mAllocationOut);
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
