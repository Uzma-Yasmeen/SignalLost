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

/**
 * The strip above the map: who you are playing as, the level, the turn counter,
 * whether the key is held, how deep the undo stack is, and a lamp that lights up
 * when the developer reveal is on.
 */
public final class HudBar extends JPanel {

    private static final int HEIGHT = 62;

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
        g.drawString(critter.displayName(), 62, 28);

        g.setColor(Palette.TEXT_DIM);
        g.setFont(Fonts.body(12f));
        g.drawString(prettyLevelName() + "  ·  SIGHT " + state.fog().sightRadius(), 62, 46);
    }

    /** Turns a resource name like "level1" into "LEVEL 1" for display. */
    private String prettyLevelName() {
        return levelName.replaceAll("(?<=\\D)(?=\\d)", " ").toUpperCase();
    }

    private void drawRightHandStats(Graphics2D g) {
        int x = getWidth() - 18;

        if (state.fog().isRevealAll()) {
            x = drawChip(g, x, "DEV REVEAL", Palette.ACCENT, Glyph.DOT);
        }
        GuardMode alert = state.highestAlertLevel();
        if (alert != null && alert != GuardMode.PATROL) {
            x = drawChip(g, x, alert.label().toUpperCase(), alertColour(alert), Glyph.DOT);
        }
        if (state.canUndo()) {
            x = drawChip(g, x, "UNDO " + state.undoDepth(), Palette.ACCENT, Glyph.DOT);
        }
        x = drawChip(g, x,
                state.hasKey() ? "KEY HELD" : "KEY MISSING",
                state.hasKey() ? Palette.KEY : Palette.TEXT_DIM,
                state.hasKey() ? Glyph.DIAMOND : Glyph.DOT);
        drawTurnCounter(g, x);
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

    private void drawTurnCounter(Graphics2D g, int rightEdge) {
        String number = String.valueOf(state.turnsTaken());

        g.setFont(Fonts.mono(22f));
        FontMetrics numberMetrics = g.getFontMetrics();
        int numberWidth = numberMetrics.stringWidth(number);

        g.setFont(Fonts.body(11f));
        FontMetrics labelMetrics = g.getFontMetrics();
        int labelWidth = labelMetrics.stringWidth("TURNS");

        int blockWidth = Math.max(numberWidth, labelWidth);
        int x = rightEdge - blockWidth;

        g.setColor(Palette.TEXT_DIM);
        g.drawString("TURNS", x + (blockWidth - labelWidth) / 2, 46);

        g.setColor(Palette.TEXT);
        g.setFont(Fonts.mono(22f));
        g.drawString(number, x + (blockWidth - numberWidth) / 2, 32);
    }
}
