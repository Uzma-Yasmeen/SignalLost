package com.signallost.android;

import com.signallost.art.Critter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.function.Consumer;

/**
 * Pick who you are playing as.
 *
 * <p>The six are drawn from {@link com.signallost.art.CritterPixels}, the same
 * sixteen rows the desktop draws, so Mochi is the same cat on both. Nothing
 * about them is restated here — this screen decides how big to draw one and
 * which is currently chosen.
 */
@SuppressLint("ViewConstructor")
class CharacterSelectView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Critter[] roster = Critter.values();
    private final Consumer<Critter> onChosen;
    private final Runnable onBack;
    private Critter chosen;

    CharacterSelectView(Context context, Critter current, Consumer<Critter> onChosen,
                        Runnable onBack) {
        super(context);
        this.chosen = current == null ? Critter.defaultChoice() : current;
        this.onChosen = onChosen;
        this.onBack = onBack;
        setBackgroundColor(Palette.BACKDROP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Palette.BACKDROP);

        paint.setColor(Palette.TEXT);
        paint.setFakeBoldText(true);
        paint.setTextSize(getHeight() * 0.075f);
        canvas.drawText("CHOOSE A CHARACTER", getWidth() * 0.22f, getHeight() * 0.11f, paint);
        paint.setFakeBoldText(false);
        BackArrow.paint(canvas, paint, getWidth(), getHeight(), "NAME");

        for (int i = 0; i < roster.length; i++) {
            drawCard(canvas, i);
        }

        RectF play = playButton();
        paint.setColor(Palette.BUTTON);
        canvas.drawRoundRect(play, play.height() / 2, play.height() / 2, paint);
        paint.setColor(CritterSprite.colourOf(chosen));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        canvas.drawRoundRect(play, play.height() / 2, play.height() / 2, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Palette.TEXT);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(play.height() * 0.42f);
        paint.setFakeBoldText(true);
        canvas.drawText("PLAY AS " + chosen.displayName().toUpperCase(),
                play.centerX(), play.centerY() + play.height() * 0.15f, paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private RectF cardAt(int index) {
        float marginX = getWidth() * 0.04f;
        float gap = getWidth() * 0.015f;
        float cardWidth = (getWidth() - marginX * 2 - gap * (roster.length - 1)) / roster.length;
        float top = getHeight() * 0.18f;
        float cardHeight = getHeight() * 0.52f;
        float left = marginX + index * (cardWidth + gap);
        return new RectF(left, top, left + cardWidth, top + cardHeight);
    }

    private RectF playButton() {
        float width = Math.min(getWidth() * 0.42f, 700);
        float height = getHeight() * 0.14f;
        float top = getHeight() * 0.78f;
        return new RectF((getWidth() - width) / 2, top, (getWidth() + width) / 2, top + height);
    }

    private void drawCard(Canvas canvas, int index) {
        Critter critter = roster[index];
        RectF card = cardAt(index);
        boolean picked = critter == chosen;
        float radius = card.width() * 0.12f;

        paint.setColor(picked ? Palette.PANEL : Palette.withAlpha(Palette.PANEL, 140));
        canvas.drawRoundRect(card, radius, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(picked ? 5 : 2);
        paint.setColor(picked ? CritterSprite.colourOf(critter) : Palette.BUTTON_EDGE);
        canvas.drawRoundRect(card, radius, radius, paint);
        paint.setStyle(Paint.Style.FILL);

        // A glow in the character's own colour, so the chosen one is obvious
        // without a tick or a border doing all the work.
        if (picked) {
            paint.setColor(Palette.withAlpha(CritterSprite.colourOf(critter), 34));
            canvas.drawCircle(card.centerX(), card.top + card.height() * 0.36f,
                    card.width() * 0.44f, paint);
        }

        CritterSprite.paint(canvas, critter, card.centerX(),
                card.top + card.height() * 0.36f, card.width() * 0.68f);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(picked ? Palette.TEXT : Palette.TEXT_DIM);
        paint.setFakeBoldText(true);
        paint.setTextSize(card.height() * 0.13f);
        canvas.drawText(critter.displayName(), card.centerX(),
                card.top + card.height() * 0.75f, paint);

        paint.setFakeBoldText(false);
        paint.setColor(Palette.TEXT_DIM);
        // "soft but stubborn" is twice the width of "all ears", so the size is
        // chosen from the card rather than fixed. At a fixed size the longer
        // taglines ran into their neighbours and read as one word.
        float tagSize = card.height() * 0.085f;
        paint.setTextSize(tagSize);
        float available = card.width() * 0.92f;
        float needed = paint.measureText(critter.tagline());
        if (needed > available) {
            paint.setTextSize(tagSize * available / needed);
        }
        canvas.drawText(critter.tagline(), card.centerX(),
                card.top + card.height() * 0.89f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
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
        performClick();
        if (BackArrow.bounds(getWidth(), getHeight()).contains(event.getX(), event.getY())) {
            onBack.run();
            return true;
        }
        if (playButton().contains(event.getX(), event.getY())) {
            onChosen.accept(chosen);
            return true;
        }
        for (int i = 0; i < roster.length; i++) {
            if (cardAt(i).contains(event.getX(), event.getY())) {
                // Tapping the one already chosen starts the game, so picking
                // and playing is one tap for anybody who likes the default.
                if (roster[i] == chosen) {
                    onChosen.accept(chosen);
                } else {
                    chosen = roster[i];
                    invalidate();
                }
                return true;
            }
        }
        return true;
    }
}
