package com.signallost.art;

import java.util.HashMap;
import java.util.Map;

/**
 * The things lying about a level: the key, the exit, a star, an arrow.
 *
 * <p>Same arrangement as the characters and the guard. The grids are here, the
 * drawing is wherever it is being drawn, and every front end therefore shows
 * the same key rather than its own idea of one.
 */
public final class ObjectPixels {

    public static final int OUTLINE = 0x161B24;
    public static final int KEY_COLOUR = 0xFBBF24;
    public static final int EXIT_LOCKED = 0x9AA8BF;
    public static final int EXIT_UNLOCKED = 0x4ADE80;

    private static final String[] KEY = {
            "................",
            ".....OOOOOO.....",
            "....OBBBBBBO....",
            "...OBBOOOOBBO...",
            "...OBO....OBO...",
            "...OBBOOOOBBO...",
            "....OBBBBBBO....",
            ".....OBBBBO.....",
            "......OBBO......",
            "......OBBO......",
            "......OBBBBO....",
            "......OBBO......",
            "......OBBBBO....",
            "......OBBO......",
            ".......OO.......",
            "................",
    };

    private static final String[] EXIT = {
            "................",
            "................",
            "..OOOOOOOOOOOO..",
            "..OBBBBBBBBBBO..",
            "..OBOOOOOOOOBO..",
            "..OBO......OBO..",
            "..OBO......OBO..",
            "..OBO...HH.OBO..",
            "..OBO......OBO..",
            "..OBO......OBO..",
            "..OBO......OBO..",
            "..OBOOOOOOOOBO..",
            "..OBBBBBBBBBBO..",
            "..OOOOOOOOOOOO..",
            "................",
            "................",
    };

    private static final String[] EXIT_OPEN = {
            "................",
            "................",
            "..OOOOOOOOOOOO..",
            "..OBBBBBBBBBBO..",
            "..OBOOOOOOOOBO..",
            "..OBOHHHHHHOBO..",
            "..OBOHHHHHHOBO..",
            "..OBOHHHHHHOBO..",
            "..OBOHHHHHHOBO..",
            "..OBOHHHHHHOBO..",
            "..OBOHHHHHHOBO..",
            "..OBOOOOOOOOBO..",
            "..OBBBBBBBBBBO..",
            "..OOOOOOOOOOOO..",
            "................",
            "................",
    };

    private static final String[] STAR = {
            "................",
            ".......BB.......",
            "......BBBB......",
            "......BBBB......",
            "..BBBBBBBBBBBB..",
            "..BBBBBBBBBBBB..",
            "...BBBBBBBBBB...",
            "....BBBBBBBB....",
            "....BBBBBBBB....",
            "...BBBBBBBBBB...",
            "..BBBB....BBBB..",
            "..BBB......BBB..",
            "..BB........BB..",
            "................",
            "................",
            "................",
    };

    private static final String[] ARROW = {
            "................",
            ".......BB.......",
            "......BBBB......",
            ".....BBBBBB.....",
            "....BBBBBBBB....",
            "...BBBBBBBBBB...",
            "..BBBBBBBBBBBB..",
            "..BBBBBBBBBBBB..",
            ".....BBBBBB.....",
            ".....BBBBBB.....",
            ".....BBBBBB.....",
            ".....BBBBBB.....",
            ".....BBBBBB.....",
            "................",
            "................",
            "................",
    };

    private ObjectPixels() {
    }

    public static String[] key() {
        return KEY;
    }

    public static String[] exit(boolean open) {
        return open ? EXIT_OPEN : EXIT;
    }

    public static String[] star() {
        return STAR;
    }

    public static String[] arrow() {
        return ARROW;
    }

    /** One tint applied to a whole sprite, which is how these are coloured. */
    public static Map<Character, Integer> paletteOf(int colour) {
        Map<Character, Integer> palette = new HashMap<>();
        palette.put('O', OUTLINE);
        palette.put('B', colour);
        palette.put('D', Critter.blend(colour, OUTLINE, 0.4));
        palette.put('W', Critter.blend(colour, 0xFFFFFF, 0.5));
        palette.put('A', Critter.blend(colour, 0xFFFFFF, 0.25));
        palette.put('E', OUTLINE);
        return palette;
    }
}
