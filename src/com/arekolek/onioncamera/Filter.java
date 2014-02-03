
package com.arekolek.onioncamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Type;

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

    public void run(byte[] data, Bitmap result) {
        in.copy1DRangeFrom(0, size, data);

        filter.forEach_SobelFirstPass(in, tmp);
        filter.forEach_SobelSecondPass(in, out);

        out.copyTo(result);
    }

}
