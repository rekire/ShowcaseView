/*
 * Copyright 2014 Alex Curran
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.amlcurran.showcaseview;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Created by curraa01 on 13/10/2013.
 */
class StandardShowcaseDrawer implements ShowcaseDrawer {
    protected final Paint eraserPaint;
    protected final Drawable showcaseDrawable;
    private final Paint basicPaint;
    private final float showcaseRadius;
    protected int backgroundColour;
    protected Point target;

    public StandardShowcaseDrawer(Resources resources) {
        PorterDuffXfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY);
        eraserPaint = new Paint();
        eraserPaint.setColor(0xFFFFFF);
        eraserPaint.setAlpha(0);
        eraserPaint.setXfermode(xfermode);
        eraserPaint.setAntiAlias(true);
        basicPaint = new Paint();
        showcaseRadius = resources.getDimension(R.dimen.showcase_radius);
        showcaseDrawable = resources.getDrawable(R.drawable.cling_bleached);
    }

    @Override
    public void setShowcaseColour(int color) {
        showcaseDrawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
    }

    @Override
    public void drawShowcase(Bitmap buffer, RectF start, RectF end, float progress) {
        Canvas bufferCanvas = new Canvas(buffer);
        target = new Point(
                (int)(end.right - (end.right - end.left) / 2),
                (int)(end.bottom - (end.bottom - end.top) / 2)
        );
        bufferCanvas.drawCircle(target.x, target.y, showcaseRadius, eraserPaint);
        int halfW = getShowcaseWidth() / 2;
        int halfH = getShowcaseHeight() / 2;
        int left = target.x - halfW;
        int top = target.y - halfH;
        showcaseDrawable.setBounds(left, top,
                left + getShowcaseWidth(),
                top + getShowcaseHeight());
        showcaseDrawable.draw(bufferCanvas);
    }

    @Override
    public int getShowcaseWidth() {
        return showcaseDrawable.getIntrinsicWidth();
    }

    @Override
    public int getShowcaseHeight() {
        return showcaseDrawable.getIntrinsicHeight();
    }

    @Override
    public boolean shouldBeenBlocked(int x, int y) {
        // if square
        float xDelta = Math.abs(x - target.x);
        float yDelta = Math.abs(y - target.y);
        double distanceFromFocus = Math.sqrt(Math.pow(xDelta, 2) + Math.pow(yDelta, 2));
        return distanceFromFocus > showcaseRadius;
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