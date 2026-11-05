package com.signallost.web;

import com.signallost.art.Critter;
import com.signallost.art.CritterPixels;
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

import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.browser.Storage;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Signal Lost in a browser.
 *
 * <p>The fourth front end on the same rules, and the only one that reaches an
 * iPhone: Apple will not run an Android package, but every phone has a browser,
 * and a page added to the home screen behaves like an installed app.
 *
 * <p>Two things differ from the other front ends, and both are handled at the
 * edges rather than by changing any rule. A browser has no classpath, so levels
 * arrive over the network and go through {@code LevelLoader.parseLevel} rather
 * than its resource loader. And it has no filesystem, so progress is kept in
 * local storage instead of {@code ProgressStore} — the same decision Android
 * made when it handed the store a different directory, taken one step further.
 */
public final class WebGame {

    private static final int BACKDROP = 0x0A0C12;
    private static final int PANEL = 0x141824;
    private static final int BUTTON = 0x1E2436;
    private static final int EDGE = 0x3A4664;
    private static final int WALL_SEEN = 0x8094C4;
    private static final int WALL_GONE = 0x2C344C;
    private static final int FLOOR_SEEN = 0x1A2030;
    private static final int FLOOR_GONE = 0x10141E;
    private static final int KEY = 0xF6C446;
    private static final int EXIT_OPEN = 0x5CD68C;
    private static final int EXIT_SHUT = 0x5A6480;
    private static final int TEXT = 0xE8ECF6;
    private static final int DIM = 0x8C96AF;
    private static final int DANGER = 0xE84654;
    private static final int ACCENT = 0x60AAFF;

    private static final String SAVE_KEY = "signal-lost.progress";

    private enum Screen { TITLE, CHARACTER, LEVELS, PLAYING }

    private static HTMLCanvasElement canvas;
    private static CanvasRenderingContext2D g;
    private static Screen screen = Screen.TITLE;
    private static Critter critter = Critter.defaultChoice();
    private static GameState state;
    private static String levelName;
    private static int hintTurn = -1;
    private static final List<String> LEVELS = new ArrayList<>();
    private static final List<Integer> UNLOCK = new ArrayList<>();

    private WebGame() {
    }

    public static void main(String[] args) {
        HTMLDocument document = Window.current().getDocument();
        canvas = (HTMLCanvasElement) document.getElementById("board");
        g = (CanvasRenderingContext2D) canvas.getContext("2d");

        canvas.addEventListener("click", (MouseEvent event) -> onClick(event));
        document.addEventListener("keydown", (KeyboardEvent event) -> onKey(event));
        Window.current().addEventListener("resize", e -> draw());

        loadCatalogue();
    }

    // ------------------------------------------------------------- loading

    /** The catalogue is a text file, same as the desktop reads from its jar. */
    private static void loadCatalogue() {
        fetch("levels/index.txt", body -> {
            for (String line : body.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split("\\s+");
                LEVELS.add(parts[0]);
                UNLOCK.add(Integer.parseInt(parts[1]));
            }
            resize();
            draw();
        });
    }

    private static void startLevel(String name) {
        fetch("levels/" + name + ".txt", body -> {
            Level level = LevelLoader.parseLevel(name, Arrays.asList(body.split("\n")));
            levelName = name;
            state = new GameState(level.map(), level.guards(),
                    GameState.DEFAULT_SIGHT_RADIUS, level.turnLimit());
            hintTurn = -1;
            screen = Screen.PLAYING;
            draw();
        });
    }

    private interface Received {
        void body(String text);
    }

    private static void fetch(String path, Received then) {
        XMLHttpRequest request = XMLHttpRequest.create();
        request.open("GET", path);
        request.setOnReadyStateChange(() -> {
            if (request.getReadyState() == XMLHttpRequest.DONE && request.getStatus() == 200) {
                then.body(request.getResponseText().replace("\r", ""));
            }
        });
        request.send();
    }

    // ------------------------------------------------------------- progress

    /** Stars per level, as "level1=3,level2=1" in local storage. */
    private static int starsOn(String name) {
        Storage saved = Storage.getLocalStorage();
        String all = saved == null ? null : saved.getItem(SAVE_KEY);
        if (all == null) {
            return 0;
        }
        for (String entry : all.split(",")) {
            String[] pair = entry.split("=");
            if (pair.length == 2 && pair[0].equals(name)) {
                return Integer.parseInt(pair[1]);
            }
        }
        return 0;
    }

