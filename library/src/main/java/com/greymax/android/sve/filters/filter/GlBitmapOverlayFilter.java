package com.greymax.android.sve.filters.filter;

import android.graphics.Bitmap;
import android.graphics.Canvas;

/**
 * Created by thinkpad on 2018/5/23.
 */

public class GlBitmapOverlayFilter extends GlOverlayFilter {

    private Bitmap bitmap;

    public GlBitmapOverlayFilter(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    @Override
    protected void drawCanvas(Canvas canvas) {

        canvas.drawBitmap(bitmap, 0, 0, null);

    }
}
