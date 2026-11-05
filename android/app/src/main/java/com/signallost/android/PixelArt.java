package com.signallost.android;

import com.signallost.art.Critter;
import com.signallost.art.CritterPixels;
import com.signallost.art.GuardPixels;
import com.signallost.art.ObjectPixels;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.Map;

/**
 * Draws the shared pixel art onto a Canvas.
 *
 * <p>Everything the game shows — the characters, the guard robot, the key, the
 * exit — is a 16x16 grid of letters in {@code com.signallost.art}, and the
 * desktop draws those same grids with a {@code Graphics2D}. This class is the
 * whole of the difference between the two: one loop, filling rectangles.
 */
final class PixelArt {

    private static final Paint PIXEL = new Paint();

    private PixelArt() {
    }

    private interface Palette {
        Integer colourOf(char letter);
    }

    static void critter(Canvas canvas, Critter who, float cx, float cy, float size) {
        paint(canvas, CritterPixels.shapeOf(who),
                letter -> CritterPixels.colourOf(who, letter), cx, cy, size);
    }

    static void guard(Canvas canvas, String modeName, float cx, float cy, float size) {
        Map<Character, Integer> palette = GuardPixels.paletteFor(modeName);
        paint(canvas, GuardPixels.shape(), palette::get, cx, cy, size);
    }

    static void key(Canvas canvas, float cx, float cy, float size) {
        tinted(canvas, ObjectPixels.key(), ObjectPixels.KEY_COLOUR, cx, cy, size);
    }

    static void exit(Canvas canvas, boolean open, float cx, float cy, float size) {
        tinted(canvas, ObjectPixels.exit(open),
                open ? ObjectPixels.EXIT_UNLOCKED : ObjectPixels.EXIT_LOCKED, cx, cy, size);
    }

    static void star(Canvas canvas, int colour, float cx, float cy, float size) {
        tinted(canvas, ObjectPixels.star(), colour, cx, cy, size);
    }

    private static void tinted(Canvas canvas, String[] rows, int colour,
                               float cx, float cy, float size) {
        Map<Character, Integer> palette = ObjectPixels.paletteOf(colour);
        paint(canvas, rows, palette::get, cx, cy, size);
    }

    private static void paint(Canvas canvas, String[] rows, Palette palette,
                              float cx, float cy, float size) {
        // Snapped to whole pixels before centring: scaling pixel art by a
        // fraction is what makes it look smeared rather than sharp.
        float step = Math.max(1f, (float) Math.floor(size / rows.length));
        float left = cx - step * rows.length / 2f;
        float top = cy - step * rows.length / 2f;

        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            for (int col = 0; col < line.length(); col++) {
                Integer packed = palette.colourOf(line.charAt(col));
                if (packed == null || packed == CritterPixels.TRANSPARENT) {
                    continue;
                }
                PIXEL.setColor(Color.rgb((packed >> 16) & 0xFF, (packed >> 8) & 0xFF,
                        packed & 0xFF));
                float x = left + col * step;
                float y = top + row * step;
                // A hair of overlap, or seams show between pixels when the step
                // lands on a fractional device pixel.
                canvas.drawRect(x, y, x + step + 0.5f, y + step + 0.5f, PIXEL);
            }
        }
    }

    static int colourOf(Critter critter) {
        int packed = critter.fur();
        return Color.rgb((packed >> 16) & 0xFF, (packed >> 8) & 0xFF, packed & 0xFF);
    }
}
