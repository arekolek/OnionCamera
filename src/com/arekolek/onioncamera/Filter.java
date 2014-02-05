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
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.view.TextureView;

public class Filter implements TextureView.SurfaceTextureListener {
    private int mHeight;
    private int mWidth;
    private RenderScript mRS;
    private Allocation mAllocationIn;
    private Allocation mAllocationTmp;
    private Allocation mAllocationOut;
    private ScriptIntrinsicYuvToRGB mYuv;
    //    private ScriptC_vintage mVintage;
    private ScriptC_effects mEffects;
    //    private ScriptIntrinsicBlur mBlur2;
    private boolean mHaveSurface;
    private SurfaceTexture mSurface;

    //    private ScriptGroup mGroup;

    public Filter(RenderScript rs) {
        mRS = rs;
        mYuv = ScriptIntrinsicYuvToRGB.create(rs, Element.RGBA_8888(mRS));
        //        mVintage = new ScriptC_vintage(mRS);
        mEffects = new ScriptC_effects(mRS);
        //        mBlur2 = ScriptIntrinsicBlur.create(mRS, Element.U8_4(mRS));
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

        tb = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        tb.setX(mWidth);
        tb.setY(mHeight);
        mAllocationTmp = Allocation.createTyped(mRS, tb.create(), Allocation.USAGE_SCRIPT);
        mAllocationOut = Allocation.createTyped(mRS, tb.create(), Allocation.USAGE_SCRIPT |
                Allocation.USAGE_IO_OUTPUT);

        setupSurface();

        mYuv.setInput(mAllocationIn);
        //        mVintage.invoke_setSize(mWidth, mHeight);
        mEffects.invoke_set_input(mAllocationTmp);
        //        mBlur2.setInput(mAllocationTmp);

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
            mYuv.forEach(mAllocationTmp);
            //            mVintage.forEach_root(mAllocationTmp, mAllocationOut);
            mEffects.forEach_edges(mAllocationOut);
            //            mBlur2.forEach(mAllocationOut);

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
