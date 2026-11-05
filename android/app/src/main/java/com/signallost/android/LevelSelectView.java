package com.signallost.android;

import com.signallost.level.LevelCatalog;
import com.signallost.progress.PlayerProfile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;
import java.util.function.Consumer;

/**
 * Ten boxes, one per level, locked until enough stars are earned.
 *
 * <p>The catalogue, the unlock costs and the stars all come from the game's own
 * classes. This screen decides how big to draw a box and nothing else.
 */
@SuppressLint("ViewConstructor")
class LevelSelectView extends View {

    private static final int COLUMNS = 5;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<LevelCatalog.Entry> levels = LevelCatalog.load();
    private final PlayerProfile profile;
    private final Consumer<String> onChosen;
    private final Runnable onBack;

    LevelSelectView(Context context, PlayerProfile profile, Consumer<String> onChosen,
                    Runnable onBack) {
        super(context);
        this.profile = profile;
        this.onChosen = onChosen;
        this.onBack = onBack;
        setBackgroundColor(Palette.BACKDROP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Palette.backdrop(canvas, paint, getWidth(), getHeight());

        paint.setColor(Palette.TEXT);
        paint.setFakeBoldText(true);
        paint.setTextSize(getHeight() * 0.075f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("CHOOSE A LEVEL", getWidth() / 2f, getHeight() * 0.11f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        BackArrow.paint(canvas, paint, getWidth(), getHeight());
        paint.setColor(Palette.TEXT);
        paint.setFakeBoldText(true);
        paint.setTextSize(getHeight() * 0.075f);

        paint.setFakeBoldText(false);
        paint.setColor(Palette.TEXT_DIM);
        paint.setTextSize(getHeight() * 0.045f);
        String tally = profile.name() + "   ·   " + profile.totalStars()
                + " of " + (levels.size() * 3) + " stars";
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(tally, getWidth() / 2f, getHeight() * 0.17f, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        for (int i = 0; i < levels.size(); i++) {
            drawBox(canvas, i);
        }
    }

    private RectF boxAt(int index) {
        float marginX = getWidth() * 0.04f;
        float top = getHeight() * 0.23f;
        float gap = getWidth() * 0.015f;
        float boxWidth = (getWidth() - marginX * 2 - gap * (COLUMNS - 1)) / COLUMNS;
        float boxHeight = (getHeight() * 0.74f - gap) / 2;

        int row = index / COLUMNS;
        int col = index % COLUMNS;
        float left = marginX + col * (boxWidth + gap);
        float boxTop = top + row * (boxHeight + gap);
        return new RectF(left, boxTop, left + boxWidth, boxTop + boxHeight);
    }

    private void drawBox(Canvas canvas, int index) {
        LevelCatalog.Entry level = levels.get(index);
        boolean unlocked = profile.totalStars() >= level.starsToUnlock();
        int stars = profile.starsOn(level.name());
        RectF box = boxAt(index);
        float radius = box.width() * 0.12f;

        paint.setColor(unlocked ? Palette.PANEL : Palette.BACKDROP);
        canvas.drawRoundRect(box, radius, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(stars > 0 ? Palette.ACCENT : (unlocked ? Palette.BUTTON_EDGE : Palette.LOCKED));
        canvas.drawRoundRect(box, radius, radius, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(unlocked ? Palette.TEXT : Palette.LOCKED);
        paint.setFakeBoldText(true);
        paint.setTextSize(box.height() * 0.34f);
        canvas.drawText(String.valueOf(index + 1), box.centerX(),
                box.centerY() + box.height() * 0.08f, paint);
        paint.setFakeBoldText(false);

        if (unlocked) {
            drawStars(canvas, box, stars);
        } else {
            paint.setColor(Palette.LOCKED);
            paint.setTextSize(box.height() * 0.13f);
            canvas.drawText(level.starsToUnlock() + "★ to unlock",
                    box.centerX(), box.bottom - box.height() * 0.1f, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawStars(Canvas canvas, RectF box, int earned) {
        float size = box.width() * 0.11f;
        float gap = size * 0.7f;
        float y = box.top + box.height() * 0.22f;
        float startX = box.centerX() - (size * 2 + gap);
        for (int i = 0; i < 3; i++) {
            paint.setColor(i < earned ? Palette.KEY : Palette.LOCKED);
            drawStar(canvas, startX + i * (size * 2 + gap) * 0.5f + size, y, size);
        }
    }

    private void drawStar(Canvas canvas, float cx, float cy, float radius) {
        Path star = new Path();
        for (int point = 0; point < 10; point++) {
            double angle = Math.PI / 5 * point - Math.PI / 2;
            float length = (point % 2 == 0) ? radius : radius * 0.45f;
            float x = cx + (float) (Math.cos(angle) * length);
            float y = cy + (float) (Math.sin(angle) * length);
            if (point == 0) {
                star.moveTo(x, y);
            } else {
                star.lineTo(x, y);
            }
        }
        star.close();
        canvas.drawPath(star, paint);
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
        for (int i = 0; i < levels.size(); i++) {
            LevelCatalog.Entry level = levels.get(i);
            if (boxAt(i).contains(event.getX(), event.getY())
                    && profile.totalStars() >= level.starsToUnlock()) {
                onChosen.accept(level.name());
                return true;
            }
        }
        return true;
    }
}
