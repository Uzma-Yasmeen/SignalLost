package com.signallost.android;

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
 * The game itself, drawn onto a canvas.
 *
 * <p>Laid out sideways: the board takes the width it needs on the left, and a
 * panel down the right holds the status and the controls. In portrait the
 * board was limited by the screen's width and left a third of the phone empty
 * above and below it; turned round, a level fifteen tiles wide and thirteen
 * tall nearly fills the glass.
 */
@SuppressLint("ViewConstructor")
class BoardView extends View {

    /** Width of the side panel, as a fraction of the screen. */
    private static final float PANEL_FRACTION = 0.22f;

    private static final int UNDO = 0;
    private static final int HINT = 1;
    private static final int RESTART = 2;
    private static final int LEVELS = 3;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final BiConsumer<String, Score> onFinished;
    private final Runnable onLeave;

    private String levelName;
    private GameState state;
    private int hintTurn = -1;
    private boolean recorded;

    // Where a drag started, so a swipe can move as well as a tap.
    private float pressX;
    private float pressY;

    BoardView(Context context, String levelName,
              BiConsumer<String, Score> onFinished, Runnable onLeave) {
        super(context);
        this.onFinished = onFinished;
        this.onLeave = onLeave;
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

    private float panelLeft() {
        return getWidth() * (1 - PANEL_FRACTION);
    }

    private int tileSize() {
        GameMap map = state.map();
        return (int) Math.min(panelLeft() / map.cols(), getHeight() / (float) map.rows());
    }

    private float boardLeft() {
        return (panelLeft() - tileSize() * state.map().cols()) / 2;
    }

    private float boardTop() {
        return (getHeight() - tileSize() * state.map().rows()) / 2f;
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
        drawPanel(canvas);
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
        canvas.drawRoundRect(bounds, tile * 0.14f, tile * 0.14f, paint);

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

    /** A round bow and a stem, so a key looks like a key rather than a dot. */
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
                    canvas.drawRoundRect(tileAt(watched), tile * 0.14f, tile * 0.14f, paint);
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
            paint.setColor(colourOf(guard.mode()));
            float inset = tile * 0.13f;
            canvas.drawRoundRect(new RectF(bounds.left + inset, bounds.top + inset,
                    bounds.right - inset, bounds.bottom - inset), tile * 0.22f, tile * 0.22f, paint);
            paint.setColor(Palette.BACKDROP);
            canvas.drawCircle(bounds.centerX(), bounds.centerY() - tile * 0.04f, tile * 0.09f, paint);
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
        paint.setStrokeWidth(tileSize() * 0.1f);
        canvas.drawRoundRect(bounds, tileSize() * 0.14f, tileSize() * 0.14f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawPlayer(Canvas canvas) {
        int tile = tileSize();
        RectF bounds = tileAt(state.player());
        float cx = bounds.centerX();
        float cy = bounds.centerY();

        paint.setColor(Palette.withAlpha(Palette.PLAYER, 40));
        canvas.drawCircle(cx, cy, tile * 0.6f, paint);

        // A blunt little animal: a round head and two ears, which reads better
        // at this size than anything more detailed would.
        paint.setColor(Palette.PLAYER);
        canvas.drawCircle(cx, cy + tile * 0.04f, tile * 0.28f, paint);
        Path ears = new Path();
        ears.moveTo(cx - tile * 0.26f, cy - tile * 0.12f);
        ears.lineTo(cx - tile * 0.3f, cy - tile * 0.34f);
        ears.lineTo(cx - tile * 0.06f, cy - tile * 0.22f);
        ears.close();
        ears.moveTo(cx + tile * 0.26f, cy - tile * 0.12f);
        ears.lineTo(cx + tile * 0.3f, cy - tile * 0.34f);
        ears.lineTo(cx + tile * 0.06f, cy - tile * 0.22f);
        ears.close();
        canvas.drawPath(ears, paint);

        paint.setColor(Palette.BACKDROP);
        canvas.drawCircle(cx - tile * 0.1f, cy, tile * 0.045f, paint);
        canvas.drawCircle(cx + tile * 0.1f, cy, tile * 0.045f, paint);
    }

    // ---------------------------------------------------------------- panel

    private void drawPanel(Canvas canvas) {
        float left = panelLeft();
        paint.setColor(Palette.PANEL);
        canvas.drawRect(left, 0, getWidth(), getHeight(), paint);

        float pad = getWidth() * 0.015f;
        paint.setColor(Palette.TEXT);
        paint.setFakeBoldText(true);
        paint.setTextSize(getHeight() * 0.075f);
        canvas.drawText(prettyLevel(), left + pad, getHeight() * 0.1f, paint);
        paint.setFakeBoldText(false);

        String[] rows = {
            "keys    " + state.keysHeld() + " / " + state.keysRequired(),
            "lives   " + state.livesLeft(),
            "hints   " + state.hintsLeft(),
            "seen    " + state.sightings(),
            state.hasTurnLimit() ? "turns   " + state.turnsRemaining() : "turns   " + state.turnsTaken(),
        };
        paint.setTextSize(getHeight() * 0.05f);
        for (int i = 0; i < rows.length; i++) {
            paint.setColor(i == 3 && state.sightings() > 0 ? Palette.DANGER : Palette.TEXT_DIM);
            canvas.drawText(rows[i], left + pad, getHeight() * (0.2f + i * 0.072f), paint);
        }

        String[] labels = {"UNDO", "HINT  " + state.hintsLeft(), "RESTART", "LEVELS"};
        for (int i = 0; i < labels.length; i++) {
            RectF button = buttonAt(i);
            boolean usable = switch (i) {
                case UNDO -> state.canUndo();
                case HINT -> state.canSpendHint();
                default -> true;
            };
            paint.setColor(Palette.BUTTON);
            canvas.drawRoundRect(button, button.height() * 0.3f, button.height() * 0.3f, paint);
            paint.setColor(usable ? Palette.TEXT : Palette.LOCKED);
            paint.setTextSize(button.height() * 0.4f);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(labels[i], button.centerX(), button.centerY() + button.height() * 0.14f,
                    paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }

    /**
     * The four buttons, stacked and guaranteed to fit.
     *
     * <p>Sized from the space actually left rather than from fractions chosen
     * by eye: the first version put the last button eleven pixels past the
     * bottom of the screen, so tapping LEVELS restarted the level instead.
     */
    private RectF buttonAt(int index) {
        float pad = getWidth() * 0.015f;
        float left = panelLeft() + pad;
        float right = getWidth() - pad;

        float top = getHeight() * 0.55f;
        float bottom = getHeight() - pad;
        float gap = getHeight() * 0.018f;
        float height = (bottom - top - gap * 3) / 4;

        float buttonTop = top + index * (height + gap);
        return new RectF(left, buttonTop, right, buttonTop + height);
    }

    private String prettyLevel() {
        return levelName.replaceAll("(?<=\\D)(?=\\d)", " ").toUpperCase();
    }

    private void drawVerdict(Canvas canvas) {
        String headline = null;
        String detail = null;
        if (state.hasEscaped()) {
            Score score = state.score();
            headline = "ESCAPED";
            detail = score.stars() + " of 3 stars   ·   " + score.turns()
                    + " turns, par " + score.par() + "   ·   seen " + score.sightings();
        } else if (state.isRunOver()) {
            headline = "RUN OVER";
            detail = "no lives left   ·   tap RESTART";
        } else if (state.isCaught()) {
            headline = "CAUGHT";
            detail = state.livesLeft() + " lives left   ·   tap the board to carry on";
        } else if (state.hasFailed()) {
            headline = "OUT OF TURNS";
            detail = "tap RESTART";
        }
        if (headline == null) {
            return;
        }

        float midX = panelLeft() / 2;
        float midY = getHeight() / 2f;
        RectF banner = new RectF(midX - panelLeft() * 0.42f, midY - getHeight() * 0.14f,
                midX + panelLeft() * 0.42f, midY + getHeight() * 0.13f);
        paint.setColor(Palette.withAlpha(Palette.BACKDROP, 238));
        canvas.drawRoundRect(banner, 28, 28, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(state.hasEscaped() ? Palette.EXIT_OPEN : Palette.DANGER);
        canvas.drawRoundRect(banner, 28, 28, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(state.hasEscaped() ? Palette.EXIT_OPEN : Palette.DANGER);
        paint.setFakeBoldText(true);
        paint.setTextSize(getHeight() * 0.1f);
        canvas.drawText(headline, midX, midY, paint);
        paint.setFakeBoldText(false);
        paint.setColor(Palette.TEXT_DIM);
        paint.setTextSize(getHeight() * 0.045f);
        canvas.drawText(detail, midX, midY + getHeight() * 0.08f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
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
        if (pressX >= panelLeft()) {
            for (int i = 0; i < 4; i++) {
                if (buttonAt(i).contains(x, y)) {
                    press(i);
                    return;
                }
            }
            return;
        }
        reportIfFinished();

        float draggedX = x - pressX;
        float draggedY = y - pressY;
        float swipe = tileSize() * 0.6f;

        // A swipe says the direction outright. A tap means "go towards there",
        // which keeps working anywhere on the board rather than only on the
        // tile next to you.
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

    /** Hands a finished level to whoever is keeping score, exactly once. */
    private void reportIfFinished() {
        if (!recorded && state.hasEscaped()) {
            recorded = true;
            onFinished.accept(levelName, state.score());
        }
    }
}
