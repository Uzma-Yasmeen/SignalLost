package com.signallost.android;

import com.signallost.guard.Guard;
import com.signallost.guard.GuardMode;
import com.signallost.level.Level;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameMap;
import com.signallost.model.GameState;
import com.signallost.model.Position;
import com.signallost.model.TileType;
import com.signallost.vision.TileVisibility;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/**
 * The whole game, drawn onto a canvas.
 *
 * <p>This class and {@link GameActivity} are the entire Android front end. The
 * fog, the guards, the pathfinding, the undo stack and the scoring are the same
 * compiled classes the desktop game runs — not a port of them, not a copy. That
 * is the only reason a phone version is a few hundred lines rather than a
 * second project.
 */
public class BoardView extends View {

    // The desktop palette, by eye. Colours are the one thing worth duplicating,
    // since the original lives in the Swing package this module excludes.
    private static final int BACKDROP = Color.rgb(10, 12, 18);
    private static final int WALL_SEEN = Color.rgb(128, 148, 196);
    private static final int WALL_REMEMBERED = Color.rgb(44, 52, 76);
    private static final int FLOOR_SEEN = Color.rgb(26, 32, 48);
    private static final int FLOOR_REMEMBERED = Color.rgb(16, 20, 30);
    private static final int WATCHED = Color.argb(110, 220, 60, 70);
    private static final int KEY = Color.rgb(246, 196, 70);
    private static final int EXIT_OPEN = Color.rgb(92, 214, 140);
    private static final int EXIT_SHUT = Color.rgb(90, 100, 128);
    private static final int PLAYER = Color.rgb(244, 164, 96);
    private static final int HINT = Color.rgb(96, 170, 255);
    private static final int TEXT = Color.rgb(232, 236, 246);
    private static final int TEXT_DIM = Color.rgb(140, 150, 175);
    private static final int BUTTON = Color.rgb(30, 36, 54);

    private static final int HUD_HEIGHT = 150;
    private static final int BUTTON_HEIGHT = 150;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    private String levelName;
    private GameState state;
    private int hintTurn = -1;

    public BoardView(Context context, String levelName) {
        super(context);
        this.levelName = levelName;
        setBackgroundColor(BACKDROP);
        load(levelName);
    }

    private void load(String name) {
        Level level = LevelLoader.loadFromResource(name);
        this.levelName = name;
        this.state = new GameState(level.map(), level.guards(),
                GameState.DEFAULT_SIGHT_RADIUS, level.turnLimit());
        this.hintTurn = -1;
    }

    public GameState state() {
        return state;
    }

    // ------------------------------------------------------------------ draw

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(BACKDROP);
        GameMap map = state.map();

        int boardTop = HUD_HEIGHT;
        int boardHeight = getHeight() - HUD_HEIGHT - BUTTON_HEIGHT;
        int tile = Math.min(getWidth() / map.cols(), boardHeight / map.rows());
        int offsetX = (getWidth() - tile * map.cols()) / 2;
        int offsetY = boardTop + (boardHeight - tile * map.rows()) / 2;

        for (int row = 0; row < map.rows(); row++) {
            for (int col = 0; col < map.cols(); col++) {
                drawTile(canvas, map, new Position(row, col), tile, offsetX, offsetY);
            }
        }