    private static int totalStars() {
        int total = 0;
        for (String name : LEVELS) {
            total += starsOn(name);
        }
        return total;
    }

    private static void record(String name, int stars) {
        if (stars <= starsOn(name)) {
            return;
        }
        Storage saved = Storage.getLocalStorage();
        if (saved == null) {
            return;
        }
        StringBuilder rebuilt = new StringBuilder();
        for (String level : LEVELS) {
            int best = level.equals(name) ? stars : starsOn(level);
            if (best > 0) {
                rebuilt.append(rebuilt.length() > 0 ? "," : "").append(level).append("=").append(best);
            }
        }
        saved.setItem(SAVE_KEY, rebuilt.toString());
    }

    // --------------------------------------------------------------- layout

    private static void resize() {
        canvas.setWidth(Window.current().getInnerWidth());
        canvas.setHeight(Window.current().getInnerHeight());
    }

    private static int width() {
        return canvas.getWidth();
    }

    private static int height() {
        return canvas.getHeight();
    }

    private static double headerBottom() {
        return height() * 0.1;
    }

    private static double footerTop() {
        return height() * 0.86;
    }

    private static int tile() {
        GameMap map = state.map();
        double usable = footerTop() - headerBottom();
        double fitted = Math.min(width() / (double) map.cols(), usable / map.rows());
        return (int) Math.max(fitted, usable / 11);
    }

    private static double follow(double view, double board, double centre, double origin) {
        if (board <= view) {
            return origin + (view - board) / 2;
        }
        return Math.max(origin + view - board, Math.min(origin, origin + view / 2 - centre));
    }

    private static double boardLeft() {
        int t = tile();
        return follow(width(), t * state.map().cols(), (state.player().col() + 0.5) * t, 0);
    }

    private static double boardTop() {
        int t = tile();
        return follow(footerTop() - headerBottom(), t * state.map().rows(),
                (state.player().row() + 0.5) * t, headerBottom());
    }

    // ----------------------------------------------------------------- draw

    private static void fill(int colour) {
        g.setFillStyle("#" + String.format("%06x", colour));
    }

    private static void fillAlpha(int colour, double alpha) {
        int r = (colour >> 16) & 0xFF;
        int gr = (colour >> 8) & 0xFF;
        int b = colour & 0xFF;
        g.setFillStyle("rgba(" + r + "," + gr + "," + b + "," + alpha + ")");
    }

    private static void box(double x, double y, double w, double h, double radius, int colour) {
        fill(colour);
        roundPath(x, y, w, h, radius);
        g.fill();
    }

    private static void roundPath(double x, double y, double w, double h, double radius) {
        double r = Math.min(radius, Math.min(w, h) / 2);
        g.beginPath();
        g.moveTo(x + r, y);
        g.lineTo(x + w - r, y);
        g.quadraticCurveTo(x + w, y, x + w, y + r);
        g.lineTo(x + w, y + h - r);
        g.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
        g.lineTo(x + r, y + h);
        g.quadraticCurveTo(x, y + h, x, y + h - r);
        g.lineTo(x, y + r);
        g.quadraticCurveTo(x, y, x + r, y);
        g.closePath();
    }

    private static void text(String what, double x, double y, double size, int colour,
                             String align, boolean bold) {
        fill(colour);
        g.setFont((bold ? "bold " : "") + (int) size + "px system-ui, sans-serif");
        g.setTextAlign(align);
        g.fillText(what, x, y);
        g.setTextAlign("left");
    }

    private static void draw() {
        if (canvas == null) {
            return;
        }
        resize();
        fill(BACKDROP);
        g.fillRect(0, 0, width(), height());
        switch (screen) {
            case TITLE -> drawTitle();
            case CHARACTER -> drawCharacters();
            case LEVELS -> drawLevels();
            case PLAYING -> drawGame();
        }
    }

    private static void drawTitle() {
        double midX = width() / 2.0;
        double midY = height() / 2.0;
        for (int ring = 7; ring >= 1; ring--) {
            fillAlpha(ACCENT, 0.02 + ring * 0.008);
            g.beginPath();
            g.arc(midX, midY - height() * 0.06, height() * 0.07 * ring, 0, Math.PI * 2);
            g.fill();
        }
        text("SIGNAL LOST", midX, midY - height() * 0.04, height() * 0.15, TEXT, "center", true);
        text("a fog-of-war stealth game", midX, midY + height() * 0.03,
                height() * 0.04, DIM, "center", false);
        int stars = totalStars();
        if (stars > 0) {
            text(stars + " of " + (LEVELS.size() * 3) + " stars", midX, midY + height() * 0.1,
                    height() * 0.035, ACCENT, "center", false);
        }
        double[] start = titleButton();
        box(start[0], start[1], start[2], start[3], start[3] / 2, BUTTON);
        text("START", start[0] + start[2] / 2, start[1] + start[3] * 0.65,
                start[3] * 0.4, TEXT, "center", true);
    }

