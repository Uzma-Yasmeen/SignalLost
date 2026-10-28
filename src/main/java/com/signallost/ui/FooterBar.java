package com.signallost.ui;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * The control strip: what every key does, said plainly.
 *
 * <p>This used to be dim grey captions on a dark bar, and the question it drew
 * was "how is a player supposed to know that H gives a hint?" They were not —
 * the bar was legible only if you already knew to look at it.
 *
 * <p>So the keycaps are now bright enough to read at a glance, the captions are
 * full-strength text rather than the muted shade used for background detail,
 * and the whole strip is separated from the board by a visible accent line
 * instead of a hairline. A control the player cannot find is a control that
 * does not exist.
 */
public final class FooterBar extends JPanel {

    private static final int HEIGHT = 50;

    /** Key label paired with what it does. */
    private static final String[][] CONTROLS = {
            {"← ↑ → ↓", "move"},
            {"SPACE", "wait"},
            {"U", "undo"},
            {"H", "hint (3 a level)"},
            {"M", "map"},
            {"R", "restart"},
            {"C", "character"},
            {"ESC", "levels"},
    };

    public FooterBar() {
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

        // A line the eye actually lands on, rather than a hairline.
        g.setColor(Palette.withAlpha(Palette.ACCENT, 90));
        g.fillRect(0, 0, getWidth(), 2);

        int x = 18;
        for (String[] control : CONTROLS) {
            x = drawControl(g, x, control[0], control[1]);
        }
        g.dispose();
    }

    /** Draws one keycap plus its caption, returning the x to continue from. */
    private int drawControl(Graphics2D g, int x, String key, String caption) {
        g.setFont(Fonts.mono(12f));
        FontMetrics keyMetrics = g.getFontMetrics();
        int keyWidth = keyMetrics.stringWidth(key) + 18;
        int keyHeight = 24;
        int keyY = (HEIGHT - keyHeight) / 2 + 1;

        // A keycap that reads as a key: light face, defined edge, bright glyph.
        g.setColor(Palette.withAlpha(Palette.TEXT, 34));
        g.fill(new RoundRectangle2D.Double(x, keyY, keyWidth, keyHeight, 7, 7));
        g.setColor(Palette.withAlpha(Palette.TEXT, 110));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new RoundRectangle2D.Double(x, keyY, keyWidth, keyHeight, 7, 7));

        g.setColor(Color.WHITE);
        g.drawString(key, x + 9, keyY + keyHeight / 2 + keyMetrics.getAscent() / 2 - 1);

        int afterKey = x + keyWidth + 7;
        g.setFont(Fonts.body(13f));
        FontMetrics captionMetrics = g.getFontMetrics();
        g.setColor(Palette.TEXT);
        g.drawString(caption, afterKey, HEIGHT / 2 + captionMetrics.getAscent() / 2 - 1);

        return afterKey + captionMetrics.stringWidth(caption) + 18;
    }
}
