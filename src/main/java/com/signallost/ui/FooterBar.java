package com.signallost.ui;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** The strip below the map listing the controls as small keycaps. */
public final class FooterBar extends JPanel {

    private static final int HEIGHT = 46;

    /** Key label paired with what it does. */
    private static final String[][] CONTROLS = {
            {"← ↑ → ↓", "move"},
            {"SPACE", "wait"},
            {"U", "undo"},
            {"H", "hint"},
            {"M", "reveal map"},
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

        g.setColor(Palette.HUD_EDGE);
        g.fillRect(0, 0, getWidth(), 1);

        int x = 18;
        for (String[] control : CONTROLS) {
            x = drawControl(g, x, control[0], control[1]);
        }
        g.dispose();
    }

    /** Draws one keycap plus its caption, returning the x to continue from. */
    private int drawControl(Graphics2D g, int x, String key, String caption) {
        g.setFont(Fonts.mono(11f));
        FontMetrics keyMetrics = g.getFontMetrics();
        int keyWidth = keyMetrics.stringWidth(key) + 18;
        int keyHeight = 22;
        int keyY = (HEIGHT - keyHeight) / 2;

        g.setColor(Palette.withAlpha(Palette.TEXT, 16));
        g.fill(new RoundRectangle2D.Double(x, keyY, keyWidth, keyHeight, 7, 7));
        g.setColor(Palette.withAlpha(Palette.TEXT, 45));
        g.draw(new RoundRectangle2D.Double(x, keyY, keyWidth, keyHeight, 7, 7));

        g.setColor(Palette.TEXT);
        g.drawString(key, x + 9, keyY + keyHeight / 2 + keyMetrics.getAscent() / 2 - 1);

        int afterKey = x + keyWidth + 8;
        g.setFont(Fonts.body(12f));
        FontMetrics captionMetrics = g.getFontMetrics();
        g.setColor(Palette.TEXT_DIM);
        g.drawString(caption, afterKey, HEIGHT / 2 + captionMetrics.getAscent() / 2 - 2);

        return afterKey + captionMetrics.stringWidth(caption) + 22;
    }
}
