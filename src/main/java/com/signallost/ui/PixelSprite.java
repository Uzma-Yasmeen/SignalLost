package com.signallost.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Map;

/**
 * Draws sprites written as grids of characters.
 *
 * <p>A sprite is an array of equal-length strings, one per row, where each
 * character names a colour in a palette and a dot means transparent. Writing art
 * this way keeps it in the source, readable in a diff and editable without a
 * drawing program, and it means a new character is a block of text rather than a
 * binary file nobody can review.
 *
 * <p><b>Pixels stay whole.</b> The size of one art pixel is rounded to a whole
 * number of screen pixels and the sprite is then centred, instead of stretching
 * the art to fill the space exactly. Stretching by a fractional amount makes
 * some rows one pixel taller than others, which is the thing that makes pixel
 * art look wrong, so a little empty space around the sprite is the better trade.
 */
public final class PixelSprite {

    private PixelSprite() {
    }

    /** Transparent, in every sprite. */
    public static final char EMPTY = '.';

    /**
     * Paints a sprite centred on {@code (cx, cy)}, fitting inside a box of
     * {@code boxSize} screen pixels.
     *
     * @param rows    the art, one string per row, all the same length
     * @param palette character to colour; characters missing from it are skipped
     */
    public static void paint(Graphics2D g, String[] rows, Map<Character, Color> palette,
                             double cx, double cy, double boxSize) {
        if (rows.length == 0) {
            return;
        }
        int gridHeight = rows.length;
        int gridWidth = rows[0].length();
        int widest = Math.max(gridWidth, gridHeight);

        // One art pixel, as a whole number of screen pixels, never smaller than 1.
        int pixel = Math.max(1, (int) Math.round(boxSize / widest));

        int left = (int) Math.round(cx - gridWidth * pixel / 2.0);
        int top = (int) Math.round(cy - gridHeight * pixel / 2.0);

        for (int row = 0; row < gridHeight; row++) {
            String line = rows[row];
            for (int col = 0; col < line.length(); col++) {
                char key = line.charAt(col);
                if (key == EMPTY) {
                    continue;
                }
                Color colour = palette.get(key);
                if (colour == null) {
                    continue;
                }
                g.setColor(colour);
                g.fillRect(left + col * pixel, top + row * pixel, pixel, pixel);
            }
        }
    }

    /** Mixes two colours, for shades a palette does not name outright. */
    public static Color blend(Color from, Color to, double amount) {
        double t = Math.max(0, Math.min(1, amount));
        return new Color(
                (int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
                (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                (int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t));
    }
}
