package com.signallost.ui;

import com.signallost.guard.GuardMode;
import com.signallost.model.GameState;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The strip above the map: who you are playing as, the level, the turn counter,
 * whether the key is held, how deep the undo stack is, and a lamp that lights up
 * when the developer reveal is on.
 */
public final class HudBar extends JPanel {

    private static final int HEIGHT = 54;

    /** Clear space kept between the player's name and the turn counter. */
    private static final int GAP_BESIDE_NAME = 16;

    /** Marks drawn on a chip's dot, so chips read at a glance. */
    private enum Glyph { DOT, DIAMOND }

    private final GameState state;
    private final Critter critter;
    private final String levelName;

    public HudBar(GameState state, Critter critter, String levelName) {
        this.state = state;
        this.critter = critter;
        this.levelName = levelName;
        setPreferredSize(new Dimension(0, HEIGHT));
        setBackground(Palette.HUD_BACKGROUND);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // A hairline under the bar separates it from the map.
        g.setColor(Palette.HUD_EDGE);
        g.fillRect(0, HEIGHT - 1, getWidth(), 1);

        drawIdentity(g);
        drawRightHandStats(g);

        g.dispose();
    }

    /** The chosen animal and the level, at the left-hand end. */
    private void drawIdentity(Graphics2D g) {
        CritterArt.paint(g, critter, 34, HEIGHT / 2.0 + 2, 15);

        g.setColor(Palette.TEXT);
        g.setFont(Fonts.display(17f));
        g.drawString(critter.displayName(), 62, 25);

        g.setColor(Palette.TEXT_DIM);
        g.setFont(Fonts.body(12f));
        g.drawString(levelAndSight(), 62, 43);
    }

    private String levelAndSight() {
        return prettyLevelName() + "  ·  SIGHT " + state.fog().sightRadius();
    }

    /** Turns a resource name like "level1" into "LEVEL 1" for display. */
    private String prettyLevelName() {
        return levelName.replaceAll("(?<=\\D)(?=\\d)", " ").toUpperCase();
    }

    /**
     * A chip waiting to be drawn, and how readily it gives up its place.
     *
     * <p>{@code rank} is the order they survive in when the bar is too narrow
     * for all of them: lowest goes first. The developer lamp is the first thing
     * nobody needs; the key count is the last.
     */
    private record Chip(String label, Color colour, Glyph glyph, int rank) {
    }

    /**
     * Chips are laid out right to left and the turn counter takes whatever is
     * left over, which used to mean it had no lower bound at all — adding one
     * more chip slid the counter straight through the player's name. So the row
     * is measured against the space actually available first, and the least
     * important chips drop off until it fits.
     */
    private void drawRightHandStats(Graphics2D g) {
        List<Chip> chips = new ArrayList<>();
        if (state.fog().isRevealAll()) {
            chips.add(new Chip("DEV REVEAL", Palette.ACCENT, Glyph.DOT, 1));
        }
        if (state.wasSpotted()) {
            chips.add(new Chip("SPOTTED", Palette.DANGER, Glyph.DOT, 5));
        }
        GuardMode alert = state.highestAlertLevel();
        if (alert != null && alert != GuardMode.PATROL) {
            chips.add(new Chip(alert.label().toUpperCase(), alertColour(alert), Glyph.DOT, 4));
        }
        if (state.sightings() > 0) {
            // Sightings cost stars, so they outrank the undo depth, which is trivia.
            chips.add(new Chip("SEEN " + state.sightings(), Palette.GUARD_ALERT, Glyph.DOT, 3));
        }
        if (state.canUndo()) {
            chips.add(new Chip("UNDO " + state.undoDepth(), Palette.ACCENT, Glyph.DOT, 2));
        }
        chips.add(new Chip(livesLabel(), livesColour(), Glyph.DOT, 7));
        chips.add(new Chip(hintsLabel(), hintsColour(), Glyph.DOT, 6));
        chips.add(new Chip("KEYS " + state.keysHeld() + "/" + state.keysRequired(),
                state.hasAllKeys() ? Palette.EXIT_OPEN : Palette.KEY, Glyph.DIAMOND, 8));

        int room = getWidth() - 18 - identityRightEdge(g) - GAP_BESIDE_NAME - turnCounterWidth(g);
        while (chips.size() > 1 && rowWidth(g, chips) > room) {
            chips.remove(chips.stream()
                    .min(Comparator.comparingInt(Chip::rank))
                    .orElseThrow());
        }

        int x = getWidth() - 18;
        for (Chip chip : chips) {
            x = drawChip(g, x, chip.label(), chip.colour(), chip.glyph());
        }
        drawTurnCounter(g, x);
    }

    private int rowWidth(Graphics2D g, List<Chip> chips) {
        int total = 0;
        for (Chip chip : chips) {
            total += chipWidth(g, chip.label()) + 10;
        }
        return total;
    }

    private int chipWidth(Graphics2D g, String label) {
        g.setFont(Fonts.bodyBold(11f));
        return g.getFontMetrics().stringWidth(label) + 16 + 22;
    }