    private static double[] titleButton() {
        double w = Math.min(width() * 0.3, 420);
        double h = height() * 0.12;
        return new double[]{(width() - w) / 2, height() * 0.74, w, h};
    }

    private static void drawCharacters() {
        text("CHOOSE A CHARACTER", width() / 2.0, height() * 0.12,
                height() * 0.07, TEXT, "center", true);
        Critter[] roster = Critter.values();
        for (int i = 0; i < roster.length; i++) {
            double[] card = cardAt(i);
            boolean picked = roster[i] == critter;
            box(card[0], card[1], card[2], card[3], card[2] * 0.12, picked ? PANEL : BACKDROP);
            g.setStrokeStyle("#" + String.format("%06x",
                    picked ? roster[i].fur() : EDGE));
            g.setLineWidth(picked ? 4 : 2);
            roundPath(card[0], card[1], card[2], card[3], card[2] * 0.12);
            g.stroke();
            sprite(roster[i], card[0] + card[2] / 2, card[1] + card[3] * 0.36, card[2] * 0.62);
            text(roster[i].displayName(), card[0] + card[2] / 2, card[1] + card[3] * 0.76,
                    card[3] * 0.13, picked ? TEXT : DIM, "center", true);
            text(roster[i].tagline(), card[0] + card[2] / 2, card[1] + card[3] * 0.89,
                    card[3] * 0.075, DIM, "center", false);
        }
        double[] play = titleButton();
        box(play[0], play[1], play[2], play[3], play[3] / 2, BUTTON);
        text("PLAY AS " + critter.displayName().toUpperCase(), play[0] + play[2] / 2,
                play[1] + play[3] * 0.65, play[3] * 0.35, TEXT, "center", true);
    }

    private static double[] cardAt(int index) {
        int count = Critter.values().length;
        double margin = width() * 0.04;
        double gap = width() * 0.015;
        double w = (width() - margin * 2 - gap * (count - 1)) / count;
        return new double[]{margin + index * (w + gap), height() * 0.2, w, height() * 0.46};
    }

    private static void drawLevels() {
        text("CHOOSE A LEVEL", width() / 2.0, height() * 0.11,
                height() * 0.07, TEXT, "center", true);
        text(totalStars() + " of " + (LEVELS.size() * 3) + " stars",
                width() / 2.0, height() * 0.17, height() * 0.04, DIM, "center", false);
        for (int i = 0; i < LEVELS.size(); i++) {
            double[] b = levelBox(i);
            boolean open = totalStars() >= UNLOCK.get(i);
            int stars = starsOn(LEVELS.get(i));
            box(b[0], b[1], b[2], b[3], b[2] * 0.12, open ? PANEL : BACKDROP);
            g.setStrokeStyle("#" + String.format("%06x", stars > 0 ? ACCENT : EDGE));
            g.setLineWidth(3);
            roundPath(b[0], b[1], b[2], b[3], b[2] * 0.12);
            g.stroke();
            text(String.valueOf(i + 1), b[0] + b[2] / 2, b[1] + b[3] * 0.62,
                    b[3] * 0.34, open ? TEXT : EDGE, "center", true);
            if (open) {
                text("★".repeat(stars) + "☆".repeat(3 - stars), b[0] + b[2] / 2,
                        b[1] + b[3] * 0.85, b[3] * 0.16, stars > 0 ? KEY : EDGE, "center", false);
            } else {
                text(UNLOCK.get(i) + "★", b[0] + b[2] / 2, b[1] + b[3] * 0.85,
                        b[3] * 0.14, EDGE, "center", false);
            }
        }
    }

    private static double[] levelBox(int index) {
        int columns = 5;
        double margin = width() * 0.04;
        double gap = width() * 0.015;
        double w = (width() - margin * 2 - gap * (columns - 1)) / columns;
        double h = (height() * 0.72 - gap) / 2;
        return new double[]{margin + (index % columns) * (w + gap),
                height() * 0.22 + (index / columns) * (h + gap), w, h};
    }

