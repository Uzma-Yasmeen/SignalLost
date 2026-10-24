package com.signallost.ui;

import com.signallost.guard.Guard;
import com.signallost.guard.GuardMode;
import com.signallost.model.GameMap;
import com.signallost.model.GameState;
import com.signallost.model.Position;
import com.signallost.model.Score;
import com.signallost.model.TileType;
import com.signallost.vision.TileVisibility;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Draws the level: one rounded tile per grid square, shaded by how the player
 * currently perceives it, with the player's light falling off over distance.
 *
 * <p>This class only reads {@link GameState}; it never changes it. Input lives in
 * {@link GameFrame}, so the renderer stays a pure view of the model.
 */
public final class GamePanel extends JPanel {

    // 40 rather than 34 so a 16-pixel-wide sprite lands on a whole
    // number of screen pixels per art pixel and stays crisp.
    private static final int TILE = 40;
    private static final int GAP = 2;
    private static final int MARGIN = 14;

    /**
     * The narrowest the board is allowed to be.
     *
     * <p>A small level would otherwise make a window too narrow for the HUD and
     * the control strip, which overlap and clip rather than shrink. The board is
     * centred in whatever extra space this leaves, so a short level is
     * letterboxed instead of squashing the furniture around it.
     */
    private static final int MIN_BOARD_WIDTH = 900;

    /** How fast sprite frames advance, when there are any. */
    private static final int ANIMATION_INTERVAL_MS = 150;

    private final GameState state;
    private final Critter critter;
    private final javax.swing.Timer animator;

    /** How many steps of the route a hint gives away. */
    private static final int HINT_STEPS = 7;

    private int animationFrame;
    private boolean showHint;

