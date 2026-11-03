package com.signallost.android;

import com.signallost.art.Critter;
import com.signallost.guard.Guard;
import com.signallost.guard.GuardMode;
import com.signallost.level.Level;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameMap;
import com.signallost.model.GameState;
import com.signallost.model.Position;
import com.signallost.model.Score;
import com.signallost.model.TileType;
import com.signallost.vision.TileVisibility;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * The game, laid out like the desktop: a status bar along the top, the board in
 * the middle, the controls along the bottom.
 *
 * <p>It had a panel down one side, which fitted the board better — in landscape
 * the board is limited by height, so taking width costs nothing. It was still
 * wrong: in landscape your hands are at the left and right edges, so a column
 * of buttons sits exactly where a thumb already rests and gets pressed by
 * accident. Top and bottom are the two edges nobody is holding.
 */
@SuppressLint("ViewConstructor")
class BoardView extends View {

    // The board is limited by height in landscape, so every pixel these bars
    // take comes straight off the tiles. They are as thin as a reliable touch
    // target allows: the footer still clears the 48dp minimum, and the header
    // carries no buttons so it can be thinner still.
    private static final float HEADER = 0.085f;
    private static final float FOOTER = 0.125f;

    private static final int UNDO = 0;
    private static final int HINT = 1;
    private static final int RESTART = 2;
    private static final int LEVELS = 3;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Critter critter;
    private final BiConsumer<String, Score> onFinished;
    private final Runnable onLeave;
    private final Runnable onNext;

    private String levelName;
    private GameState state;
    private int hintTurn = -1;
    private boolean recorded;
    private float pressX;
    private float pressY;

    BoardView(Context context, String levelName, Critter critter,
              BiConsumer<String, Score> onFinished, Runnable onLeave, Runnable onNext) {
        super(context);
        this.critter = critter;
        this.onFinished = onFinished;
        this.onLeave = onLeave;
        this.onNext = onNext;
        setBackgroundColor(Palette.BACKDROP);
        load(levelName);
    }

    private void load(String name) {
        Level level = LevelLoader.loadFromResource(name);
        this.levelName = name;
        this.state = new GameState(level.map(), level.guards(),
                GameState.DEFAULT_SIGHT_RADIUS, level.turnLimit());
        this.hintTurn = -1;
        this.recorded = false;
    }

    GameState state() {
        return state;
    }

    // --------------------------------------------------------------- layout

    private float headerBottom() {
        return getHeight() * HEADER;
    }

    private float footerTop() {
        return getHeight() * (1 - FOOTER);
    }

    private int tileSize() {
        GameMap map = state.map();
        float usableHeight = footerTop() - headerBottom();
        return (int) Math.min(getWidth() / (float) map.cols(), usableHeight / map.rows());
    }

    private float boardLeft() {
        return (getWidth() - tileSize() * state.map().cols()) / 2f;
    }

    private float boardTop() {
        return headerBottom()
                + (footerTop() - headerBottom() - tileSize() * state.map().rows()) / 2f;
    }

    private RectF tileAt(Position at) {
        int tile = tileSize();
        float left = boardLeft() + at.col() * tile;
        float top = boardTop() + at.row() * tile;
        return new RectF(left + 1, top + 1, left + tile - 1, top + tile - 1);
    }

