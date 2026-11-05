package com.signallost.android;

import com.signallost.art.Critter;

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
    private final String tally;
    private final int levelCount;
    private final Runnable onStart;
    private final Runnable onQuit;

    TitleView(Context context, String greeting, String tally, int levelCount,
              Runnable onStart, Runnable onQuit) {
        super(context);
        this.greeting = greeting;
        this.tally = tally;
        this.levelCount = levelCount;
        this.onStart = onStart;
        this.onQuit = onQuit;
        setBackgroundColor(Palette.BACKDROP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Palette.backdrop(canvas, paint, getWidth(), getHeight());
        float midX = getWidth() / 2f;
        // The block is laid out as one piece against the screen height, the way
        // the desktop centres its title, so the heading is never stranded.
        // Sized against the smaller of the two dimensions, not the height
        // alone: on a short, wide window a height-sized title runs off both
        // edges, which is exactly what a laptop browser is.
        float unit = Math.min(getHeight(), getWidth() * 0.9f);
        float top = getHeight() * 0.5f - unit * 0.34f;

        drawBeam(canvas, midX, top + unit * 0.14f);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Palette.TEXT);
        paint.setFakeBoldText(true);
        paint.setTextSize(unit * 0.14f);
        canvas.drawText("SIGNAL LOST", midX, top + unit * 0.17f, paint);

        paint.setFakeBoldText(false);
        paint.setColor(Palette.ACCENT);
        paint.setTextSize(unit * 0.042f);
        canvas.drawText("Find the keys. Reach the exit. Do not be seen.",
                midX, top + unit * 0.245f, paint);

        paint.setColor(Palette.TEXT_DIM);
        paint.setTextSize(unit * 0.036f);
        canvas.drawText(levelCount + " levels  ·  three stars each",
                midX, top + unit * 0.3f, paint);

        drawCast(canvas, midX, top + unit * 0.4f, unit);

        if (greeting != null) {
            paint.setColor(Palette.TEXT);
            paint.setFakeBoldText(true);
            paint.setTextSize(unit * 0.042f);
            canvas.drawText(greeting, midX, top + unit * 0.52f, paint);
            paint.setFakeBoldText(false);
            paint.setColor(Palette.KEY);
            paint.setTextSize(unit * 0.036f);
            canvas.drawText(tally, midX, top + unit * 0.565f, paint);
        }

        drawButton(canvas, startButton(), "START", Palette.ACCENT, Palette.TEXT);
        drawButton(canvas, quitButton(), "QUIT", Palette.BUTTON_EDGE, Palette.TEXT_DIM);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    /** A pool of light with the dark pressing in, which is the game in one picture. */
    private void drawBeam(Canvas canvas, float cx, float cy) {
        for (int ring = 7; ring >= 1; ring--) {
            paint.setColor(Palette.withAlpha(Palette.PLAYER, 3 + ring));
            canvas.drawCircle(cx, cy, getHeight() * 0.055f * ring, paint);
        }
    }

    /**
     * The whole cast lined up, as on the desktop.
     *
     * <p>It is the one thing on this screen that is not a word: it says what the
     * game looks like, and that the characters are yours to pick.
     */
    private void drawCast(Canvas canvas, float midX, float centreY, float unit) {
        Critter[] cast = Critter.values();
        float size = unit * 0.115f;
        float gap = size * 0.4f;
        float total = cast.length * size + (cast.length - 1) * gap;
        float x = midX - total / 2 + size / 2;
        for (int i = 0; i < cast.length; i++) {
            PixelArt.critter(canvas, cast[i], x + i * (size + gap), centreY, size);
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
        float top = getHeight() * 0.78f;
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
