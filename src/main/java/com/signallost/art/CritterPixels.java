package com.signallost.art;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * The characters as 16x16 pixel art, written as text.
 *
 * <p>Each row is sixteen characters, and each character names a colour:
 *
 * <pre>
 *   .  transparent      B  fur             W  eye white
 *   O  outline          D  dark marking    E  pupil
 *   A  accent           c  cheek           m  muzzle
 * </pre>
 *
 * <p>Most of the colours come from the {@link Critter} itself, so one grid
 * produces a ginger cat and a pink rabbit. Only the shapes are written out,
 * which is why the six look like a set rather than six separate drawings.
 *
 * <p>Art living in the source as readable text shows up properly in a diff, can
 * be reviewed like any other code, and — since this package knows nothing about
 * how it will be drawn — is the same art on a laptop and on a phone.
 */
public final class CritterPixels {

    /** A dark outline on every character, which is what makes them show up on a dark floor. */
    public static final int OUTLINE = 0x1C222C;
    public static final int EYE_WHITE = 0xFFFBF2;
    public static final int PUPIL = 0x242C3A;
    public static final int MUZZLE = 0xFFE8D6;

    /** Returned for '.', meaning draw nothing here. */
    public static final int TRANSPARENT = -1;

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
    private static final Map<Critter, Map<Character, Integer>> PALETTES =
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

    private CritterPixels() {
    }

    /** The sixteen rows of this character. */
    public static String[] shapeOf(Critter critter) {
        return SHAPES.get(critter);
    }

    /** The packed colour for one letter, or {@link #TRANSPARENT}. */
    public static int colourOf(Critter critter, char letter) {
        Integer colour = PALETTES.get(critter).get(letter);
        return colour == null ? TRANSPARENT : colour;
    }

    private static Map<Character, Integer> paletteFor(Critter critter) {
        // The panda's pale fur shadow would vanish against its white head, so
        // its markings use the near-black accent instead.
        int marking = critter == Critter.PANDA ? critter.accent() : critter.furShadow();

        Map<Character, Integer> palette = new HashMap<>();
        palette.put('O', OUTLINE);
        palette.put('B', critter.fur());
        palette.put('D', marking);
        palette.put('d', marking);
        palette.put('A', critter.accent());
        palette.put('W', EYE_WHITE);
        palette.put('E', PUPIL);
        palette.put('c', Critter.blend(critter.fur(), critter.accent(), 0.55));
        palette.put('m', MUZZLE);
        return palette;
    }
}
