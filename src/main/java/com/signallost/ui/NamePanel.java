package com.signallost.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

/**
 * Asks who is playing, once, so scores have somebody to belong to.
 *
 * <p>It draws its own text field rather than using a Swing one, for the same
 * reason everything else here is hand-drawn: a default text box in the middle
 * of a pixel-art game looks like a dialog that escaped from another program.
 */
public final class NamePanel extends JPanel {

    public static final int MAX_LENGTH = 14;

    private static final Color BACKDROP_TOP = new Color(0x17203C);
    private static final Color BACKDROP_BOTTOM = new Color(0x0B1020);

    private final Consumer<String> onConfirmed;
    private final StringBuilder typed = new StringBuilder();

    public NamePanel(String existingName, Consumer<String> onConfirmed) {
        this.onConfirmed = onConfirmed;
        if (existingName != null) {
            typed.append(existingName);
        }
        setPreferredSize(new Dimension(620, 340));
        setBackground(BACKDROP_BOTTOM);
    }

    // ---------------------------------------------------------------- typing

    /** Accepts one typed character, ignoring anything that is not a name. */
    public void type(char character) {
        if (typed.length() >= MAX_LENGTH) {
            return;
        }
        if (Character.isLetterOrDigit(character) || character == ' ' || character == '-') {
            typed.append(character);
            repaint();
        }
    }

    public void backspace() {
        if (typed.length() > 0) {
            typed.deleteCharAt(typed.length() - 1);
            repaint();
        }
    }

    public String name() {
        return typed.toString().trim();
    }

    public boolean isUsable() {
        return !name().isEmpty();
    }

    public void confirm() {
        if (isUsable()) {
            onConfirmed.accept(name());
        }
    }

    // -------------------------------------------------------------- painting

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

        g.setColor(Palette.ACCENT);
        g.setFont(Fonts.bodyBold(12f));
        centre(g, "SIGNAL LOST", 64);

        g.setColor(Palette.TEXT);
        g.setFont(Fonts.display(30f));
        centre(g, "Who is escaping?", 112);

        drawField(g);

        g.setColor(Palette.TEXT_DIM);
        g.setFont(Fonts.body(13f));
        centre(g, isUsable() ? "Enter to begin" : "type a name, then Enter",
                getHeight() - 48);

        g.dispose();
    }

    private void drawField(Graphics2D g) {
        int width = 360;
        int height = 54;
        int x = (getWidth() - width) / 2;
        int y = 150;

        g.setColor(Palette.withAlpha(Palette.TEXT, 14));
        g.fill(new RoundRectangle2D.Double(x, y, width, height, 12, 12));
        g.setColor(isUsable() ? Palette.ACCENT : Palette.withAlpha(Palette.TEXT, 50));
        g.draw(new RoundRectangle2D.Double(x, y, width, height, 12, 12));

        String shown = typed.toString();
        g.setFont(Fonts.mono(22f));
        FontMetrics metrics = g.getFontMetrics();
        int textX = x + 18;
        int baseline = y + height / 2 + metrics.getAscent() / 2 - 2;

        g.setColor(Palette.TEXT);
        g.drawString(shown, textX, baseline);

        // A block caret, which reads as "typing here" without needing a blink.
        g.setColor(Palette.withAlpha(Palette.ACCENT, 170));
        g.fillRect(textX + metrics.stringWidth(shown) + 2, y + 14, 10, height - 28);
    }

    private void centre(Graphics2D g, String text, int baselineY) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, baselineY);
    }
}
