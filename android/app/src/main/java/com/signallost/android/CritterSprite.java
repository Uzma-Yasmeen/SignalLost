package com.signallost.android;

import com.signallost.art.Critter;
import com.signallost.art.CritterPixels;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Draws a character from the shared 16x16 grids.
 *
 * <p>The desktop has its own renderer doing exactly this with a
 * {@code Graphics2D}; this one uses a {@code Canvas}. Both read the same rows
 * and the same colours out of {@link CritterPixels}, so Mochi is the same cat
 * on a laptop and on a phone — not a cat redrawn to look similar.
 */
final class CritterSprite {

    private static final Paint PIXEL = new Paint();

    private CritterSprite() {
    }

    /**
     * Paints the animal centred on a point.
     *
     * @param size the width the whole sprite should occupy
     */
    static void paint(Canvas canvas, Critter critter, float cx, float cy, float size) {
        String[] rows = CritterPixels.shapeOf(critter);
        // Snapped to whole pixels before centring: scaling pixel art by a
        // fraction is what makes it look smeared rather than sharp.
        float step = Math.max(1f, (float) Math.floor(size / rows.length));
        float left = cx - step * rows.length / 2f;
        float top = cy - step * rows.length / 2f;

        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            for (int col = 0; col < line.length(); col++) {
                int packed = CritterPixels.colourOf(critter, line.charAt(col));
                if (packed == CritterPixels.TRANSPARENT) {
                    continue;
                }
                PIXEL.setColor(Color.rgb((packed >> 16) & 0xFF, (packed >> 8) & 0xFF, packed & 0xFF));
                float x = left + col * step;
                float y = top + row * step;
                // A hair of overlap, or seams show between pixels when the
                // step lands on a fractional device pixel.
                canvas.drawRect(x, y, x + step + 0.5f, y + step + 0.5f, PIXEL);
            }
        }
    }

    /** The character's own colour, for glows and highlights. */
    static int colourOf(Critter critter) {
        int packed = critter.fur();
        return Color.rgb((packed >> 16) & 0xFF, (packed >> 8) & 0xFF, packed & 0xFF);
    }
}
