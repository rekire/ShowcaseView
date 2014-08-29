/*
 * Copyright (c) 2014 TOMORROW FOCUS News+ GmbH. All rights reserved.
 */

package com.github.amlcurran.showcaseview;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;


/**
 * A highly flexible showcase drawer which displays a circle for square views and rectangle of other.
 * Created on 28.08.2014.
 *
 * @author René Kilczan
 */
class FancyShowcaseDrawer implements ShowcaseDrawer {
    private static final int ALPHA_60_PERCENT = 153;
    private final float ringSpacing;
    private final float minimalRadius;
    private final Paint eraserPaint;
    private final Paint basicPaint;

    private int endWidth, endHeight;
    private RectF targetRect;
    private int backgroundColour;

    public FancyShowcaseDrawer(Resources resources) {
        PorterDuffXfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY);
        eraserPaint = new Paint();
        eraserPaint.setColor(0xFFFFFF);
        eraserPaint.setAlpha(0);
        eraserPaint.setXfermode(xfermode);
        eraserPaint.setAntiAlias(true);
        basicPaint = new Paint();
        ringSpacing = resources.getDimension(R.dimen.showcase_ring_spacing);
        minimalRadius = resources.getDimension(R.dimen.showcase_minimal_radius);
    }

    @Override
    public void setShowcaseColour(int color) {
        eraserPaint.setColor(color);
    }

    @Override
    public void drawShowcase(Bitmap buffer, RectF start, RectF end, float progress) {
        Canvas bufferCanvas = new Canvas(buffer);
        eraserPaint.setAlpha(ALPHA_60_PERCENT);
        float startRadius = getRadius(start);
        float endRadius = getRadius(end);
        targetRect = end;
        endWidth = (int)(end.right - end.left + ringSpacing * 2);
        endHeight = (int)(end.bottom - end.top + ringSpacing * 2);
        RectF inner;
        float innerRadius;
        if(start == null) {
            inner = end;
            innerRadius = endRadius;
        } else {
            inner = new RectF(start.left - (start.left - end.left) * progress,
                    start.top - (start.top - end.top) * progress,
                    start.right - (start.right - end.right) * progress,
                    start.bottom - (start.bottom - end.bottom) * progress
            );
            innerRadius = startRadius - (startRadius - endRadius) * progress;
        }
        RectF outer = new RectF(inner.left - ringSpacing,
                inner.top - ringSpacing,
                inner.right + ringSpacing,
                inner.bottom + ringSpacing);
        float outerRadius = innerRadius + ringSpacing;
        bufferCanvas.drawRoundRect(outer, outerRadius, outerRadius, eraserPaint);
        eraserPaint.setAlpha(0);
        bufferCanvas.drawRoundRect(inner, innerRadius, innerRadius, eraserPaint);
    }

    private float getRadius(RectF rect) {
        if(rect == null) {
            return 0;
        }

        // TODO there should be also a case for huge square views which should been treated as rect.

        float width = rect.right - rect.left;
        float height = rect.bottom - rect.top;
        float relation;
        if(width < height) {
            relation = width / height;
        } else {
            relation = height / width;
        }

        if(relation >= 0.9f) { // counts as square
            if(width < height) {
                return height / 2f;
            } else {
                return width / 2f;
            }
        } else {
            return minimalRadius;
        }
    }

    @Override
    public int getShowcaseWidth() {
        return endWidth;
    }

    @Override
    public int getShowcaseHeight() {
        return endHeight;
    }

    @Override
    public boolean shouldBeenBlocked(int x, int y) {
        return !targetRect.contains(x, y);
    }

    @Override
    public void setBackgroundColour(int backgroundColor) {
        this.backgroundColour = backgroundColor;
    }

    @Override
    public void erase(Bitmap bitmapBuffer) {
        if(bitmapBuffer != null) {
            bitmapBuffer.eraseColor(backgroundColour);
        }
    }

    @Override
    public void drawToCanvas(Canvas canvas, Bitmap bitmapBuffer) {
        canvas.drawBitmap(bitmapBuffer, 0, 0, basicPaint);
    }
}