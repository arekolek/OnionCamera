
package com.arekolek.onioncamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.Type;

public class Filter {
    private ScriptC_filter filter;
    private Allocation in;
    private Allocation tmp;
    private Allocation out;
    private int size;

    public Filter(Context context, int width, int height) {
        RenderScript rs = RenderScript.create(context);

        Type.Builder typeIn = new Type.Builder(rs, Element.U8(rs)).setX(width).setY(height)
                .setMipmaps(false)
                .setFaces(false);

        Type.Builder typeTmp = new Type.Builder(rs, Element.F32(rs)).setX(width).setY(height)
                .setMipmaps(false).setFaces(false);

        Type.Builder typeOut = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height)
                .setMipmaps(false).setFaces(false);

        in = Allocation.createTyped(rs, typeIn.create(),
                Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT
                        & Allocation.USAGE_SHARED);
        tmp = Allocation.createTyped(rs, typeTmp.create(),
                Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT
                        & Allocation.USAGE_SHARED);
        out = Allocation.createTyped(rs, typeOut.create(),
                Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT
                        & Allocation.USAGE_SHARED);

        filter = new ScriptC_filter(rs);
        filter.set_gScript(filter);
        filter.set_gIn(in);
        filter.set_gTmp(tmp);
        filter.set_gOut(out);
        filter.set_width(width);
        filter.set_height(height);

        size = width * height;
    }

    public byte[] createBuffer() {
        return new byte[size * 12 / 8];
    }

    public void run(byte[] data, Bitmap result) {
        in.copy1DRangeFrom(0, size, data);

        filter.forEach_SobelFirstPass(in, tmp);
        filter.forEach_SobelSecondPass(in, out);

        out.copyTo(result);
    }

    public int getBuffersNum() {
        return 3;
    }
}