    private static void drawGame() {
        GameMap map = state.map();
        int t = tile();
        g.save();
        g.beginPath();
        g.rect(0, headerBottom(), width(), footerTop() - headerBottom());
        g.clip();

        for (int row = 0; row < map.rows(); row++) {
            for (int col = 0; col < map.cols(); col++) {
                Position at = new Position(row, col);
                TileVisibility seen = state.fog().visibilityOf(at);
                if (seen == TileVisibility.UNEXPLORED) {
                    continue;
                }
                boolean lit = seen == TileVisibility.VISIBLE;
                TileType type = map.tileAt(at);
                double x = boardLeft() + col * t;
                double y = boardTop() + row * t;
                box(x + 1, y + 1, t - 2, t - 2, t * 0.16,
                        type == TileType.WALL ? (lit ? WALL_SEEN : WALL_GONE)
                                : (lit ? FLOOR_SEEN : FLOOR_GONE));
                if (lit && type == TileType.KEY) {
                    fill(KEY);
                    g.beginPath();
                    g.arc(x + t * 0.42, y + t * 0.5, t * 0.14, 0, Math.PI * 2);
                    g.fill();
                    g.fillRect(x + t * 0.48, y + t * 0.46, t * 0.26, t * 0.08);
                } else if (lit && type == TileType.EXIT) {
                    box(x + t * 0.2, y + t * 0.2, t * 0.6, t * 0.6, t * 0.1,
                            state.hasAllKeys() ? EXIT_OPEN : EXIT_SHUT);
                }
            }
        }

        for (Guard guard : state.guards()) {
            if (!state.fog().isVisible(guard.position())) {
                continue;
            }
            int colour = guard.mode() == GuardMode.HUNT ? DANGER
                    : guard.mode() == GuardMode.ALERT ? KEY : 0x6088D4;
            for (Position watched : guard.watchedTiles(map)) {
                if (state.fog().visibilityOf(watched) == TileVisibility.VISIBLE) {
                    fillAlpha(DANGER, guard.mode() == GuardMode.HUNT ? 0.55 : 0.4);
                    roundPath(boardLeft() + watched.col() * t + 1,
                            boardTop() + watched.row() * t + 1, t - 2, t - 2, t * 0.16);
                    g.fill();
                }
            }
            double gx = boardLeft() + guard.position().col() * t;
            double gy = boardTop() + guard.position().row() * t;
            fillAlpha(colour, 0.22);
            g.beginPath();
            g.arc(gx + t / 2.0, gy + t / 2.0, t * 0.6, 0, Math.PI * 2);
            g.fill();
            if ("Hunter".equals(guard.type())) {
                fill(colour);
                g.beginPath();
                g.moveTo(gx + t * 0.2, gy + t * 0.4);
                g.lineTo(gx + t * 0.32, gy + t * 0.08);
                g.lineTo(gx + t * 0.46, gy + t * 0.36);
                g.lineTo(gx + t * 0.54, gy + t * 0.36);
                g.lineTo(gx + t * 0.68, gy + t * 0.08);
                g.lineTo(gx + t * 0.8, gy + t * 0.4);
                g.closePath();
                g.fill();
            }
            box(gx + t * 0.16, gy + t * 0.16, t * 0.68, t * 0.68, t * 0.22, colour);
        }

        if (hintTurn == state.turnsTaken()) {
            List<Position> route = state.hintRoute(1);
            if (!route.isEmpty()) {
                g.setStrokeStyle("#" + String.format("%06x",
                        state.hintSeeksCover() ? KEY : ACCENT));
                g.setLineWidth(t * 0.1);
                roundPath(boardLeft() + route.get(0).col() * t + 1,
                        boardTop() + route.get(0).row() * t + 1, t - 2, t - 2, t * 0.16);
                g.stroke();
            }
        }

        double px = boardLeft() + state.player().col() * t;
        double py = boardTop() + state.player().row() * t;
        fillAlpha(critter.fur(), 0.18);
        g.beginPath();
        g.arc(px + t / 2.0, py + t / 2.0, t * 0.65, 0, Math.PI * 2);
        g.fill();
        sprite(critter, px + t / 2.0, py + t / 2.0, t * 0.92);
        g.restore();

        drawBars();
        drawVerdict();
    }

