package com.signallost.ui;

import com.signallost.art.Critter;
import com.signallost.art.CritterPixels;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Draws a character from the shared pixel art.
 *
 * <p>The grids and the colours now live in {@link CritterPixels}, which knows
 * nothing about Swing, so the phone draws the same sixteen rows from the same
 * source. All this class does is turn packed colours into AWT ones once, and
 * hand them to {@link PixelSprite}.
 *
 * <p>It is the fallback used when no sprite files are installed — see
 * {@link CritterArt}. Anyone adding their own art can match it by working at
 * 16×16, or ignore it and work at any size they like.
 */
public final class CritterRenderer {

    private static final Map<Critter, Map<Character, Color>> PALETTES =
            new EnumMap<>(Critter.class);

    static {
        for (Critter critter : Critter.values()) {
            PALETTES.put(critter, awtPaletteFor(critter));
        }
    }

    private CritterRenderer() {
    }

    /** Paints the animal centred on {@code (cx, cy)} at the given head radius. */
    public static void paint(Graphics2D g, Critter critter, double cx, double cy, double r) {
        PixelSprite.paint(g, CritterPixels.shapeOf(critter), PALETTES.get(critter),
                cx, cy, r * 2.3);
    }

    private static Map<Character, Color> awtPaletteFor(Critter critter) {
        Map<Character, Color> palette = new HashMap<>();
        for (char letter : new char[]{'O', 'B', 'D', 'd', 'A', 'W', 'E', 'c', 'm'}) {
            int packed = CritterPixels.colourOf(critter, letter);
            if (packed != CritterPixels.TRANSPARENT) {
                palette.put(letter, new Color(packed));
            }
        }
        return palette;
    }
}
