package com.signallost.web;

import com.signallost.art.Critter;
import com.signallost.art.CritterPixels;
import com.signallost.art.GuardPixels;
import com.signallost.art.ObjectPixels;
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
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
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

    // Every value below is the desktop's, from com.signallost.ui.Palette. They
    // were picked by hand before and every one was a near-miss, which is most of
    // why the browser looked like a different game.
    private static final int BACKDROP = 0x05070C;
    private static final int BACKDROP_TOP = 0x17203C;
    private static final int BACKDROP_BOTTOM = 0x080B12;
    private static final int PANEL = 0x141824;
    private static final int BUTTON = 0x1E2436;
    private static final int EDGE = 0x3A4664;
    private static final int PLAYER_GLOW = 0x5EEAD4;
    private static final int HUD_EDGE = 0x1C2638;
    private static final int GUARD_HUNT = 0xF87171;
    private static final int WALL_SEEN = 0x6E86B2;
    private static final int WALL_GONE = 0x3D4D70;
    private static final int FLOOR_SEEN = 0x24324B;
    private static final int FLOOR_GONE = 0x121828;
    private static final int KEY = 0xFBBF24;
    private static final int EXIT_OPEN = 0x4ADE80;
    private static final int EXIT_SHUT = 0x9AA8BF;
    private static final int TEXT = 0xE6EDF7;
    private static final int DIM = 0x75849E;
    private static final int DANGER = 0xEF4444;
    private static final int ACCENT = 0x38BDF8;

    /**
     * Progress is stored per player, as on the phone.
     *
     * <p>A browser is shared more often than a phone is, so one anonymous pile
     * of stars would mean whoever played last overwrote whoever played before.
     * The key carries the name; {@code LAST_PLAYER} is only so the title screen
     * can say hello.
     */
    private static final String SAVE_PREFIX = "signal-lost.progress.";
    private static final String LAST_PLAYER = "signal-lost.player";

    private enum Screen { TITLE, NAME, CHARACTER, LEVELS, PLAYING, GOODBYE }

    private static HTMLCanvasElement canvas;
    private static CanvasRenderingContext2D g;
    private static Screen screen = Screen.TITLE;
    private static Critter critter = Critter.defaultChoice();
    private static String player = "";
    private static HTMLInputElement nameField;
    private static boolean nameMissing;
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
        // Covers the case above: a page shown after it was loaded hidden.
        document.addEventListener("visibilitychange", e -> draw());
        Window.current().addEventListener("pageshow", e -> draw());

        nameField = (HTMLInputElement) document.getElementById("name");
        hideNameField();
        loadCatalogue();
    }

    /**
     * The one piece of the game that is not drawn.
     *
     * <p>Reimplementing a text field and a keyboard on a canvas to keep the
     * styling uniform would be a lot of work to arrive somewhere worse, so the
     * name screen borrows the browser's own input exactly as the phone borrows
     * Android's.
     */
    private static void showNameField() {
        HTMLElement style = nameField;
        nameField.getStyle().setProperty("display", "block");
        nameField.setValue(player.isEmpty() ? lastPlayer() : player);
        nameField.focus();
    }

    private static void hideNameField() {
        if (nameField != null) {
            nameField.getStyle().setProperty("display", "none");
        }
    }

    private static void acceptName() {
        String typed = nameField.getValue().trim();
        if (typed.isEmpty()) {
            // The name is the save key, not a greeting. Falling back to
            // "Player" meant everyone who skipped the box shared one set of
            // stars and watched their progress change under them.
            nameMissing = true;
            nameField.focus();
            draw();
            return;
        }
        nameMissing = false;
        player = typed.length() > 40 ? typed.substring(0, 40) : typed;
        rememberLastPlayer();
        hideNameField();
        screen = Screen.CHARACTER;
        draw();
    }

    // ------------------------------------------------------------- loading

    /** The catalogue is a text file, same as the desktop reads from its jar. */
    /** Who played here last, so the title can greet them. */
    private static void rememberLastPlayer() {
        Storage saved = Storage.getLocalStorage();
        if (saved != null && !player.isEmpty()) {
            saved.setItem(LAST_PLAYER, player);
        }
    }

    private static String lastPlayer() {
        Storage saved = Storage.getLocalStorage();
        String name = saved == null ? null : saved.getItem(LAST_PLAYER);
        return name == null ? "" : name;
    }

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
    private static String saveKey() {
        return SAVE_PREFIX + player.toLowerCase();
    }

    private static int starsOn(String name) {
        Storage saved = Storage.getLocalStorage();
        String all = saved == null ? null : saved.getItem(saveKey());
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

    /** The greeting on the title screen needs a total before anyone has signed in. */
    private static int totalStarsFor(String who) {
        String keep = player;
        player = who;
        int total = totalStars();
        player = keep;
        return total;
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
        saved.setItem(saveKey(), rebuilt.toString());
    }

    // --------------------------------------------------------------- layout

    /**
     * Matches the canvas to the window, and refuses to accept nothing.
     *
     * <p>A page that is loaded while hidden — a background tab, a web app
     * restored but not yet shown — reports an inner size of zero. Taking that
     * at face value gives a canvas of no size, so the game draws nothing, and
     * because it only redraws on input it stays blank even once the tab is
     * looked at. A floor plus a redraw when the page becomes visible is the
     * difference between that and a game.
     */
    private static void resize() {
        int w = Math.max(320, Window.current().getInnerWidth());
        int h = Math.max(240, Window.current().getInnerHeight());
        if (canvas.getWidth() != w || canvas.getHeight() != h) {
            canvas.setWidth(w);
            canvas.setHeight(h);
        }
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

    /**
     * How brightly a tile is lit, by how far the light had to travel to it.
     *
     * <p>The same curve the desktop uses, over the same step counts: sight is a
     * flood fill, so every visible tile already knows how many steps from the
     * player it is. Without this the lit area is one flat sheet and the torch
     * stops feeling like a torch.
     */
    private static double falloff(Position at) {
        if (state.fog().isRevealAll()) {
            return 1.0;
        }
        int distance = state.fog().sightDistance(at);
        if (distance < 0 || state.fog().sightRadius() == 0) {
            return 1.0;
        }
        return 1.0 - 0.5 * ((double) distance / state.fog().sightRadius());
    }

    private static int shade(int colour, double factor) {
        double f = Math.max(0, Math.min(1, factor));
        int r = (int) (((colour >> 16) & 0xFF) * f);
        int g2 = (int) (((colour >> 8) & 0xFF) * f);
        int b = (int) ((colour & 0xFF) * f);
        return (r << 16) | (g2 << 8) | b;
    }

    private static void fill(int colour) {
        g.setFillStyle("#" + String.format("%06x", colour));
    }

    private static void fillAlpha(int colour, double alpha) {
        int r = (colour >> 16) & 0xFF;
        int gr = (colour >> 8) & 0xFF;
        int b = colour & 0xFF;
        g.setFillStyle("rgba(" + r + "," + gr + "," + b + "," + alpha + ")");
    }

    private static void strokeAlpha(int colour, double alpha) {
        int r = (colour >> 16) & 0xFF;
        int gr = (colour >> 8) & 0xFF;
        int b = colour & 0xFF;
        g.setStrokeStyle("rgba(" + r + "," + gr + "," + b + "," + alpha + ")");
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
        backdrop();
        switch (screen) {
            case TITLE -> drawTitle();
            case NAME -> drawName();
            case CHARACTER -> drawCharacters();
            case LEVELS -> drawLevels();
            case PLAYING -> drawGame();
            case GOODBYE -> drawGoodbye();
        }
    }

    /**
     * The vertical wash the desktop paints behind every screen.
     *
     * <p>A stack of strips rather than a canvas gradient object, so the same
     * arithmetic runs on all three front ends over the same two colours.
     */
    private static void backdrop() {
        int strips = 48;
        double band = height() / (double) strips + 1;
        for (int i = 0; i < strips; i++) {
            fill(Critter.blend(BACKDROP_TOP, BACKDROP_BOTTOM, i / (double) (strips - 1)));
            g.fillRect(0, i * (height() / (double) strips), width(), band);
        }
    }

    private static void drawTitle() {
        double midX = width() / 2.0;
        // The block is centred as one piece, so a wide window does not strand
        // the heading near the top. The desktop centres the same way.
        // Sized against the smaller of the two dimensions, not the height
        // alone: on a short, wide window a height-sized title runs off both
        // edges, which is exactly what a laptop browser is.
        double unit = Math.min(height(), width() * 0.9);
        double top = height() * 0.5 - unit * 0.34;

        for (int ring = 7; ring >= 1; ring--) {
            fillAlpha(PLAYER_GLOW, 0.012 + ring * 0.004);
            g.beginPath();
            g.arc(midX, top + unit * 0.14, unit * 0.055 * ring, 0, Math.PI * 2);
            g.fill();
        }

        text("SIGNAL LOST", midX, top + unit * 0.17, unit * 0.14, TEXT, "center", true);
        text("Find the keys. Reach the exit. Do not be seen.",
                midX, top + unit * 0.245, unit * 0.042, ACCENT, "center", false);
        text(LEVELS.size() + " levels  ·  three stars each",
                midX, top + unit * 0.3, unit * 0.036, DIM, "center", false);

        // The whole cast lined up, as on the desktop: a reason to look at the
        // screen, and a promise that the characters are choosable.
        Critter[] cast = Critter.values();
        double size = unit * 0.115;
        double gap = size * 0.4;
        double total = cast.length * size + (cast.length - 1) * gap;
        double castX = midX - total / 2 + size / 2;
        for (int i = 0; i < cast.length; i++) {
            sprite(cast[i], castX + i * (size + gap), top + unit * 0.4, size);
        }

        String back = lastPlayer();
        if (!back.isEmpty()) {
            text("Welcome back, " + back.toUpperCase(),
                    midX, top + unit * 0.52, unit * 0.042, TEXT, "center", true);
            text(totalStarsFor(back) + " of " + (LEVELS.size() * 3) + " stars collected",
                    midX, top + unit * 0.565, unit * 0.036, KEY, "center", false);
        }

        double[] start = startButton();
        box(start[0], start[1], start[2], start[3], start[3] / 2, BUTTON);
        text("START", start[0] + start[2] / 2, start[1] + start[3] * 0.65,
                start[3] * 0.4, TEXT, "center", true);
        double[] quit = quitButton();
        box(quit[0], quit[1], quit[2], quit[3], quit[3] / 2, BUTTON);
        text("QUIT", quit[0] + quit[2] / 2, quit[1] + quit[3] * 0.65,
                quit[3] * 0.4, DIM, "center", true);
    }

    private static void drawName() {
        drawBack();
        text("WHO IS PLAYING?", width() / 2.0, height() * 0.3,
                height() * 0.09, TEXT, "center", true);
        if (nameMissing) {
            text("a name is needed \u2014 your stars are saved under it",
                    width() / 2.0, height() * 0.38, height() * 0.04, DANGER, "center", true);
        } else {
            text("your stars are saved under this name", width() / 2.0, height() * 0.38,
                    height() * 0.04, DIM, "center", false);
        }
        double[] go = titleButton();
        box(go[0], go[1], go[2], go[3], go[3] / 2, BUTTON);
        text("CONTINUE", go[0] + go[2] / 2, go[1] + go[3] * 0.65,
                go[3] * 0.36, TEXT, "center", true);
    }

    /**
     * A browser cannot close its own tab unless a script opened it, so quitting
     * says goodbye rather than pretending. Installed to a home screen it is the
     * last thing a player sees before they swipe away.
     */
    private static void drawGoodbye() {
        text("THANKS FOR PLAYING", width() / 2.0, height() * 0.42,
                height() * 0.1, TEXT, "center", true);
        if (!player.isEmpty()) {
            text(player + "  ·  " + totalStars() + " of " + (LEVELS.size() * 3) + " stars",
                    width() / 2.0, height() * 0.52, height() * 0.045, ACCENT, "center", false);
        }
        text("you can close this tab", width() / 2.0, height() * 0.6,
                height() * 0.035, DIM, "center", false);
        double[] again = titleButton();
        box(again[0], again[1], again[2], again[3], again[3] / 2, BUTTON);
        text("PLAY AGAIN", again[0] + again[2] / 2, again[1] + again[3] * 0.65,
                again[3] * 0.36, TEXT, "center", true);
    }

    /** A chevron and nothing else, as on the phone: labels make it a form. */
    private static double[] backBox() {
        double size = height() * 0.1;
        double margin = height() * 0.04;
        return new double[]{margin, margin, size, size};
    }

    private static void drawBack() {
        double[] b = backBox();
        box(b[0], b[1], b[2], b[3], b[3] * 0.3, BUTTON);
        g.setStrokeStyle("#" + String.format("%06x", TEXT));
        g.setLineWidth(Math.max(3, b[3] * 0.09));
        double cx = b[0] + b[2] / 2;
        double cy = b[1] + b[3] / 2;
        double arm = b[3] * 0.2;
        g.beginPath();
        g.moveTo(cx + arm * 0.5, cy - arm);
        g.lineTo(cx - arm * 0.5, cy);
        g.lineTo(cx + arm * 0.5, cy + arm);
        g.stroke();
    }

    private static double[] buttonRow(double top) {
        double w = Math.min(width() * 0.24, 360);
        double gap = width() * 0.025;
        return new double[]{width() / 2.0 - w - gap / 2, top, w, height() * 0.12};
    }

    private static double[] titleButton() {
        return buttonRow(height() * 0.74);
    }

    /**
     * START and QUIT, which sit lower than the other screens' single button.
     *
     * <p>The title screen has two lines above them for a returning player, and
     * at the shared height the greeting ran into the buttons.
     */
    private static double[] startButton() {
        return buttonRow(height() * 0.78);
    }

    private static double[] quitButton() {
        double[] start = startButton();
        return new double[]{start[0] + start[2] + width() * 0.025, start[1], start[2], start[3]};
    }

    private static void drawCharacters() {
        drawBack();
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
            sprite(roster[i], card[0] + card[2] / 2, card[1] + card[3] * 0.36, card[2] * 0.56);
            fitted(roster[i].displayName(), card[0] + card[2] / 2, card[1] + card[3] * 0.74,
                    card[2] * 0.26, card[2] * 0.82, picked ? TEXT : DIM, true);
            fitted(roster[i].tagline(), card[0] + card[2] / 2, card[1] + card[3] * 0.88,
                    card[2] * 0.13, card[2] * 0.86, DIM, false);
        }
        double[] play = titleButton();
        box(play[0], play[1], play[2], play[3], play[3] / 2, BUTTON);
        text("PLAY AS " + critter.displayName().toUpperCase(), play[0] + play[2] / 2,
                play[1] + play[3] * 0.65, play[3] * 0.35, TEXT, "center", true);
    }

    /**
     * One card in a centred row of six.
     *
     * <p>The width is capped rather than stretched to fill: six cards spread
     * across a wide window became six billboards, and nothing on the screen
     * needed to be that size. Whatever the row comes to, it is centred.
     */
    private static double[] cardAt(int index) {
        int count = Critter.values().length;
        double gap = width() * 0.015;
        double available = width() * 0.92 - gap * (count - 1);
        double w = Math.min(available / count, height() * 0.2);
        double h = Math.min(w * 1.6, height() * 0.46);
        double left = (width() - (w * count + gap * (count - 1))) / 2;
        return new double[]{left + index * (w + gap), height() * 0.22, w, h};
    }

    /**
     * Centred text that shrinks rather than overrun the box it belongs to.
     *
     * <p>Sizing these from the screen let a short tagline fit and a long one
     * spill into the next card along, which reads as a layout fault even though
     * every card is the same size.
     */
    private static void fitted(String what, double centreX, double baseline,
                               double size, double maxWidth, int colour, boolean bold) {
        double font = size;
        for (int attempt = 0; attempt < 12; attempt++) {
            g.setFont((bold ? "bold " : "") + (int) font + "px system-ui, sans-serif");
            if (g.measureText(what).getWidth() <= maxWidth || font <= 7) {
                break;
            }
            font *= 0.9;
        }
        text(what, centreX, baseline, font, colour, "center", bold);
    }

    private static void drawLevels() {
        drawBack();
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
                double light = lit ? falloff(at) : 1.0;
                int base = type == TileType.WALL ? (lit ? WALL_SEEN : WALL_GONE)
                        : (lit ? FLOOR_SEEN : FLOOR_GONE);
                box(x + 1, y + 1, t - 2, t - 2, t * 0.16, lit ? shade(base, light) : base);
                if (lit && type == TileType.KEY) {
                    tinted(ObjectPixels.key(), ObjectPixels.KEY_COLOUR,
                            x + t / 2.0, y + t / 2.0, t * 0.78);
                } else if (lit && type == TileType.EXIT) {
                    boolean open = state.hasAllKeys();
                    tinted(ObjectPixels.exit(open),
                            open ? ObjectPixels.EXIT_UNLOCKED : ObjectPixels.EXIT_LOCKED,
                            x + t / 2.0, y + t / 2.0, t * 0.86);
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
                    // Red means one thing: do not end your turn here. These are the
            // desktop's exact strengths. Guessing at them gave a wash twice as
            // heavy, which turned most of a lit room red and left the player
            // nothing to read.
                    fillAlpha(DANGER, dangerStrength(guard.mode()));
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
            // The robot, and only the robot. Markers for hunters and sentinels
            // were tried and dropped: they told the player which guard would
            // chase them, and made a clean sprite look like a warning sign.
            // The lens colour carries the state, as it does on the desktop.
            pixels(GuardPixels.shape(), GuardPixels.paletteFor(guard.mode().name()),
                    gx + t / 2.0, gy + t / 2.0, t * 0.95);
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
        // Several faint discs rather than one: a soft pool of torchlight,
        // which is what makes the dark around it read as dark.
        for (int ring = 5; ring >= 1; ring--) {
            fillAlpha(critter.fur(), 0.035);
            g.beginPath();
            g.arc(px + t / 2.0, py + t / 2.0, t * 0.5 * ring, 0, Math.PI * 2);
            g.fill();
        }
        sprite(critter, px + t / 2.0, py + t / 2.0, t * 0.92);
        g.restore();

        drawBars();
        drawVerdict();
    }

    private static void drawBars() {
        fill(PANEL);
        g.fillRect(0, 0, width(), headerBottom());
        g.fillRect(0, footerTop(), width(), height() - footerTop());
        fill(HUD_EDGE);
        g.fillRect(0, headerBottom() - 1, width(), 1);
        g.fillRect(0, footerTop(), width(), 1);

        drawIdentity();
        drawReadings();

        String[] labels = {"UNDO", "HINT " + state.hintsLeft(), "RESTART", "LEVELS"};
        for (int i = 0; i < labels.length; i++) {
            double[] b = footerButton(i);
            box(b[0], b[1], b[2], b[3], b[3] * 0.3, BUTTON);
            text(labels[i], b[0] + b[2] / 2, b[1] + b[3] * 0.65, b[3] * 0.38, TEXT, "center", false);
        }
    }

    /** Who you are playing as, and where, at the left-hand end. */
    private static void drawIdentity() {
        double h = headerBottom();
        sprite(critter, width() * 0.028, h * 0.5, h * 0.62);
        text(critter.displayName(), identityLeft(), h * 0.46, h * 0.34, TEXT, "left", true);
        text(prettyLevel() + "  \u00b7  SIGHT " + state.fog().sightRadius(),
                identityLeft(), h * 0.82, h * 0.24, DIM, "left", false);
    }

    private static double identityLeft() {
        return width() * 0.028 + headerBottom() * 0.45;
    }

    private static String prettyLevel() {
        return levelName.replaceAll("(?<=\\D)(?=\\d)", " ").toUpperCase();
    }

    /**
     * The chips and the turn counter.
     *
     * <p>Chips are laid out right to left and the counter takes what is left,
     * so when the row is too long the least important chips are dropped first
     * rather than letting the counter slide over the player's name. On a phone
     * in landscape that is most of the time.
     */
    private static void drawReadings() {
        List<String[]> chips = new ArrayList<>();
        if (state.fog().isRevealAll()) {
            chips.add(reading("DEV REVEAL", ACCENT, 1));
        }
        if (state.wasSpotted()) {
            chips.add(reading("SPOTTED", DANGER, 5));
        }
        GuardMode alert = state.highestAlertLevel();
        if (alert != null && alert != GuardMode.PATROL) {
            chips.add(reading(alert.label().toUpperCase(),
                    alert == GuardMode.HUNT ? GUARD_HUNT : KEY, 4));
        }
        if (state.sightings() > 0) {
            chips.add(reading("SEEN " + state.sightings(), KEY, 3));
        }
        if (state.canUndo()) {
            chips.add(reading("UNDO " + state.undoDepth(), ACCENT, 2));
        }
        chips.add(reading(state.livesLeft() == 1 ? "LAST LIFE" : "LIVES " + state.livesLeft(),
                livesColour(), 7));
        chips.add(reading(state.hintsLeft() == 0 ? "NO HINTS" : "HINTS " + state.hintsLeft(),
                hintsColour(), 6));
        chips.add(reading("KEYS " + state.keysHeld() + "/" + state.keysRequired(),
                state.hasAllKeys() ? EXIT_OPEN : KEY, 8));

        double room = width() * 0.97 - identityRightEdge() - headerBottom() * 0.3 - counterWidth();
        while (chips.size() > 1 && rowWidth(chips) > room) {
            int weakest = 0;
            for (int i = 1; i < chips.size(); i++) {
                if (rank(chips.get(i)) < rank(chips.get(weakest))) {
                    weakest = i;
                }
            }
            chips.remove(weakest);
        }

        double x = width() * 0.97;
        for (String[] chip : chips) {
            x = drawChip(x, chip[0], Integer.parseInt(chip[1]));
        }
        drawTurnCounter(x);
    }

    private static String[] reading(String label, int colour, int rank) {
        return new String[]{label, String.valueOf(colour), String.valueOf(rank)};
    }

    private static int rank(String[] chip) {
        return Integer.parseInt(chip[2]);
    }

    private static double rowWidth(List<String[]> chips) {
        double total = 0;
        for (String[] chip : chips) {
            total += chipWidth(chip[0]) + headerBottom() * 0.1;
        }
        return total;
    }

    private static double chipFont() {
        return headerBottom() * 0.26;
    }

    private static double chipWidth(String label) {
        g.setFont("bold " + (int) chipFont() + "px system-ui, sans-serif");
        return g.measureText(label).getWidth() + headerBottom() * 0.62;
    }

    /** Where the identity block stops, so nothing is drawn across it. */
    private static double identityRightEdge() {
        g.setFont("bold " + (int) (headerBottom() * 0.34) + "px system-ui, sans-serif");
        double name = g.measureText(critter.displayName()).getWidth();
        g.setFont((int) (headerBottom() * 0.24) + "px system-ui, sans-serif");
        double beneath = g.measureText(
                prettyLevel() + "  \u00b7  SIGHT " + state.fog().sightRadius()).getWidth();
        return identityLeft() + Math.max(name, beneath);
    }

    private static int hintsColour() {
        int hints = state.hintsLeft();
        if (hints == 0) {
            return DANGER;
        }
        return hints == 1 ? KEY : DIM;
    }

    private static int livesColour() {
        int lives = state.livesLeft();
        if (lives <= 1) {
            return DANGER;
        }
        return lives == 2 ? KEY : DIM;
    }

    /** A pill right-aligned at {@code rightEdge}, returning the next right edge. */
    private static double drawChip(double rightEdge, String label, int colour) {
        double font = chipFont();
        double w = chipWidth(label);
        double h = headerBottom() * 0.46;
        double x = rightEdge - w;
        double y = (headerBottom() - h) / 2;

        fillAlpha(colour, 0.11);
        roundPath(x, y, w, h, h / 2);
        g.fill();
        strokeAlpha(colour, 0.35);
        g.setLineWidth(1.5);
        roundPath(x, y, w, h, h / 2);
        g.stroke();

        // The dot is what makes a pill readable at a glance: the colour lands
        // before the word does.
        fill(colour);
        g.beginPath();
        g.arc(x + h * 0.44, y + h / 2, h * 0.15, 0, Math.PI * 2);
        g.fill();

        text(label, x + h * 0.72, y + h * 0.69, font, colour, "left", true);
        return x - headerBottom() * 0.1;
    }

    private static String turnNumber() {
        return state.hasTurnLimit()
                ? state.turnsTaken() + "/" + state.turnLimit()
                : String.valueOf(state.turnsTaken());
    }

    private static String turnLabel() {
        return state.hasTurnLimit() ? "TURNS LEFT " + state.turnsRemaining() : "TURNS";
    }

    private static double counterWidth() {
        g.setFont("bold " + (int) (headerBottom() * 0.44) + "px ui-monospace, monospace");
        double number = g.measureText(turnNumber()).getWidth();
        g.setFont((int) (headerBottom() * 0.22) + "px system-ui, sans-serif");
        return Math.max(number, g.measureText(turnLabel()).getWidth());
    }

    /** Turns taken over the limit, in the gap the chips left. */
    private static void drawTurnCounter(double rightEdge) {
        double block = counterWidth();
        double x = Math.max(rightEdge - block,
                identityRightEdge() + headerBottom() * 0.3) + block / 2;
        text(turnNumber(), x, headerBottom() * 0.52, headerBottom() * 0.44,
                clockColour(), "center", true);
        text(turnLabel(), x, headerBottom() * 0.84, headerBottom() * 0.22, DIM, "center", false);
    }

    /** Pale while there is room, amber when the clock tightens, red at the end. */
    private static int clockColour() {
        if (!state.hasTurnLimit()) {
            return TEXT;
        }
        double left = (double) state.turnsRemaining() / state.turnLimit();
        if (left <= 0.10) {
            return DANGER;
        }
        return left <= 0.25 ? KEY : TEXT;
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

    private static double dangerStrength(GuardMode mode) {
        return switch (mode) {
            case PATROL -> 46 / 255.0;
            case ALERT -> 66 / 255.0;
            case HUNT -> 92 / 255.0;
        };
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

    /**
     * The shared 16x16 art, one filled rectangle per pixel.
     *
     * <p>The desktop does exactly this with a {@code Graphics2D} and the phone
     * with a {@code Canvas}. All three read the same grids, so the key, the
     * exit, the guard robot and the six characters are the same drawings
     * everywhere rather than three sets that look roughly alike.
     */
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

    private static void pixels(String[] rows, java.util.Map<Character, Integer> palette,
                               double cx, double cy, double size) {
        double step = Math.max(1, Math.floor(size / rows.length));
        double left = cx - step * rows.length / 2;
        double top = cy - step * rows.length / 2;
        for (int row = 0; row < rows.length; row++) {
            for (int col = 0; col < rows[row].length(); col++) {
                Integer packed = palette.get(rows[row].charAt(col));
                if (packed == null) {
                    continue;
                }
                fill(packed);
                g.fillRect(left + col * step, top + row * step, step + 0.5, step + 0.5);
            }
        }
    }

    private static void tinted(String[] rows, int colour, double cx, double cy, double size) {
        pixels(rows, ObjectPixels.paletteOf(colour), cx, cy, size);
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
                if (inside(startButton(), x, y)) {
                    screen = Screen.NAME;
                    showNameField();
                } else if (inside(quitButton(), x, y)) {
                    screen = Screen.GOODBYE;
                }
            }
            case NAME -> {
                if (inside(backBox(), x, y)) {
                    hideNameField();
                    screen = Screen.TITLE;
                } else if (inside(titleButton(), x, y)) {
                    acceptName();
                    return;
                }
            }
            case GOODBYE -> {
                if (inside(titleButton(), x, y)) {
                    screen = Screen.TITLE;
                }
            }
            case CHARACTER -> {
                if (inside(backBox(), x, y)) {
                    screen = Screen.NAME;
                    showNameField();
                } else if (inside(titleButton(), x, y)) {
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
                if (inside(backBox(), x, y)) {
                    screen = Screen.CHARACTER;
                    draw();
                    return;
                }
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