    private static void drawBars() {
        fill(PANEL);
        g.fillRect(0, 0, width(), headerBottom());
        g.fillRect(0, footerTop(), width(), height() - footerTop());

        sprite(critter, width() * 0.03, headerBottom() * 0.5, headerBottom() * 0.6);
        text(levelName.replaceAll("(?<=\\D)(?=\\d)", " ").toUpperCase(),
                width() * 0.06, headerBottom() * 0.66, headerBottom() * 0.4, TEXT, "left", true);

        String stats = "KEYS " + state.keysHeld() + "/" + state.keysRequired()
                + "    LIVES " + state.livesLeft()
                + "    HINTS " + state.hintsLeft()
                + "    SEEN " + state.sightings()
                + "    TURNS " + (state.hasTurnLimit() ? state.turnsRemaining() : state.turnsTaken());
        text(stats, width() * 0.97, headerBottom() * 0.66, headerBottom() * 0.3,
                state.sightings() > 0 ? DANGER : DIM, "right", false);

        String[] labels = {"UNDO", "HINT " + state.hintsLeft(), "RESTART", "LEVELS"};
        for (int i = 0; i < labels.length; i++) {
            double[] b = footerButton(i);
            box(b[0], b[1], b[2], b[3], b[3] * 0.3, BUTTON);
            text(labels[i], b[0] + b[2] / 2, b[1] + b[3] * 0.65, b[3] * 0.38, TEXT, "center", false);
        }
    }

    private static double[] footerButton(int index) {
        double pad = width() * 0.02;
        double gap = width() * 0.012;
        double w = (width() - pad * 2 - gap * 3) / 4;
        double top = footerTop() + height() * 0.015;
        return new double[]{pad + index * (w + gap), top, w, height() - top - height() * 0.02};
    }

    private static boolean verdictShowing() {
        return state != null && (state.hasEscaped() || state.isRunOver()
                || state.hasFailed() || state.isCaught());
    }

    private static double[] verdictBanner() {
        double w = Math.min(width() * 0.5, 760);
        double h = height() * 0.5;
        return new double[]{(width() - w) / 2, (height() - h) / 2, w, h};
    }

    private static double[] verdictButton(boolean left) {
        double[] b = verdictBanner();
        double w = b[2] * 0.38;
        double h = b[3] * 0.24;
        double y = b[1] + b[3] - h - b[3] * 0.1;
        double x = left ? b[0] + b[2] / 2 - w - b[2] * 0.02 : b[0] + b[2] / 2 + b[2] * 0.02;
        return new double[]{x, y, w, h};
    }

    private static void drawVerdict() {
        if (!verdictShowing()) {
            return;
        }
        boolean escaped = state.hasEscaped();
        boolean caught = state.isCaught() && !state.isRunOver();
        double[] b = verdictBanner();
        fillAlpha(BACKDROP, 0.95);
        roundPath(b[0], b[1], b[2], b[3], 24);
        g.fill();
        g.setStrokeStyle("#" + String.format("%06x", escaped ? EXIT_OPEN : DANGER));
        g.setLineWidth(3);
        roundPath(b[0], b[1], b[2], b[3], 24);
        g.stroke();

        String headline = escaped ? "ESCAPED"
                : state.isRunOver() ? "RUN OVER" : caught ? "CAUGHT" : "OUT OF TURNS";
        text(headline, b[0] + b[2] / 2, b[1] + b[3] * 0.3, b[3] * 0.19,
                escaped ? EXIT_OPEN : DANGER, "center", true);

        String detail = escaped
                ? state.score().stars() + " of 3 stars   ·   " + state.score().turns()
                        + " turns, par " + state.score().par()
                : caught ? "a hunter reached you   ·   " + state.livesLeft() + " lives left"
                        : "better luck next time";
        text(detail, b[0] + b[2] / 2, b[1] + b[3] * 0.48, b[3] * 0.085, DIM, "center", false);

        double[] left = verdictButton(true);
        double[] right = verdictButton(false);
        box(left[0], left[1], left[2], left[3], left[3] * 0.3, BUTTON);
        text("LEVELS", left[0] + left[2] / 2, left[1] + left[3] * 0.65,
                left[3] * 0.34, TEXT, "center", true);
        box(right[0], right[1], right[2], right[3], right[3] * 0.3, BUTTON);
        text(escaped ? "CONTINUE" : caught ? "TRY AGAIN" : "RESTART",
                right[0] + right[2] / 2, right[1] + right[3] * 0.65,
                right[3] * 0.34, TEXT, "center", true);
    }