    /** Where the name and level line stop, so nothing is drawn on top of them. */
    private int identityRightEdge(Graphics2D g) {
        g.setFont(Fonts.display(17f));
        int name = g.getFontMetrics().stringWidth(critter.displayName());
        g.setFont(Fonts.body(12f));
        int beneath = g.getFontMetrics().stringWidth(levelAndSight());
        return 62 + Math.max(name, beneath);
    }

    private String livesLabel() {
        int lives = state.livesLeft();
        return lives == 1 ? "LAST LIFE" : "LIVES " + lives;
    }

    private String hintsLabel() {
        int hints = state.hintsLeft();
        return hints == 0 ? "NO HINTS" : "HINTS " + hints;
    }

    /** Dim while there are plenty, amber on the last one, red once spent. */
    private Color hintsColour() {
        int hints = state.hintsLeft();
        if (hints == 0) {
            return Palette.DANGER;
        }
        return hints == 1 ? Palette.GUARD_ALERT : Palette.TEXT_DIM;
    }

    /** The counter turns amber then red as the attempts run out. */
    private Color livesColour() {
        int lives = state.livesLeft();
        if (lives <= 1) {
            return Palette.DANGER;
        }
        return lives == 2 ? Palette.GUARD_ALERT : Palette.TEXT_DIM;
    }

    private static Color alertColour(GuardMode mode) {
        return mode == GuardMode.HUNT ? Palette.GUARD_HUNT : Palette.GUARD_ALERT;
    }

    /** Draws a pill right-aligned at {@code rightEdge}, returning the new right edge. */
    private int drawChip(Graphics2D g, int rightEdge, String label, Color colour, Glyph glyph) {
        g.setFont(Fonts.bodyBold(11f));
        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(label);
        int glyphSpace = 16;
        int width = textWidth + glyphSpace + 22;
        int height = 24;
        int x = rightEdge - width;
        int y = (HEIGHT - height) / 2 - 1;

        g.setColor(Palette.withAlpha(colour, 28));
        g.fill(new RoundRectangle2D.Double(x, y, width, height, height, height));
        g.setColor(Palette.withAlpha(colour, 90));
        g.draw(new RoundRectangle2D.Double(x, y, width, height, height, height));

        g.setColor(colour);
        double markCentreX = x + 14.5;
        double markCentreY = y + height / 2.0;
        if (glyph == Glyph.DIAMOND) {
            drawDiamond(g, markCentreX, markCentreY, 6);
        } else {
            g.fill(new Ellipse2D.Double(markCentreX - 3.5, markCentreY - 3.5, 7, 7));
        }

        g.drawString(label, x + 11 + glyphSpace, y + height / 2 + metrics.getAscent() / 2 - 1);
        return x - 10;
    }

    /** A small diamond matching the key marker on the map. */
    private void drawDiamond(Graphics2D g, double cx, double cy, double size) {
        Path2D diamond = new Path2D.Double();
        diamond.moveTo(cx, cy - size);
        diamond.lineTo(cx + size, cy);
        diamond.lineTo(cx, cy + size);
        diamond.lineTo(cx - size, cy);
        diamond.closePath();
        g.fill(diamond);
    }

    /**
     * Turns taken, over the limit when the level has one.
     *
     * <p>The number changes colour as the clock runs down, so the pressure is
     * felt on the glance rather than only on the arithmetic.
     */
    private String turnNumber() {
        return state.hasTurnLimit()
                ? state.turnsTaken() + "/" + state.turnLimit()
                : String.valueOf(state.turnsTaken());
    }

    private String turnLabel() {
        return state.hasTurnLimit() ? "TURNS LEFT " + state.turnsRemaining() : "TURNS";
    }

    private int turnCounterWidth(Graphics2D g) {
        g.setFont(Fonts.mono(22f));
        int number = g.getFontMetrics().stringWidth(turnNumber());
        g.setFont(Fonts.body(11f));
        return Math.max(number, g.getFontMetrics().stringWidth(turnLabel()));
    }

    private void drawTurnCounter(Graphics2D g, int rightEdge) {
        String number = turnNumber();
        String label = turnLabel();

        g.setFont(Fonts.mono(22f));
        FontMetrics numberMetrics = g.getFontMetrics();
        int numberWidth = numberMetrics.stringWidth(number);

        g.setFont(Fonts.body(11f));
        FontMetrics labelMetrics = g.getFontMetrics();
        int labelWidth = labelMetrics.stringWidth(label);

        int blockWidth = Math.max(numberWidth, labelWidth);

        // Never let the counter reach the name, however many chips are up.
        int x = Math.max(rightEdge - blockWidth, identityRightEdge(g) + GAP_BESIDE_NAME);

        g.setColor(Palette.TEXT_DIM);
        g.drawString(label, x + (blockWidth - labelWidth) / 2, 43);

        g.setColor(clockColour());
        g.setFont(Fonts.mono(22f));
        g.drawString(number, x + (blockWidth - numberWidth) / 2, 29);
    }

    /** Pale while there is room, amber when the clock tightens, red at the end. */
    private Color clockColour() {
        if (!state.hasTurnLimit()) {
            return Palette.TEXT;
        }
        double left = (double) state.turnsRemaining() / state.turnLimit();
        if (left <= 0.10) {
            return Palette.DANGER;
        }
        return left <= 0.25 ? Palette.GUARD_ALERT : Palette.TEXT;
    }
}
