package com.signallost.android;

import com.signallost.art.Critter;
import com.signallost.guard.Guard;
import com.signallost.guard.GuardMode;
import com.signallost.level.BoardScale;
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

import java.util.ArrayList;
import java.util.Comparator;
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

    /**
     * Big enough to read, even when that means the board does not all fit.
     *
     * <p>Fitting the whole level on the glass sounds right and is not: level 1
     * is eleven rows and came out at 78 pixels a tile, level 10 is fifteen and
     * came out at 57. The later levels are where a player most needs to see
     * what they are doing, and they were the hardest to look at.
     *
     * <p>So a tile never goes below the size eleven rows would give it, and
     * anything taller than that scrolls. The fog makes this cost less than it
     * sounds: most of a level is unexplored or remembered, and what matters is
     * nearly always within a few tiles of where you are standing.
     */
    /**
     * How big one tile is drawn — the same on every level.
     *
     * <p>Calibrated to a reference board, not to the level being played and not
     * to the largest level that ships. Per level meant the character shrank on
     * reaching level 5, the first board taller than the four before it; to the
     * largest meant every level drawn at the size the biggest one needs, which
     * shrinks levels 1 to 4 to fix level 5. Boards larger than the reference
     * scroll, and the camera follows.
     */
    private int tileSize() {
        float usableHeight = footerTop() - headerBottom();
        float fitted = Math.min(getWidth() / (float) BoardScale.COLS_THAT_FIT,
                usableHeight / BoardScale.ROWS_THAT_FIT);
        return (int) Math.max(fitted, usableHeight / 11f);
    }

    /**
     * Keeps the player in view, and the board's edges on screen where possible.
     *
     * <p>Centring on the player always would leave half the screen empty at the
     * edges of a level, so the view is clamped: it follows while there is board
     * to show and stops when the edge is reached.
     */
    private static float follow(float viewSize, float boardSize, float playerCentre, float origin) {
        if (boardSize <= viewSize) {
            return origin + (viewSize - boardSize) / 2;
        }
        float wanted = origin + viewSize / 2 - playerCentre;
        float furthest = origin;
        float nearest = origin + viewSize - boardSize;
        return Math.max(nearest, Math.min(furthest, wanted));
    }

    private float boardLeft() {
        int tile = tileSize();
        return follow(getWidth(), tile * state.map().cols(),
                (state.player().col() + 0.5f) * tile, 0);
    }

    private float boardTop() {
        int tile = tileSize();
        return follow(footerTop() - headerBottom(), tile * state.map().rows(),
                (state.player().row() + 0.5f) * tile, headerBottom());
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

        // The board can now be taller than the space between the bars, so
        // anything outside that band is clipped rather than drawn over them.
        canvas.save();
        canvas.clipRect(0, headerBottom(), getWidth(), footerTop());

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
        canvas.restore();

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

        int base = type == TileType.WALL
                ? (lit ? Palette.WALL_SEEN : Palette.WALL_REMEMBERED)
                : (lit ? Palette.FLOOR_SEEN : Palette.FLOOR_REMEMBERED);
        paint.setColor(lit ? Palette.shade(base, falloff(at)) : base);
        canvas.drawRoundRect(bounds, tile * 0.16f, tile * 0.16f, paint);

        if (!lit) {
            return;
        }
        if (type == TileType.KEY) {
            PixelArt.key(canvas, bounds.centerX(), bounds.centerY(), tile * 0.78f);
        } else if (type == TileType.EXIT) {
            PixelArt.exit(canvas, state.hasAllKeys(),
                    bounds.centerX(), bounds.centerY(), tile * 0.86f);
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

    /**
     * How brightly a tile is lit, by how far the light had to travel to it.
     *
     * <p>The same curve the desktop uses, over the same step counts that the
     * flood fill already produced. Without it the lit area is one flat sheet
     * and the torch stops feeling like a torch.
     */
    private double falloff(Position at) {
        if (state.fog().isRevealAll()) {
            return 1.0;
        }
        int distance = state.fog().sightDistance(at);
        if (distance < 0 || state.fog().sightRadius() == 0) {
            return 1.0;
        }
        return 1.0 - 0.5 * ((double) distance / state.fog().sightRadius());
    }

    private void drawGuardSight(Canvas canvas) {
        int tile = tileSize();
        for (Guard guard : state.guards()) {
            if (!state.fog().isVisible(guard.position())) {
                continue;
            }
            // Red means one thing: do not end your turn here. These are the
            // desktop's exact strengths. Guessing at them gave a wash twice as
            // heavy, which turned most of a lit room red and left the player
            // nothing to read.
            paint.setColor(Palette.withAlpha(Palette.DANGER, dangerStrength(guard.mode())));
            for (Position watched : guard.watchedTiles(state.map())) {
                if (state.fog().visibilityOf(watched) == TileVisibility.VISIBLE) {
                    canvas.drawRoundRect(tileAt(watched), tile * 0.16f, tile * 0.16f, paint);
                }
            }
        }
    }

    /**
     * Guards, coloured by what they are doing and shaped by what they are.
     *
     * <p>Colour alone was telling the player only half the story. A hunter that
     * had not noticed them yet drew in the same calm blue as a patroller, so
     * the one guard worth avoiding was invisible among the ones that merely
     * block a corridor — and by the time it turned red it was already chasing.
     *
     * <p>So colour is state and shape is identity: a patroller is a plain
     * rounded block, a hunter has two spikes, and a sentinel is a lens with the
     * quarter it is watching marked. Whichever mode it is in, you can see what
     * it is.
     */
    private void drawGuards(Canvas canvas) {
        int tile = tileSize();
        for (Guard guard : state.guards()) {
            if (!state.fog().isVisible(guard.position())) {
                continue;
            }
            RectF bounds = tileAt(guard.position());
            int colour = colourOf(guard.mode());
            float cx = bounds.centerX();
            float cy = bounds.centerY();

            paint.setColor(Palette.withAlpha(colour, 50));
            canvas.drawCircle(cx, cy, tile * 0.6f, paint);
            paint.setColor(colour);

            // The robot, and only the robot. Markers for hunters and sentinels
            // were tried and dropped: a clean sprite beats a warning sign, and
            // the lens colour carries the state as it does on the desktop.
            PixelArt.guard(canvas, guard.mode().name(), cx, cy, tile * 0.95f);
        }
    }

    /** Colour says what a guard is doing: calm, searching, or hunting you. */
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
        // A soft pool of torchlight rather than one disc, which is what makes
        // the dark around it read as dark.
        for (int ring = 5; ring >= 1; ring--) {
            paint.setColor(Palette.withAlpha(PixelArt.colourOf(critter), 9));
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), tile * 0.5f * ring, paint);
        }
        PixelArt.critter(canvas, critter, bounds.centerX(), bounds.centerY(), tile * 0.95f);
    }

    // --------------------------------------------------------- header, footer

    private static int dangerStrength(GuardMode mode) {
        return switch (mode) {
            case PATROL -> 46;
            case ALERT -> 66;
            case HUNT -> 92;
        };
    }

    private void drawHeader(Canvas canvas) {
        float bottom = headerBottom();
        paint.setColor(Palette.PANEL);
        canvas.drawRect(0, 0, getWidth(), bottom, paint);
        paint.setColor(Palette.withAlpha(Palette.BUTTON_EDGE, 90));
        canvas.drawRect(0, bottom - 2, getWidth(), bottom, paint);

        drawIdentity(canvas);
        drawReadings(canvas);
    }

    /** Who you are playing as, and where, at the left-hand end. */
    private void drawIdentity(Canvas canvas) {
        float bottom = headerBottom();
        PixelArt.critter(canvas, critter, getWidth() * 0.02f + bottom * 0.3f,
                bottom * 0.5f, bottom * 0.62f);

        paint.setColor(Palette.TEXT);
        paint.setFakeBoldText(true);
        paint.setTextSize(bottom * 0.36f);
        canvas.drawText(critter.displayName(), identityLeft(), bottom * 0.48f, paint);

        paint.setFakeBoldText(false);
        paint.setColor(Palette.TEXT_DIM);
        paint.setTextSize(bottom * 0.25f);
        canvas.drawText(prettyLevel() + "  \u00b7  SIGHT " + state.fog().sightRadius(),
                identityLeft(), bottom * 0.85f, paint);
    }

    private float identityLeft() {
        return getWidth() * 0.02f + headerBottom() * 0.62f;
    }

    /** Where the identity block stops, so nothing is drawn across it. */
    private float identityRightEdge() {
        float bottom = headerBottom();
        paint.setFakeBoldText(true);
        paint.setTextSize(bottom * 0.36f);
        float name = paint.measureText(critter.displayName());
        paint.setFakeBoldText(false);
        paint.setTextSize(bottom * 0.25f);
        float beneath = paint.measureText(
                prettyLevel() + "  \u00b7  SIGHT " + state.fog().sightRadius());
        return identityLeft() + Math.max(name, beneath);
    }

    /**
     * A chip waiting to be drawn, and how readily it gives up its place.
     *
     * <p>{@code rank} is the order they survive in when the bar is too narrow:
     * lowest goes first. The developer lamp is the first thing nobody needs; the
     * key count is the last. The desktop drops them the same way, and on a phone
     * in landscape it happens most of the time.
     */
    private record Chip(String label, int colour, int rank) {
    }

    private void drawReadings(Canvas canvas) {
        List<Chip> chips = new ArrayList<>();
        if (state.fog().isRevealAll()) {
            chips.add(new Chip("DEV REVEAL", Palette.ACCENT, 1));
        }
        if (state.wasSpotted()) {
            chips.add(new Chip("SPOTTED", Palette.DANGER, 5));
        }
        GuardMode alert = state.highestAlertLevel();
        if (alert != null && alert != GuardMode.PATROL) {
            chips.add(new Chip(alert.label().toUpperCase(),
                    alert == GuardMode.HUNT ? Palette.GUARD_HUNT : Palette.GUARD_ALERT, 4));
        }
        if (state.sightings() > 0) {
            chips.add(new Chip("SEEN " + state.sightings(), Palette.GUARD_ALERT, 3));
        }
        if (state.canUndo()) {
            chips.add(new Chip("UNDO " + state.undoDepth(), Palette.ACCENT, 2));
        }
        chips.add(new Chip(state.livesLeft() == 1 ? "LAST LIFE" : "LIVES " + state.livesLeft(),
                livesColour(), 7));
        chips.add(new Chip(state.hintsLeft() == 0 ? "NO HINTS" : "HINTS " + state.hintsLeft(),
                hintsColour(), 6));
        chips.add(new Chip("KEYS " + state.keysHeld() + "/" + state.keysRequired(),
                state.hasAllKeys() ? Palette.EXIT_OPEN : Palette.KEY, 8));

        float room = getWidth() * 0.98f - identityRightEdge()
                - headerBottom() * 0.3f - counterWidth();
        while (chips.size() > 1 && rowWidth(chips) > room) {
            chips.remove(chips.stream().min(Comparator.comparingInt(Chip::rank)).orElseThrow());
        }

        float x = getWidth() * 0.98f;
        for (Chip chip : chips) {
            x = drawChip(canvas, x, chip.label(), chip.colour());
        }
        drawTurnCounter(canvas, x);
    }

    private float rowWidth(List<Chip> chips) {
        float total = 0;
        for (Chip chip : chips) {
            total += chipWidth(chip.label()) + headerBottom() * 0.1f;
        }
        return total;
    }

    private float chipWidth(String label) {
        paint.setFakeBoldText(true);
        paint.setTextSize(headerBottom() * 0.27f);
        float width = paint.measureText(label) + headerBottom() * 0.62f;
        paint.setFakeBoldText(false);
        return width;
    }

    private int hintsColour() {
        int hints = state.hintsLeft();
        if (hints == 0) {
            return Palette.DANGER;
        }
        return hints == 1 ? Palette.GUARD_ALERT : Palette.TEXT_DIM;
    }

    private int livesColour() {
        int lives = state.livesLeft();
        if (lives <= 1) {
            return Palette.DANGER;
        }
        return lives == 2 ? Palette.GUARD_ALERT : Palette.TEXT_DIM;
    }

    /** A pill right-aligned at {@code rightEdge}, returning the next right edge. */
    private float drawChip(Canvas canvas, float rightEdge, String label, int colour) {
        float bottom = headerBottom();
        float w = chipWidth(label);
        float h = bottom * 0.48f;
        float x = rightEdge - w;
        float y = (bottom - h) / 2;
        RectF pill = new RectF(x, y, x + w, y + h);

        paint.setColor(Palette.withAlpha(colour, 28));
        canvas.drawRoundRect(pill, h / 2, h / 2, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(Palette.withAlpha(colour, 90));
        canvas.drawRoundRect(pill, h / 2, h / 2, paint);
        paint.setStyle(Paint.Style.FILL);

        // The dot is what makes a pill readable at a glance: the colour lands
        // before the word does.
        paint.setColor(colour);
        canvas.drawCircle(x + h * 0.44f, y + h / 2, h * 0.15f, paint);

        paint.setFakeBoldText(true);
        paint.setTextSize(bottom * 0.27f);
        canvas.drawText(label, x + h * 0.72f, y + h * 0.68f, paint);
        paint.setFakeBoldText(false);
        return x - bottom * 0.1f;
    }

    private String turnNumber() {
        return state.hasTurnLimit()
                ? state.turnsTaken() + "/" + state.turnLimit()
                : String.valueOf(state.turnsTaken());
    }

    private String turnLabel() {
        return state.hasTurnLimit() ? "TURNS LEFT " + state.turnsRemaining() : "TURNS";
    }

    private float counterWidth() {
        paint.setFakeBoldText(true);
        paint.setTextSize(headerBottom() * 0.46f);
        float number = paint.measureText(turnNumber());
        paint.setFakeBoldText(false);
        paint.setTextSize(headerBottom() * 0.23f);
        return Math.max(number, paint.measureText(turnLabel()));
    }

    /** Turns taken over the limit, in whatever gap the chips left. */
    private void drawTurnCounter(Canvas canvas, float rightEdge) {
        float bottom = headerBottom();
        float block = counterWidth();
        float centre = Math.max(rightEdge - block,
                identityRightEdge() + bottom * 0.3f) + block / 2;

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(clockColour());
        paint.setFakeBoldText(true);
        paint.setTextSize(bottom * 0.46f);
        canvas.drawText(turnNumber(), centre, bottom * 0.52f, paint);

        paint.setFakeBoldText(false);
        paint.setColor(Palette.TEXT_DIM);
        paint.setTextSize(bottom * 0.23f);
        canvas.drawText(turnLabel(), centre, bottom * 0.85f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    /** Pale while there is room, amber when the clock tightens, red at the end. */
    private int clockColour() {
        if (!state.hasTurnLimit()) {
            return Palette.TEXT;
        }
        double left = (double) state.turnsRemaining() / state.turnLimit();
        if (left <= 0.10) {
            return Palette.DANGER;
        }
        return left <= 0.25 ? Palette.GUARD_ALERT : Palette.TEXT;
    }

    private void drawFooter(Canvas canvas) {
        float top = footerTop();
        paint.setColor(Palette.PANEL);
        canvas.drawRect(0, top, getWidth(), getHeight(), paint);
        paint.setColor(Palette.withAlpha(Palette.BUTTON_EDGE, 90));
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

    /**
     * Whether play has stopped and the player is owed an explanation.
     *
     * <p>Being caught used to print one line and carry on at the next tap,
     * which meant the board jumped back to the start with no account of why.
     * Losing a life is the most important thing that happens in a run; it gets
     * the same full stop that escaping does.
     */
    private boolean verdictShowing() {
        return state.hasEscaped() || state.isRunOver() || state.hasFailed() || state.isCaught();
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
        if (!verdictShowing()) {
            return;
        }
        boolean escaped = state.hasEscaped();
        boolean caughtWithLives = state.isCaught() && !state.isRunOver();

        RectF banner = verdictBanner();
        paint.setColor(Palette.withAlpha(Palette.BACKDROP, 243));
        canvas.drawRoundRect(banner, 30, 30, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(escaped ? Palette.EXIT_OPEN : Palette.DANGER);
        canvas.drawRoundRect(banner, 30, 30, paint);
        paint.setStyle(Paint.Style.FILL);

        String headline;
        if (escaped) {
            headline = "ESCAPED";
        } else if (state.isRunOver()) {
            headline = "RUN OVER";
        } else if (caughtWithLives) {
            headline = "CAUGHT";
        } else {
            headline = "OUT OF TURNS";
        }

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(escaped ? Palette.EXIT_OPEN : Palette.DANGER);
        paint.setFakeBoldText(true);
        paint.setTextSize(banner.height() * 0.19f);
        canvas.drawText(headline, banner.centerX(), banner.top + banner.height() * 0.26f, paint);
        paint.setFakeBoldText(false);

        paint.setColor(Palette.TEXT_DIM);
        paint.setTextSize(banner.height() * 0.082f);
        if (escaped) {
            Score score = state.score();
            drawStars(canvas, banner, score.stars());
            canvas.drawText(score.turns() + " turns, par " + score.par()
                            + "   ·   seen " + score.sightings(),
                    banner.centerX(), banner.top + banner.height() * 0.58f, paint);
        } else if (caughtWithLives) {
            canvas.drawText("a hunter reached you", banner.centerX(),
                    banner.top + banner.height() * 0.4f, paint);
            paint.setColor(state.livesLeft() <= 1 ? Palette.DANGER : Palette.TEXT);
            paint.setTextSize(banner.height() * 0.1f);
            canvas.drawText(state.livesLeft() + (state.livesLeft() == 1 ? " life left" : " lives left"),
                    banner.centerX(), banner.top + banner.height() * 0.53f, paint);
        } else if (state.isRunOver()) {
            canvas.drawText("no lives left", banner.centerX(),
                    banner.top + banner.height() * 0.45f, paint);
        } else {
            canvas.drawText("the clock ran out", banner.centerX(),
                    banner.top + banner.height() * 0.45f, paint);
        }

        drawVerdictButton(canvas, verdictButton(true), "LEVELS", Palette.BUTTON_EDGE);
        String onward = escaped ? "CONTINUE" : (caughtWithLives ? "TRY AGAIN" : "RESTART");
        drawVerdictButton(canvas, verdictButton(false), onward,
                escaped ? Palette.EXIT_OPEN : Palette.DANGER);
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
            // Nothing else on the board responds while a verdict is up. Tapping
            // anywhere used to dismiss it, which is how a lost life went by
            // without the player knowing what had happened.
            if (verdictButton(true).contains(x, y)) {
                onLeave.run();
            } else if (verdictButton(false).contains(x, y)) {
                if (state.hasEscaped()) {
                    onNext.run();
                } else if (state.isCaught() && !state.isRunOver()) {
                    state.beginNextLife();
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
