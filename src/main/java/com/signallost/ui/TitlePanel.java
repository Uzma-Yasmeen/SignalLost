package com.signallost.ui;

import com.signallost.model.Score;
import com.signallost.progress.PlayerProfile;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

/**
 * The screen the game opens on.
 *
 * <p>It exists to give the game a front door. Dropping a player straight into a
 * name box tells them nothing about what they are about to play; a title, a line
 * saying what the game is, and the cast lined up underneath does.
 *
 * <p>When there is a saved profile it says so, so a returning player can see
 * their name and their stars before deciding to carry on.
 */
public final class TitlePanel extends JPanel {

    private static final Color BACKDROP_TOP = new Color(0x17203C);
    private static final Color BACKDROP_BOTTOM = new Color(0x080B12);

    private final PlayerProfile profile;
    private final int levelCount;

    public TitlePanel(PlayerProfile profile, int levelCount) {
        this.profile = profile;
        this.levelCount = levelCount;
        setPreferredSize(new Dimension(680, 440));
        setBackground(BACKDROP_BOTTOM);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setPaint(new GradientPaint(0, 0, BACKDROP_TOP, 0, getHeight(), BACKDROP_BOTTOM));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setPaint(null);

        // A faint pool of light behind the title, like the one the player carries.
        glow(g, getWidth() / 2.0, 120, 260, Palette.PLAYER, 26);

        drawTitle(g);
        drawCast(g);
        drawFooter(g);

        g.dispose();
    }

    private void drawTitle(Graphics2D g) {
        g.setColor(Palette.TEXT);
        g.setFont(Fonts.display(52f));
        centre(g, "SIGNAL LOST", 126);

        g.setColor(Palette.ACCENT);
        g.setFont(Fonts.body(15f));
        centre(g, "Find the keys. Reach the exit. Do not be seen.", 158);

        g.setColor(Palette.TEXT_DIM);
        g.setFont(Fonts.body(13f));
        centre(g, levelCount + " levels  ·  three stars each", 184);
    }

    /** The six characters lined up, as a reason to look at the screen. */
    private void drawCast(Graphics2D g) {
        Critter[] cast = Critter.values();
        double size = 46;
        double gap = 18;
        double total = cast.length * size + (cast.length - 1) * gap;
        double x = (getWidth() - total) / 2 + size / 2;

        for (int i = 0; i < cast.length; i++) {
            CritterArt.paint(g, cast[i], x + i * (size + gap), 258, size / 2);
        }
    }

    private void drawFooter(Graphics2D g) {
        if (profile != null) {
            g.setColor(Palette.TEXT);
            g.setFont(Fonts.bodyBold(15f));
            centre(g, "Welcome back, " + profile.name(), 340);

            g.setColor(Palette.KEY);
            g.setFont(Fonts.body(13f));
            centre(g, profile.totalStars() + " of " + (levelCount * Score.MAX_STARS)
                    + " stars collected", 364);
        }

        g.setColor(Palette.TEXT_DIM);
        g.setFont(Fonts.body(14f));
        centre(g, "Press Enter to begin", getHeight() - 44);
    }

    private void glow(Graphics2D g, double cx, double cy, double radius,
                      Color colour, int alpha) {
        RadialGradientPaint paint = new RadialGradientPaint(
                new Point2D.Double(cx, cy), (float) radius,
                new float[] {0f, 1f},
                new Color[] {Palette.withAlpha(colour, alpha), Palette.withAlpha(colour, 0)});
        g.setPaint(paint);
        g.fill(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
        g.setPaint(null);
    }

    private void centre(Graphics2D g, String text, int baselineY) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, baselineY);
    }
}