        drawGuardSight(canvas, tile, offsetX, offsetY);
        drawGuards(canvas, tile, offsetX, offsetY);
        if (hintTurn == state.turnsTaken()) {
            drawHint(canvas, tile, offsetX, offsetY);
        }
        drawPlayer(canvas, tile, offsetX, offsetY);
        drawHud(canvas);
        drawButtons(canvas);
        drawVerdict(canvas);
    }

    private RectF boundsOf(Position at, int tile, int offsetX, int offsetY) {
        float left = offsetX + at.col() * tile;
        float top = offsetY + at.row() * tile;
        return new RectF(left + 1, top + 1, left + tile - 1, top + tile - 1);
    }

    private void drawTile(Canvas canvas, GameMap map, Position at,
                          int tile, int offsetX, int offsetY) {
        TileVisibility seen = state.fog().visibilityOf(at);
        if (seen == TileVisibility.UNEXPLORED) {
            return;
        }
        boolean lit = seen == TileVisibility.VISIBLE;
        TileType type = map.tileAt(at);
        RectF bounds = boundsOf(at, tile, offsetX, offsetY);

        fill.setColor(type == TileType.WALL
                ? (lit ? WALL_SEEN : WALL_REMEMBERED)
                : (lit ? FLOOR_SEEN : FLOOR_REMEMBERED));
        canvas.drawRoundRect(bounds, tile * 0.12f, tile * 0.12f, fill);

        // Keys and the exit are only drawn where the player can see them now,
        // so a remembered room does not quietly keep showing a collected key.
        if (!lit) {
            return;
        }
        if (type == TileType.KEY) {
            fill.setColor(KEY);
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), tile * 0.22f, fill);
        } else if (type == TileType.EXIT) {
            fill.setColor(state.hasAllKeys() ? EXIT_OPEN : EXIT_SHUT);
            canvas.drawRoundRect(new RectF(
                    bounds.left + tile * 0.18f, bounds.top + tile * 0.18f,
                    bounds.right - tile * 0.18f, bounds.bottom - tile * 0.18f),
                    tile * 0.1f, tile * 0.1f, fill);
        }
    }

    private void drawGuardSight(Canvas canvas, int tile, int offsetX, int offsetY) {
        fill.setColor(WATCHED);
        for (Guard guard : state.guards()) {
            if (!state.fog().isVisible(guard.position())) {
                continue;
            }
            for (Position watched : guard.watchedTiles(state.map())) {
                if (state.fog().visibilityOf(watched) == TileVisibility.VISIBLE) {
                    canvas.drawRoundRect(boundsOf(watched, tile, offsetX, offsetY),
                            tile * 0.12f, tile * 0.12f, fill);
                }
            }
        }
    }

    private void drawGuards(Canvas canvas, int tile, int offsetX, int offsetY) {
        for (Guard guard : state.guards()) {
            if (!state.fog().isVisible(guard.position())) {
                continue;
            }
            RectF bounds = boundsOf(guard.position(), tile, offsetX, offsetY);
            fill.setColor(colourOf(guard.mode()));
            canvas.drawRoundRect(new RectF(
                    bounds.left + tile * 0.12f, bounds.top + tile * 0.12f,
                    bounds.right - tile * 0.12f, bounds.bottom - tile * 0.12f),
                    tile * 0.2f, tile * 0.2f, fill);
            fill.setColor(BACKDROP);
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), tile * 0.1f, fill);
        }
    }

    private static int colourOf(GuardMode mode) {
        return switch (mode) {
            case HUNT -> Color.rgb(232, 70, 84);
            case ALERT -> Color.rgb(240, 176, 64);
            case PATROL -> Color.rgb(96, 136, 212);
        };
    }

    private void drawHint(Canvas canvas, int tile, int offsetX, int offsetY) {
        var route = state.hintRoute(1);
        if (route.isEmpty()) {
            return;
        }
        RectF bounds = boundsOf(route.get(0), tile, offsetX, offsetY);
        fill.setColor(state.hintSeeksCover() ? KEY : HINT);
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), tile * 0.16f, fill);
    }

    private void drawPlayer(Canvas canvas, int tile, int offsetX, int offsetY) {
        RectF bounds = boundsOf(state.player(), tile, offsetX, offsetY);
        fill.setColor(PLAYER);
        canvas.drawRoundRect(new RectF(
                bounds.left + tile * 0.14f, bounds.top + tile * 0.14f,
                bounds.right - tile * 0.14f, bounds.bottom - tile * 0.14f),
                tile * 0.25f, tile * 0.25f, fill);
    }

    private void drawHud(Canvas canvas) {
        text.setColor(TEXT);
        text.setTextSize(46);
        text.setFakeBoldText(true);
        canvas.drawText(prettyLevel(), 28, 58, text);

        text.setFakeBoldText(false);
        text.setTextSize(34);
        text.setColor(state.wasSpotted() ? Color.rgb(232, 70, 84) : TEXT_DIM);
        String line = "keys " + state.keysHeld() + "/" + state.keysRequired()
                + "   lives " + state.livesLeft()
                + "   hints " + state.hintsLeft()
                + "   seen " + state.sightings()
                + (state.hasTurnLimit() ? "   turns " + state.turnsRemaining() : "");
        canvas.drawText(line, 28, 110, text);
    }

    private String prettyLevel() {
        return levelName.replaceAll("(?<=\\D)(?=\\d)", " ").toUpperCase();
    }

    /** Undo, hint and restart, as three strips along the bottom. */
    private void drawButtons(Canvas canvas) {
        String[] labels = {"UNDO", "HINT " + state.hintsLeft(), "RESTART"};
        int width = getWidth() / labels.length;
        int top = getHeight() - BUTTON_HEIGHT;

        for (int i = 0; i < labels.length; i++) {
            RectF bounds = new RectF(i * width + 10, top + 16,
                    (i + 1) * width - 10, getHeight() - 24);
            fill.setColor(BUTTON);
            canvas.drawRoundRect(bounds, 22, 22, fill);
            text.setColor(TEXT);
            text.setTextSize(38);
            text.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(labels[i], bounds.centerX(), bounds.centerY() + 14, text);
            text.setTextAlign(Paint.Align.LEFT);
        }
    }

    private void drawVerdict(Canvas canvas) {
        String message = null;
        if (state.hasEscaped()) {
            message = "ESCAPED  ·  " + state.score().stars() + " / 3 stars";
        } else if (state.isCaught()) {
            message = state.isRunOver() ? "CAUGHT  ·  run over" : "CAUGHT  ·  tap restart";
        } else if (state.hasFailed()) {
            message = "OUT OF TURNS";
        }
        if (message == null) {
            return;
        }
        fill.setColor(Color.argb(220, 10, 12, 18));
        canvas.drawRoundRect(new RectF(30, getHeight() / 2f - 70,
                getWidth() - 30, getHeight() / 2f + 40), 24, 24, fill);
        text.setColor(TEXT);
        text.setTextSize(52);
        text.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(message, getWidth() / 2f, getHeight() / 2f, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    // ----------------------------------------------------------------- touch

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return true;
        }
        performClick();

        float x = event.getX();
        float y = event.getY();

        if (y > getHeight() - BUTTON_HEIGHT) {
            pressButton((int) (x / (getWidth() / 3f)));
        } else if (y > HUD_HEIGHT) {
            moveTowards(x, y);
        }
        invalidate();
        return true;
    }

    private void pressButton(int index) {
        switch (index) {
            case 0 -> state.undoLastMove();
            case 1 -> {
                if (hintTurn != state.turnsTaken() && state.spendHint()) {
                    hintTurn = state.turnsTaken();
                }
            }
            default -> load(levelName);
        }
    }

    /**
     * Steps one tile towards wherever was tapped.
     *
     * <p>A phone has no arrow keys, and asking somebody to hit a 40-pixel tile
     * with a thumb is a different game. Tapping anywhere on the board moves one
     * step in that general direction, which keeps the turn-based feel while
     * being forgiving about precision.
     */
    private void moveTowards(float x, float y) {
        if (state.isAttemptOver()) {
            if (state.isCaught()) {
                state.beginNextLife();
            }
            return;
        }
        GameMap map = state.map();
        int boardHeight = getHeight() - HUD_HEIGHT - BUTTON_HEIGHT;
        int tile = Math.min(getWidth() / map.cols(), boardHeight / map.rows());
        int offsetX = (getWidth() - tile * map.cols()) / 2;
        int offsetY = HUD_HEIGHT + (boardHeight - tile * map.rows()) / 2;

        int tappedRow = (int) ((y - offsetY) / tile);
        int tappedCol = (int) ((x - offsetX) / tile);
        int downwards = tappedRow - state.player().row();
        int rightwards = tappedCol - state.player().col();

        if (downwards == 0 && rightwards == 0) {
            state.movePlayer(Direction.WAIT);
            return;
        }
        Direction move = Math.abs(downwards) >= Math.abs(rightwards)
                ? (downwards < 0 ? Direction.UP : Direction.DOWN)
                : (rightwards < 0 ? Direction.LEFT : Direction.RIGHT);
        state.movePlayer(move);
    }
}