    /** The shared 16x16 art, one filled rectangle per pixel. */
    private static void sprite(Critter who, double cx, double cy, double size) {
        String[] rows = CritterPixels.shapeOf(who);
        double step = Math.max(1, Math.floor(size / rows.length));
        double left = cx - step * rows.length / 2;
        double top = cy - step * rows.length / 2;
        for (int row = 0; row < rows.length; row++) {
            for (int col = 0; col < rows[row].length(); col++) {
                int packed = CritterPixels.colourOf(who, rows[row].charAt(col));
                if (packed == CritterPixels.TRANSPARENT) {
                    continue;
                }
                fill(packed);
                g.fillRect(left + col * step, top + row * step, step + 0.5, step + 0.5);
            }
        }
    }

    // ---------------------------------------------------------------- input

    private static boolean inside(double[] box, double x, double y) {
        return x >= box[0] && x <= box[0] + box[2] && y >= box[1] && y <= box[1] + box[3];
    }

    private static void onClick(MouseEvent event) {
        double x = event.getClientX();
        double y = event.getClientY();
        switch (screen) {
            case TITLE -> {
                if (inside(titleButton(), x, y)) {
                    screen = Screen.CHARACTER;
                }
            }
            case CHARACTER -> {
                if (inside(titleButton(), x, y)) {
                    screen = Screen.LEVELS;
                } else {
                    Critter[] roster = Critter.values();
                    for (int i = 0; i < roster.length; i++) {
                        if (inside(cardAt(i), x, y)) {
                            critter = roster[i];
                        }
                    }
                }
            }
            case LEVELS -> {
                for (int i = 0; i < LEVELS.size(); i++) {
                    if (inside(levelBox(i), x, y) && totalStars() >= UNLOCK.get(i)) {
                        startLevel(LEVELS.get(i));
                        return;
                    }
                }
            }
            case PLAYING -> clickInGame(x, y);
            default -> { }
        }
        draw();
    }

    private static void clickInGame(double x, double y) {
        if (verdictShowing()) {
            if (inside(verdictButton(true), x, y)) {
                screen = Screen.LEVELS;
            } else if (inside(verdictButton(false), x, y)) {
                if (state.hasEscaped()) {
                    nextLevel();
                } else if (state.isCaught() && !state.isRunOver()) {
                    state.beginNextLife();
                } else {
                    startLevel(levelName);
                }
            }
            return;
        }
        if (y >= footerTop()) {
            if (inside(footerButton(0), x, y)) {
                state.undoLastMove();
            } else if (inside(footerButton(1), x, y)) {
                if (hintTurn != state.turnsTaken() && state.spendHint()) {
                    hintTurn = state.turnsTaken();
                }
            } else if (inside(footerButton(2), x, y)) {
                startLevel(levelName);
            } else if (inside(footerButton(3), x, y)) {
                screen = Screen.LEVELS;
            }
            return;
        }
        if (y < headerBottom()) {
            return;
        }
        int t = tile();
        int downwards = (int) Math.floor((y - boardTop()) / t) - state.player().row();
        int rightwards = (int) Math.floor((x - boardLeft()) / t) - state.player().col();
        if (downwards == 0 && rightwards == 0) {
            state.movePlayer(Direction.WAIT);
        } else {
            state.movePlayer(Math.abs(downwards) >= Math.abs(rightwards)
                    ? (downwards < 0 ? Direction.UP : Direction.DOWN)
                    : (rightwards < 0 ? Direction.LEFT : Direction.RIGHT));
        }
        if (state.hasEscaped()) {
            record(levelName, state.score().stars());
        }
    }

    private static void nextLevel() {
        int at = LEVELS.indexOf(levelName);
        if (at >= 0 && at + 1 < LEVELS.size() && totalStars() >= UNLOCK.get(at + 1)) {
            startLevel(LEVELS.get(at + 1));
        } else {
            screen = Screen.LEVELS;
            draw();
        }
    }

    /** Arrow keys and WASD, because a laptop is a browser too. */
    private static void onKey(KeyboardEvent event) {
        if (screen != Screen.PLAYING || state == null || verdictShowing()) {
            return;
        }
        Direction move = switch (event.getKey()) {
            case "ArrowUp", "w", "W" -> Direction.UP;
            case "ArrowDown", "s", "S" -> Direction.DOWN;
            case "ArrowLeft", "a", "A" -> Direction.LEFT;
            case "ArrowRight", "d", "D" -> Direction.RIGHT;
            case " " -> Direction.WAIT;
            default -> null;
        };
        if (move != null) {
            event.preventDefault();
            state.movePlayer(move);
            if (state.hasEscaped()) {
                record(levelName, state.score().stars());
            }
            draw();
        } else if ("u".equals(event.getKey())) {
            state.undoLastMove();
            draw();
        }
    }
}