    // ----------------------------------------------------------------- draw

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Palette.BACKDROP);
        GameMap map = state.map();

        for (int row = 0; row < map.rows(); row++) {
            for (int col = 0; col < map.cols(); col++) {
                drawTile(canvas, map, new Position(row, col));
            }
        }
        drawGuardSight(canvas);
        drawGuards(canvas);
        if (hintTurn == state.turnsTaken()) {
            drawHint(canvas);
        }
        drawPlayer(canvas);
        drawHeader(canvas);
        drawFooter(canvas);
        drawVerdict(canvas);
    }

    private void drawTile(Canvas canvas, GameMap map, Position at) {
        TileVisibility seen = state.fog().visibilityOf(at);
        if (seen == TileVisibility.UNEXPLORED) {
            return;
        }
        boolean lit = seen == TileVisibility.VISIBLE;
        TileType type = map.tileAt(at);
        RectF bounds = tileAt(at);
        int tile = tileSize();

        paint.setColor(type == TileType.WALL
                ? (lit ? Palette.WALL_SEEN : Palette.WALL_REMEMBERED)
                : (lit ? Palette.FLOOR_SEEN : Palette.FLOOR_REMEMBERED));
        canvas.drawRoundRect(bounds, tile * 0.16f, tile * 0.16f, paint);

        if (!lit) {
            return;
        }
        if (type == TileType.KEY) {
            paint.setColor(Palette.KEY);
            drawKey(canvas, bounds, tile);
        } else if (type == TileType.EXIT) {
            paint.setColor(state.hasAllKeys() ? Palette.EXIT_OPEN : Palette.EXIT_SHUT);
            float inset = tile * 0.2f;
            canvas.drawRoundRect(new RectF(bounds.left + inset, bounds.top + inset,
                    bounds.right - inset, bounds.bottom - inset), tile * 0.1f, tile * 0.1f, paint);
            paint.setColor(Palette.BACKDROP);
            canvas.drawCircle(bounds.centerX() + tile * 0.1f, bounds.centerY(), tile * 0.05f, paint);
        }
    }

    private void drawKey(Canvas canvas, RectF bounds, int tile) {
        float cx = bounds.centerX();
        float cy = bounds.centerY();
        canvas.drawCircle(cx - tile * 0.1f, cy, tile * 0.13f, paint);
        canvas.drawRect(cx - tile * 0.02f, cy - tile * 0.04f,
                cx + tile * 0.22f, cy + tile * 0.04f, paint);
        canvas.drawRect(cx + tile * 0.13f, cy, cx + tile * 0.18f, cy + tile * 0.14f, paint);
    }

    private void drawGuardSight(Canvas canvas) {
        int tile = tileSize();
        for (Guard guard : state.guards()) {
            if (!state.fog().isVisible(guard.position())) {
                continue;
            }
            paint.setColor(guard.mode() == GuardMode.HUNT
                    ? Palette.withAlpha(Palette.DANGER, 150) : Palette.WATCHED);
            for (Position watched : guard.watchedTiles(state.map())) {
                if (state.fog().visibilityOf(watched) == TileVisibility.VISIBLE) {
                    canvas.drawRoundRect(tileAt(watched), tile * 0.16f, tile * 0.16f, paint);
                }
            }
        }
    }

    private void drawGuards(Canvas canvas) {
        int tile = tileSize();
        for (Guard guard : state.guards()) {
            if (!state.fog().isVisible(guard.position())) {
                continue;
            }
            RectF bounds = tileAt(guard.position());
            int colour = colourOf(guard.mode());
            paint.setColor(Palette.withAlpha(colour, 50));
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), tile * 0.6f, paint);
            paint.setColor(colour);
            float inset = tile * 0.14f;
            canvas.drawRoundRect(new RectF(bounds.left + inset, bounds.top + inset,
                    bounds.right - inset, bounds.bottom - inset), tile * 0.2f, tile * 0.2f, paint);
            paint.setColor(Palette.BACKDROP);
            canvas.drawCircle(bounds.centerX() - tile * 0.09f, bounds.centerY() - tile * 0.03f,
                    tile * 0.06f, paint);
            canvas.drawCircle(bounds.centerX() + tile * 0.09f, bounds.centerY() - tile * 0.03f,
                    tile * 0.06f, paint);
        }
    }

    private static int colourOf(GuardMode mode) {
        return switch (mode) {
            case HUNT -> Palette.GUARD_HUNT;
            case ALERT -> Palette.GUARD_ALERT;
            case PATROL -> Palette.GUARD_PATROL;
        };
    }

    private void drawHint(Canvas canvas) {
        List<Position> route = state.hintRoute(1);
        if (route.isEmpty()) {
            return;
        }
        RectF bounds = tileAt(route.get(0));
        paint.setColor(state.hintSeeksCover() ? Palette.GUARD_ALERT : Palette.HINT);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(tileSize() * 0.11f);
        canvas.drawRoundRect(bounds, tileSize() * 0.16f, tileSize() * 0.16f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawPlayer(Canvas canvas) {
        int tile = tileSize();
        RectF bounds = tileAt(state.player());
        paint.setColor(Palette.withAlpha(CritterSprite.colourOf(critter), 45));
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), tile * 0.7f, paint);
        CritterSprite.paint(canvas, critter, bounds.centerX(), bounds.centerY(), tile * 0.95f);
    }

    // --------------------------------------------------------- header, footer

    private void drawHeader(Canvas canvas) {
        float bottom = headerBottom();
        paint.setColor(Palette.PANEL);
        canvas.drawRect(0, 0, getWidth(), bottom, paint);
        paint.setColor(Palette.withAlpha(Palette.ACCENT, 60));
        canvas.drawRect(0, bottom - 2, getWidth(), bottom, paint);

        float pad = getWidth() * 0.02f;
        float baseline = bottom * 0.66f;

        CritterSprite.paint(canvas, critter, pad + bottom * 0.3f, bottom * 0.5f, bottom * 0.62f);

        paint.setColor(Palette.TEXT);
        paint.setFakeBoldText(true);
        paint.setTextSize(bottom * 0.42f);
        canvas.drawText(prettyLevel(), pad + bottom * 0.7f, baseline, paint);
        paint.setFakeBoldText(false);

        // The numbers run right to left from the far edge, so the level name
        // on the left never collides with them however long it gets.
        float x = getWidth() - pad;
        x = chip(canvas, x, "TURNS " + (state.hasTurnLimit()
                ? state.turnsRemaining() : state.turnsTaken()), Palette.TEXT_DIM, baseline);
        x = chip(canvas, x, "SEEN " + state.sightings(),
                state.sightings() > 0 ? Palette.DANGER : Palette.TEXT_DIM, baseline);
        x = chip(canvas, x, "HINTS " + state.hintsLeft(), Palette.TEXT_DIM, baseline);
        x = chip(canvas, x, "LIVES " + state.livesLeft(),
                state.livesLeft() <= 1 ? Palette.DANGER : Palette.TEXT_DIM, baseline);
        chip(canvas, x, "KEYS " + state.keysHeld() + "/" + state.keysRequired(),
                state.hasAllKeys() ? Palette.EXIT_OPEN : Palette.KEY, baseline);
    }

    /** One right-aligned label; returns the left edge for the next one along. */
    private float chip(Canvas canvas, float rightEdge, String label, int colour, float baseline) {
        paint.setTextSize(headerBottom() * 0.3f);
        paint.setColor(colour);
        float width = paint.measureText(label);
        canvas.drawText(label, rightEdge - width, baseline, paint);
        return rightEdge - width - getWidth() * 0.022f;
    }

    private void drawFooter(Canvas canvas) {
        float top = footerTop();
        paint.setColor(Palette.PANEL);
        canvas.drawRect(0, top, getWidth(), getHeight(), paint);
        paint.setColor(Palette.withAlpha(Palette.ACCENT, 60));
        canvas.drawRect(0, top, getWidth(), top + 2, paint);

        String[] labels = {"UNDO", "HINT  " + state.hintsLeft(), "RESTART", "LEVELS"};
        for (int i = 0; i < labels.length; i++) {
            RectF button = buttonAt(i);
            boolean usable = switch (i) {
                case UNDO -> state.canUndo();
                case HINT -> state.canSpendHint();
                default -> true;
            };
            paint.setColor(Palette.BUTTON);
            canvas.drawRoundRect(button, button.height() * 0.32f, button.height() * 0.32f, paint);
            paint.setColor(usable ? Palette.TEXT : Palette.LOCKED);
            paint.setTextSize(button.height() * 0.38f);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(labels[i], button.centerX(),
                    button.centerY() + button.height() * 0.14f, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }

    private RectF buttonAt(int index) {
        float pad = getWidth() * 0.02f;
        float gap = getWidth() * 0.012f;
        float width = (getWidth() - pad * 2 - gap * 3) / 4;
        float top = footerTop() + getHeight() * 0.025f;
        float bottom = getHeight() - getHeight() * 0.025f;
        float left = pad + index * (width + gap);
        return new RectF(left, top, left + width, bottom);
    }

    private String prettyLevel() {
        return levelName.replaceAll("(?<=\\D)(?=\\d)", " ").toUpperCase();
    }

    // -------------------------------------------------------------- verdict

    private boolean verdictShowing() {
        return state.hasEscaped() || state.isRunOver() || state.hasFailed();
    }

    private RectF verdictButton(boolean left) {
        RectF banner = verdictBanner();
        float width = banner.width() * 0.38f;
        float height = banner.height() * 0.26f;
        float y = banner.bottom - height - banner.height() * 0.09f;
        float x = left ? banner.centerX() - width - banner.width() * 0.02f
                : banner.centerX() + banner.width() * 0.02f;
        return new RectF(x, y, x + width, y + height);
    }

    private RectF verdictBanner() {
        float width = Math.min(getWidth() * 0.5f, 1100);
        float height = getHeight() * 0.56f;
        return new RectF((getWidth() - width) / 2, (getHeight() - height) / 2,
                (getWidth() + width) / 2, (getHeight() + height) / 2);
    }

    private void drawVerdict(Canvas canvas) {
        if (!verdictShowing() && !state.isCaught()) {
            return;
        }
        if (state.isCaught() && !state.isRunOver()) {
            // A lost life is a nudge, not a full stop.
            paint.setColor(Palette.withAlpha(Palette.DANGER, 220));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(getHeight() * 0.05f);
            canvas.drawText("CAUGHT  ·  " + state.livesLeft() + " lives left  ·  tap to carry on",
                    getWidth() / 2f, headerBottom() + getHeight() * 0.08f, paint);
            paint.setTextAlign(Paint.Align.LEFT);
            return;
        }

        RectF banner = verdictBanner();
        paint.setColor(Palette.withAlpha(Palette.BACKDROP, 243));
        canvas.drawRoundRect(banner, 30, 30, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(state.hasEscaped() ? Palette.EXIT_OPEN : Palette.DANGER);
        canvas.drawRoundRect(banner, 30, 30, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(state.hasEscaped() ? Palette.EXIT_OPEN : Palette.DANGER);
        paint.setFakeBoldText(true);
        paint.setTextSize(banner.height() * 0.19f);
        canvas.drawText(state.hasEscaped() ? "ESCAPED" : (state.isRunOver() ? "RUN OVER"
                : "OUT OF TURNS"), banner.centerX(), banner.top + banner.height() * 0.26f, paint);
        paint.setFakeBoldText(false);

        if (state.hasEscaped()) {
            Score score = state.score();
            drawStars(canvas, banner, score.stars());
            paint.setColor(Palette.TEXT_DIM);
            paint.setTextSize(banner.height() * 0.082f);
            canvas.drawText(score.turns() + " turns, par " + score.par()
                            + "   ·   seen " + score.sightings(),
                    banner.centerX(), banner.top + banner.height() * 0.58f, paint);
        } else {
            paint.setColor(Palette.TEXT_DIM);
            paint.setTextSize(banner.height() * 0.082f);
            canvas.drawText("better luck next time",
                    banner.centerX(), banner.top + banner.height() * 0.45f, paint);
        }

        drawVerdictButton(canvas, verdictButton(true), "LEVELS", Palette.BUTTON);
        drawVerdictButton(canvas, verdictButton(false),
                state.hasEscaped() ? "CONTINUE" : "RETRY",
                state.hasEscaped() ? Palette.EXIT_OPEN : Palette.DANGER);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawVerdictButton(Canvas canvas, RectF button, String label, int edge) {
        paint.setColor(Palette.BUTTON);
        canvas.drawRoundRect(button, button.height() * 0.3f, button.height() * 0.3f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(edge);
        canvas.drawRoundRect(button, button.height() * 0.3f, button.height() * 0.3f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Palette.TEXT);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(button.height() * 0.36f);
        paint.setFakeBoldText(true);
        canvas.drawText(label, button.centerX(), button.centerY() + button.height() * 0.13f, paint);
        paint.setFakeBoldText(false);
    }

    private void drawStars(Canvas canvas, RectF banner, int earned) {
        float size = banner.height() * 0.1f;
        float gap = size * 0.9f;
        float y = banner.top + banner.height() * 0.42f;
        for (int i = 0; i < 3; i++) {
            paint.setColor(i < earned ? Palette.KEY : Palette.LOCKED);
            drawStar(canvas, banner.centerX() + (i - 1) * (size * 2 + gap), y, size);
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

    // ---------------------------------------------------------------- touch

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN -> {
                pressX = event.getX();
                pressY = event.getY();
            }
            case MotionEvent.ACTION_UP -> {
                performClick();
                handleRelease(event.getX(), event.getY());
                invalidate();
            }
            default -> { }
        }
        return true;
    }

    private void handleRelease(float x, float y) {
        if (verdictShowing()) {
            if (verdictButton(true).contains(x, y)) {
                onLeave.run();
            } else if (verdictButton(false).contains(x, y)) {
                if (state.hasEscaped()) {
                    onNext.run();
                } else {
                    load(levelName);
                }
            }
            return;
        }
        if (pressY >= footerTop()) {
            for (int i = 0; i < 4; i++) {
                if (buttonAt(i).contains(x, y)) {
                    press(i);
                    return;
                }
            }
            return;
        }
        if (pressY < headerBottom()) {
            return;
        }

        float draggedX = x - pressX;
        float draggedY = y - pressY;
        float swipe = tileSize() * 0.6f;
        if (Math.abs(draggedX) > swipe || Math.abs(draggedY) > swipe) {
            step(Math.abs(draggedX) > Math.abs(draggedY)
                    ? (draggedX < 0 ? Direction.LEFT : Direction.RIGHT)
                    : (draggedY < 0 ? Direction.UP : Direction.DOWN));
        } else {
            stepTowards(x, y);
        }
    }

    private void press(int button) {
        switch (button) {
            case UNDO -> state.undoLastMove();
            case HINT -> {
                if (hintTurn != state.turnsTaken() && state.spendHint()) {
                    hintTurn = state.turnsTaken();
                }
            }
            case RESTART -> load(levelName);
            default -> onLeave.run();
        }
    }

    private void step(Direction move) {
        if (state.isAttemptOver()) {
            return;
        }
        state.movePlayer(move);
        reportIfFinished();
    }

    private void stepTowards(float x, float y) {
        if (state.isAttemptOver()) {
            if (state.isCaught() && !state.isRunOver()) {
                state.beginNextLife();
            }
            return;
        }
        int tile = tileSize();
        int tappedRow = (int) Math.floor((y - boardTop()) / tile);
        int tappedCol = (int) Math.floor((x - boardLeft()) / tile);
        int downwards = tappedRow - state.player().row();
        int rightwards = tappedCol - state.player().col();

        if (downwards == 0 && rightwards == 0) {
            state.movePlayer(Direction.WAIT);
        } else {
            state.movePlayer(Math.abs(downwards) >= Math.abs(rightwards)
                    ? (downwards < 0 ? Direction.UP : Direction.DOWN)
                    : (rightwards < 0 ? Direction.LEFT : Direction.RIGHT));
        }
        reportIfFinished();
    }

    private void reportIfFinished() {
        if (!recorded && state.hasEscaped()) {
            recorded = true;
            onFinished.accept(levelName, state.score());
        }
    }
}
