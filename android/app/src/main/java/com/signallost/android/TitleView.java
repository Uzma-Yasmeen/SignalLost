package com.signallost.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/**
 * The first thing you see: the name of the game, and a way in.
 *
 * <p>The logo is drawn rather than loaded, for the same reason the characters
 * are text in the desktop build — nothing binary to manage, and it scales to
 * whatever screen it lands on. The torch beam behind the title is the whole
 * game in one image: a cone of light, and everything outside it dark.
 */
@SuppressLint("ViewConstructor")
class TitleView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String greeting;
    private final Runnable onStart;
    private final Runnable onQuit;

    TitleView(Context context, String greeting, Runnable onStart, Runnable onQuit) {
        super(context);
        this.greeting = greeting;
        this.onStart = onStart;
        this.onQuit = onQuit;
        setBackgroundColor(Palette.BACKDROP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Palette.BACKDROP);
        float midX = getWidth() / 2f;
        float midY = getHeight() / 2f;

        drawBeam(canvas, midX, midY);

        paint.setColor(Palette.TEXT);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(getHeight() * 0.17f);
        canvas.drawText("SIGNAL LOST", midX, midY - getHeight() * 0.06f, paint);

        paint.setFakeBoldText(false);
        paint.setColor(Palette.TEXT_DIM);
        paint.setTextSize(getHeight() * 0.045f);
        canvas.drawText("a fog-of-war stealth game", midX, midY + getHeight() * 0.02f, paint);

        if (greeting != null) {
            paint.setColor(Palette.ACCENT);
            paint.setTextSize(getHeight() * 0.04f);
            canvas.drawText(greeting, midX, midY + getHeight() * 0.11f, paint);
        }

        drawButton(canvas, startButton(), "START", Palette.ACCENT, Palette.TEXT);
        drawButton(canvas, quitButton(), "QUIT", Palette.BUTTON_EDGE, Palette.TEXT_DIM);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    /** A cone of light with the dark pressing in, which is the game in one picture. */
    private void drawBeam(Canvas canvas, float midX, float midY) {
        for (int ring = 7; ring >= 1; ring--) {
            paint.setColor(Palette.withAlpha(Palette.ACCENT, 5 + ring * 2));
            canvas.drawCircle(midX, midY - getHeight() * 0.04f,
                    getHeight() * 0.07f * ring, paint);
        }
    }

    private void drawButton(Canvas canvas, RectF button, String label, int edge, int text) {
        paint.setColor(Palette.BUTTON);
        canvas.drawRoundRect(button, button.height() / 2, button.height() / 2, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(edge);
        canvas.drawRoundRect(button, button.height() / 2, button.height() / 2, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(text);
        paint.setTextSize(button.height() * 0.42f);
        paint.setFakeBoldText(true);
        canvas.drawText(label, button.centerX(), button.centerY() + button.height() * 0.15f, paint);
        paint.setFakeBoldText(false);
    }

    /** START and QUIT sit side by side, the same size as each other. */
    private RectF buttonAt(int index) {
        float width = Math.min(getWidth() * 0.26f, 420);
        float height = getHeight() * 0.13f;
        float gap = getWidth() * 0.025f;
        float left = getWidth() / 2f - width - gap / 2 + index * (width + gap);
        float top = getHeight() * 0.74f;
        return new RectF(left, top, left + width, top + height);
    }

    private RectF startButton() {
        return buttonAt(0);
    }

    private RectF quitButton() {
        return buttonAt(1);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) {
            return true;
        }
        // Only the buttons do anything. Starting on a tap anywhere meant a
        // stray touch while reading the screen threw you into name entry.
        if (startButton().contains(event.getX(), event.getY())) {
            performClick();
            onStart.run();
        } else if (quitButton().contains(event.getX(), event.getY())) {
            performClick();
            onQuit.run();
        }
        return true;
    }
}
