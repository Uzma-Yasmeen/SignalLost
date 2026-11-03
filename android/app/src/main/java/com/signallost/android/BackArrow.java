package com.signallost.android;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * The way out of a screen, drawn the same on every one of them.
 *
 * <p>There was only the hardware back gesture, which on a phone held sideways
 * is a swipe from an edge your palm is already resting on. Anything a player
 * needs in order to leave should be visible.
 */
final class BackArrow {

    private BackArrow() {
    }

    /** Where the arrow sits, and the area that counts as pressing it. */
    static RectF bounds(int width, int height) {
        float size = height * 0.115f;
        float margin = height * 0.045f;
        return new RectF(margin, margin, margin + size * 1.6f, margin + size);
    }

    static void paint(Canvas canvas, Paint paint, int width, int height, String label) {
        RectF box = bounds(width, height);

        paint.setColor(Palette.BUTTON);
        canvas.drawRoundRect(box, box.height() * 0.32f, box.height() * 0.32f, paint);

        // A chevron rather than a glyph, so it needs no font to be present.
        float cx = box.left + box.height() * 0.55f;
        float cy = box.centerY();
        float arm = box.height() * 0.2f;
        Path chevron = new Path();
        chevron.moveTo(cx + arm * 0.5f, cy - arm);
        chevron.lineTo(cx - arm * 0.5f, cy);
        chevron.lineTo(cx + arm * 0.5f, cy + arm);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(3f, box.height() * 0.1f));
        paint.setColor(Palette.TEXT);
        canvas.drawPath(chevron, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Palette.TEXT_DIM);
        paint.setTextSize(box.height() * 0.38f);
        canvas.drawText(label, cx + arm * 1.2f, cy + box.height() * 0.14f, paint);
    }
}