    public GamePanel(GameState state, Critter critter) {
        this.state = state;
        this.critter = critter;
        GameMap map = state.map();
        setPreferredSize(new Dimension(
                Math.max(map.cols() * TILE + MARGIN * 2, MIN_BOARD_WIDTH),
                map.rows() * TILE + MARGIN * 2));
        setBackground(Palette.BACKDROP);

        // Only tick when there is artwork to animate: the drawn-in-code
        // characters are still images, and a turn-based game should not be
        // repainting itself for nothing.
        animator = new javax.swing.Timer(ANIMATION_INTERVAL_MS, event -> {
            animationFrame++;
            repaint();
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (CritterArt.anyArtworkPresent()) {
            animator.start();
        }
    }

    @Override
    public void removeNotify() {
        animator.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        GameMap map = state.map();
        for (int row = 0; row < map.rows(); row++) {
            for (int col = 0; col < map.cols(); col++) {
                drawTile(g, new Position(row, col));
            }
        }
        drawGuardVision(g);
        if (showHint) {
            drawHint(g);
        }
        drawPlayerLight(g);
        drawGuards(g);
        drawPlayer(g);

        if (state.wasSpotted() && !state.isAttemptOver() && !state.isFinished()) {
            drawAlarmVignette(g);
        }
        drawOverlays(g);

        g.dispose();
    }

    // ---------------------------------------------------------------- tiles

    private void drawTile(Graphics2D g, Position position) {
        TileVisibility visibility = state.fog().visibilityOf(position);
        TileType type = state.map().tileAt(position);
        Rectangle2D bounds = tileBounds(position);

        if (visibility == TileVisibility.UNEXPLORED) {
            g.setColor(Palette.UNEXPLORED);
            g.fill(new RoundRectangle2D.Double(
                    bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 5, 5));
            return;
        }

        boolean lit = visibility == TileVisibility.VISIBLE;
        double brightness = lit ? lightFalloff(position) : 1.0;

        if (type == TileType.WALL) {
            drawWall(g, bounds, lit, brightness);
        } else {
            drawFloor(g, bounds, lit, brightness);
            drawObjective(g, bounds, type, lit, brightness);
        }
    }

    private void drawFloor(Graphics2D g, Rectangle2D bounds, boolean lit, double brightness) {
        Color base = lit
                ? Palette.shade(Palette.FLOOR_LIT, brightness)
                : Palette.FLOOR_REMEMBERED;
        g.setColor(base);
        g.fill(new RoundRectangle2D.Double(
                bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 5, 5));

        // A faint dot in the middle gives the floor some texture at this size.
        g.setColor(Palette.withAlpha(Color.WHITE, lit ? (int) (26 * brightness) : 8));
        double dot = 3;
        g.fill(new Ellipse2D.Double(
                bounds.getCenterX() - dot / 2, bounds.getCenterY() - dot / 2, dot, dot));
    }

    private void drawWall(Graphics2D g, Rectangle2D bounds, boolean lit, double brightness) {
        Color body = lit
                ? Palette.shade(Palette.WALL_LIT, brightness)
                : Palette.WALL_REMEMBERED;
        g.setColor(body);
        g.fill(new RoundRectangle2D.Double(
                bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 4, 4));

        // A lighter strip along the top edge reads as a lit face and gives the
        // walls a sense of height without any sprite work.
        Color top = lit
                ? Palette.shade(Palette.WALL_LIT_TOP, brightness)
                : Palette.shade(Palette.WALL_REMEMBERED, 1.28);
        g.setColor(top);
        g.fill(new RoundRectangle2D.Double(
                bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight() * 0.28, 4, 4));
    }

    private void drawObjective(Graphics2D g, Rectangle2D bounds, TileType type,
                               boolean lit, double brightness) {
        switch (type) {
            case KEY -> drawKey(g, bounds, lit, brightness);
            case EXIT -> drawExit(g, bounds, lit, brightness);
            default -> {
                // FLOOR and START need no marker.
            }
        }
    }

    private void drawKey(Graphics2D g, Rectangle2D bounds, boolean lit, double brightness) {
        if (lit) {
            paintGlow(g, bounds.getCenterX(), bounds.getCenterY(), TILE * 0.95,
                    Palette.KEY, (int) (110 * brightness));
        }
        ObjectSprite.paintKey(g, bounds.getCenterX(), bounds.getCenterY(),
                TILE * 0.8, lit ? brightness : 0.45);
    }

    private void drawExit(Graphics2D g, Rectangle2D bounds, boolean lit, double brightness) {
        boolean open = state.hasAllKeys();
        if (lit && open) {
            paintGlow(g, bounds.getCenterX(), bounds.getCenterY(), TILE * 1.1,
                    Palette.EXIT_OPEN, (int) (120 * brightness));
        }
        ObjectSprite.paintExit(g, bounds.getCenterX(), bounds.getCenterY(),
                TILE * 0.85, open, lit ? brightness : 0.45);
    }

    // ------------------------------------------------------------------ hint

    public void toggleHint() {
        showHint = !showHint;
        repaint();
    }

    public boolean isHintShowing() {
        return showHint;
    }

    /**
     * A trail of dots along the suggested route, fading out as it goes.
     *
     * <p>Fading matters: a trail of equal dots looks like an instruction, while
     * one that dissolves looks like a suggestion that runs out, which is what a
     * hint that only shows the next few steps actually is.
     */
    private void drawHint(Graphics2D g) {
        List<Position> route = state.hintRoute(HINT_STEPS);
        for (int i = 0; i < route.size(); i++) {
            Rectangle2D bounds = tileBounds(route.get(i));
            double fade = 1.0 - (double) i / route.size();
            double dot = TILE * (0.13 * fade + 0.05);

            g.setColor(Palette.withAlpha(Palette.HINT, (int) (210 * fade)));
            g.fill(new Ellipse2D.Double(
                    bounds.getCenterX() - dot, bounds.getCenterY() - dot, dot * 2, dot * 2));
        }
    }

    // ---------------------------------------------------------------- guards

    /**
     * Washes red over the tiles a guard is watching.
     *
     * <p>Only guards the player can actually see are drawn, and only the watched
     * tiles the player can also see. The player never learns about danger they
     * have no way of perceiving, which keeps the fog honest: what is on screen is
     * exactly what the character knows.
     */
    private void drawGuardVision(Graphics2D g) {
        for (Guard guard : state.guards()) {
            if (!state.fog().isVisible(guard.position())) {
                continue;
            }
            int strength = dangerStrength(guard.mode());
            for (Position watched : guard.watchedTiles(state.map())) {
                // Walls in the sightline are reddened too. Skipping them left
                // holes in the danger zone, so the watched area read as a
                // scatter of tiles rather than one region to stay out of.
                if (!state.fog().isVisible(watched)) {
                    continue;
                }
                Rectangle2D bounds = tileBounds(watched);
                g.setColor(Palette.withAlpha(Palette.DANGER, strength));
                g.fill(new RoundRectangle2D.Double(
                        bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 5, 5));
            }
        }
        if (state.wasSpotted()) {
            drawSpottedTile(g);
        }
    }

    /**
     * Watched tiles are red whoever is watching, because red has to mean one
     * thing: do not end your turn here. How much trouble the guard itself is in
     * is said by its own colour instead.
     */
    private static int dangerStrength(GuardMode mode) {
        return switch (mode) {
            case PATROL -> 46;
            case ALERT -> 66;
            case HUNT -> 92;
        };
    }

    /** A hard red ring on the player's own tile the turn they are seen. */
    private void drawSpottedTile(Graphics2D g) {
        Rectangle2D bounds = tileBounds(state.player());
        g.setColor(Palette.withAlpha(Palette.DANGER, 120));
        g.fill(new RoundRectangle2D.Double(
                bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 5, 5));
        g.setColor(Palette.DANGER);
        g.setStroke(new BasicStroke(2.4f));
        g.draw(new RoundRectangle2D.Double(
                bounds.getX() + 1, bounds.getY() + 1,
                bounds.getWidth() - 2, bounds.getHeight() - 2, 5, 5));
    }

    private void drawGuards(Graphics2D g) {
        for (Guard guard : state.guards()) {
            if (state.fog().isVisible(guard.position())) {
                drawGuard(g, guard);
            }
        }
    }

    /** The guard sprite, with a glow that brightens as it gets more interested. */
    private void drawGuard(Graphics2D g, Guard guard) {
        Rectangle2D bounds = tileBounds(guard.position());
        Color colour = modeColour(guard.mode());

        paintGlow(g, bounds.getCenterX(), bounds.getCenterY(), TILE * 1.3, colour,
                guard.mode() == GuardMode.PATROL ? 70 : 140);
        GuardSprite.paint(g, guard.mode(),
                bounds.getCenterX(), bounds.getCenterY(), TILE * 0.85);
    }

    private static Color modeColour(GuardMode mode) {
        return GuardSprite.colourOf(mode);
    }

    // --------------------------------------------------------------- player

    /** A soft radial glow centred on the player, suggesting a carried light. */
    private void drawPlayerLight(Graphics2D g) {
        if (state.fog().isRevealAll()) {
            return;
        }
        Rectangle2D bounds = tileBounds(state.player());
        paintGlow(g, bounds.getCenterX(), bounds.getCenterY(),
                TILE * (state.fog().sightRadius() + 1.2), critter.glow(), 42);
    }

    /**
     * The chosen animal, drawn a little above the centre of its tile so its ears
     * are not clipped by the tile above.
     */
    private void drawPlayer(Graphics2D g) {
        Rectangle2D bounds = tileBounds(state.player());
        double cx = bounds.getCenterX();
        double cy = bounds.getCenterY() + TILE * 0.07;

        paintGlow(g, cx, cy, TILE * 1.6, critter.glow(), 130);

        CritterArt.paint(g, critter, cx, cy, TILE * 0.3, animationFrame);
    }

    // ---------------------------------------------------------------- extras

    /** Whatever the player most needs told: run over, escaped, or attempt lost. */
    private void drawOverlays(Graphics2D g) {
        if (state.isRunOver()) {
            drawEndOverlay(g, "GAME OVER", Palette.DANGER,
                    "out of lives  ·  " + sightingsLine() + "  ·  R to start again");
        } else if (state.hasEscaped()) {
            drawEscapeOverlay(g);
        } else if (state.isAttemptOver()) {
            int attemptsLeft = state.livesLeft() - 1;
            String detail = attemptsLeft > 0
                    ? attemptsLeft + (attemptsLeft == 1 ? " life left" : " lives left")
                            + "  ·  SPACE to try again"
                    : "that was the last life  ·  SPACE to see the damage";
            drawEndOverlay(g, state.isCaught() ? "CAUGHT" : "OUT OF TIME",
                    Palette.DANGER, detail);
        }
    }

    /**
     * A red glow around the edge of the screen, and a line saying what happened.
     *
     * <p>A chip in the corner was not enough. Being seen is the single most
     * important thing that can happen on a turn, and the player is looking at
     * their character in the middle of the map, not at the HUD.
     */
    private void drawAlarmVignette(Graphics2D g) {
        int depth = 26;
        for (int i = 0; i < depth; i++) {
            int alpha = (int) (95 * Math.pow(1.0 - (double) i / depth, 2));
            if (alpha <= 0) {
                continue;
            }
            g.setColor(Palette.withAlpha(Palette.DANGER, alpha));
            g.setStroke(new BasicStroke(1f));
            g.drawRect(i, i, getWidth() - 1 - i * 2, getHeight() - 1 - i * 2);
        }

        String message = "SPOTTED  ·  the alarm is up  ·  press U to take it back";
        g.setFont(Fonts.bodyBold(13f));
        FontMetrics metrics = g.getFontMetrics();
        int width = metrics.stringWidth(message) + 28;
        int x = (getWidth() - width) / 2;

        g.setColor(Palette.withAlpha(Palette.DANGER, 220));
        g.fill(new RoundRectangle2D.Double(x, 8, width, 26, 26, 26));
        g.setColor(Color.WHITE);
        g.drawString(message, x + 14, 8 + 18);
    }

    /**
     * The escape screen: the headline, the rating, and what earned it.
     *
     * <p>Naming the two bars rather than only showing a number is the point.
     * A player who got two stars should be able to read why, because that is
     * what sends them back in to get the third.
     */
    private void drawEscapeOverlay(Graphics2D g) {
        Score score = state.score();

        g.setColor(Palette.withAlpha(Palette.BACKDROP, 212));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Palette.EXIT_OPEN);
        g.setFont(Fonts.display(38f));
        centreText(g, "ESCAPED", getHeight() / 2 - 46);

        double starSize = 54;
        double gap = 14;
        double totalWidth = Score.MAX_STARS * starSize + (Score.MAX_STARS - 1) * gap;
        double x = (getWidth() - totalWidth) / 2 + starSize / 2;
        for (int i = 0; i < Score.MAX_STARS; i++) {
            ObjectSprite.paintStar(g, x + i * (starSize + gap), getHeight() / 2.0 + 6,
                    starSize, i < score.stars());
        }

        g.setColor(Palette.TEXT_DIM);
        g.setFont(Fonts.body(15f));
        centreText(g, score.summary(), getHeight() / 2 + 60);
        centreText(g, "R to replay  ·  C to change character", getHeight() / 2 + 84);
    }

    private void drawEndOverlay(Graphics2D g, String headline, Color colour, String detail) {
        g.setColor(Palette.withAlpha(Palette.BACKDROP, 212));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(colour);
        g.setFont(Fonts.display(38f));
        centreText(g, headline, getHeight() / 2 - 10);

        g.setColor(Palette.TEXT_DIM);
        g.setFont(Fonts.body(15f));
        centreText(g, detail, getHeight() / 2 + 24);
    }

    private String sightingsLine() {
        int seen = state.sightings();
        if (seen == 0) {
            return "never seen";
        }
        return seen == 1 ? "seen once" : "seen " + seen + " times";
    }

    private void centreText(Graphics2D g, String text, int baselineY) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, baselineY);
    }

    /**
     * Light fades with the BFS step count, so the tile under the player is
     * brightest and the edge of sight is dim. Walls and floors share the curve.
     */
    private double lightFalloff(Position position) {
        // With the developer reveal on, tiles outside real sight have no step
        // count. Dimming only the ones that do would make the lit area the
        // darkest part of the screen, so the whole map is drawn at full light.
        if (state.fog().isRevealAll()) {
            return 1.0;
        }
        int distance = state.fog().sightDistance(position);
        if (distance < 0 || state.fog().sightRadius() == 0) {
            return 1.0;
        }
        double ratio = (double) distance / state.fog().sightRadius();
        return 1.0 - 0.5 * ratio;
    }

    private void paintGlow(Graphics2D g, double cx, double cy, double radius,
                           Color colour, int alpha) {
        if (radius <= 0 || alpha <= 0) {
            return;
        }
        RadialGradientPaint paint = new RadialGradientPaint(
                new Point2D.Double(cx, cy), (float) radius,
                new float[] {0f, 1f},
                new Color[] {Palette.withAlpha(colour, alpha), Palette.withAlpha(colour, 0)});
        g.setPaint(paint);
        g.fill(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
        g.setPaint(null);
    }

    private Rectangle2D tileBounds(Position position) {
        return new Rectangle2D.Double(
                boardLeft() + position.col() * TILE + GAP / 2.0,
                MARGIN + position.row() * TILE + GAP / 2.0,
                TILE - GAP,
                TILE - GAP);
    }

    /** Where the grid starts, centred when the panel is wider than the level. */
    private double boardLeft() {
        double gridWidth = state.map().cols() * TILE;
        return Math.max(MARGIN, (getWidth() - gridWidth) / 2.0);
    }
}
