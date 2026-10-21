package com.signallost.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * The characters, drawn as 16×16 pixel art.
 *
 * <p>Each animal is a block of text below. The characters in it name colours:
 *
 * <pre>
 *   .  transparent      B  fur             W  eye white
 *   O  outline          D  dark marking    E  pupil
 *   A  accent           c  cheek           m  muzzle
 * </pre>
 *
 * <p>The colours come from the {@link Critter} itself, so the same grid produces
 * a ginger cat and a pink bunny. Only the shapes are written out here, which is
 * why the six animals look like a set rather than six separate drawings.
 *
 * <p>This is the fallback used when no sprite files are installed — see
 * {@link CritterArt}. Anyone adding their own art can match it by working at
 * 16×16, or ignore it entirely and work at any size they like.
 */
public final class CritterRenderer {

    /** A dark outline on every character, which is what makes them pop on a dark floor. */
    private static final Color OUTLINE = new Color(0x1C222C);
    private static final Color EYE_WHITE = new Color(0xFFFBF2);
    private static final Color PUPIL = new Color(0x242C3A);
    private static final Color MUZZLE = new Color(0xFFE8D6);

    private static final String[] CAT = {
            "................",
            "..O..........O..",
            "..OO........OO..",
            "..OAO......OAO..",
            "..OAAOOOOOOAAO..",
            "..OBBBBBBBBBBO..",
            "..OBBBBBBBBBBO..",
            "..OBWEBBBBEWBO..",
            "..OBBBBBBBBBBO..",
            "..OBBBBAABBBBO..",
            "..OBcBBAABBcBO..",
            "..OBBBBBBBBBBO..",
            "..OOBBBBBBBBOO..",
            "....OOOOOOOO....",
            "................",
            "................",
    };

    private static final String[] FOX = {
            "................",
            "..O..........O..",
            "..OO........OO..",
            "..OAO......OAO..",
            "..OAAOOOOOOAAO..",
            "..OBBBBBBBBBBO..",
            "..OBBBBBBBBBBO..",
            "..OBWEBBBBEWBO..",
            "..OBBBBBBBBBBO..",
            "..OBBBmmmmBBBO..",
            "..OBBBmAAmBBBO..",
            "..OBBBBmmBBBBO..",
            "..OOBBBBBBBBOO..",
            "....OOOOOOOO....",
            "................",
            "................",
    };

    private static final String[] PANDA = {
            "................",
            "..DD........DD..",
            ".DDDD......DDDD.",
            ".DDDD......DDDD.",
            "..DDDOOOOOODDD..",
            "..OBBBBBBBBBBO..",
            "..OBDDBBBBDDBO..",
            "..OBDWEBBEWDBO..",
            "..OBDDBBBBDDBO..",
            "..OBBBBAABBBBO..",
            "..OBBBBBBBBBBO..",
            "..OBBBBBBBBBBO..",
            "..OOBBBBBBBBOO..",
            "....OOOOOOOO....",
            "................",
            "................",
    };

    private static final String[] FROG = {
            "................",
            "...OOOO..OOOO...",
            "...OWWO..OWWO...",
            "...OWEO..OEWO...",
            "..OOOOOOOOOOOO..",
            "..OBBBBBBBBBBO..",
            "..OBBBBBBBBBBO..",
            "..OBBBdBBdBBBO..",
            "..OBBBBBBBBBBO..",
            "..OBOBBBBBBOBO..",
            "..OBBOOOOOOBBO..",
            "..OBBBBBBBBBBO..",
            "..OOBBBBBBBBOO..",
            "....OOOOOOOO....",
            "................",
            "................",
    };

    private static final String[] OWL = {
            "................",
            "...OO......OO...",
            "..OOO......OOO..",
            "..OBO......OBO..",
            "..OBBOOOOOOBBO..",
            "..OBBBBBBBBBBO..",
            "..OBWWWBBWWWBO..",
            "..OBWEWBBWEWBO..",
            "..OBWWWAAWWWBO..",
            "..OBBBBAABBBBO..",
            "..OBBBBBBBBBBO..",
            "..OBBBBBBBBBBO..",
            "..OOBBBBBBBBOO..",
            "....OOOOOOOO....",
            "................",
            "................",
    };

    private static final String[] BUNNY = {
            "...OO......OO...",
            "...OAO....OAO...",
            "...OAO....OAO...",
            "...OAO....OAO...",
            "...OAO....OAO...",
            "..OOBOOOOOOBOO..",
            "..OBBBBBBBBBBO..",
            "..OBWEBBBBEWBO..",
            "..OBBBBBBBBBBO..",
            "..OBBBBAABBBBO..",
            "..OBcBBAABBcBO..",
            "..OBBBBBBBBBBO..",
            "..OOBBBBBBBBOO..",
            "....OOOOOOOO....",
            "................",
            "................",
    };

    private static final Map<Critter, String[]> SHAPES = new EnumMap<>(Critter.class);
    private static final Map<Critter, Map<Character, Color>> PALETTES =
            new EnumMap<>(Critter.class);

    static {
        SHAPES.put(Critter.CAT, CAT);
        SHAPES.put(Critter.FOX, FOX);
        SHAPES.put(Critter.PANDA, PANDA);
        SHAPES.put(Critter.FROG, FROG);
        SHAPES.put(Critter.OWL, OWL);
        SHAPES.put(Critter.BUNNY, BUNNY);

        for (Critter critter : Critter.values()) {
            PALETTES.put(critter, paletteFor(critter));
        }
    }

    private CritterRenderer() {
    }

    /** Paints the animal centred on {@code (cx, cy)} at the given head radius. */
    public static void paint(Graphics2D g, Critter critter, double cx, double cy, double r) {
        PixelSprite.paint(g, SHAPES.get(critter), PALETTES.get(critter), cx, cy, r * 2.3);
    }

    private static Map<Character, Color> paletteFor(Critter critter) {
        // The panda's pale fur shadow would vanish against its white head, so its
        // markings use the near-black accent instead.
        Color marking = critter == Critter.PANDA ? critter.accent() : critter.furShadow();

        Map<Character, Color> palette = new HashMap<>();
        palette.put('O', OUTLINE);
        palette.put('B', critter.fur());
        palette.put('D', marking);
        palette.put('d', marking);
        palette.put('A', critter.accent());
        palette.put('W', EYE_WHITE);
        palette.put('E', PUPIL);
        palette.put('c', PixelSprite.blend(critter.fur(), critter.accent(), 0.55));
        palette.put('m', MUZZLE);
        return palette;
    }
}
