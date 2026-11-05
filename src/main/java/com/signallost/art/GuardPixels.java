package com.signallost.art;

import java.util.HashMap;
import java.util.Map;

/**
 * The guard robot, as 16x16 pixel art.
 *
 * <p>One chassis for every guard; the lens takes its colour from what the guard
 * is doing. Lifted out of the Swing package for the same reason the characters
 * were: three front ends draw this now, and three copies of it would be three
 * drawings that slowly stopped matching.
 *
 * <p>Note what this does <em>not</em> distinguish: a hunter from a patroller.
 * They differ only by lens colour, so a hunter that has not noticed you looks
 * exactly like the guard that merely blocks a corridor. Front ends are expected
 * to mark the difference themselves.
 */
public final class GuardPixels {

    public static final int OUTLINE = 0x161B24;
    public static final int CHASSIS = 0x3A4663;
    public static final int PATROL = 0x93A8CC;
    public static final int ALERT = 0xFBBF24;
    public static final int HUNT = 0xF87171;

    private static final String[] ROBOT = {
            "................",
            ".......OO.......",
            ".......OO.......",
            "..OOOOOOOOOOOO..",
            "..OBBBBBBBBBBO..",
            "..OBDDDDDDDDBO..",
            "..OBDWWEEWWDBO..",
            "..OBDWEEEEWDBO..",
            "..OBDDDDDDDDBO..",
            "..OBBBBBBBBBBO..",
            "..OBBOOOOOOBBO..",
            "..OOOOOOOOOOOO..",
            "...O.O....O.O...",
            "...OOO....OOO...",
            "................",
            "................",
    };

    private GuardPixels() {
    }

    public static String[] shape() {
        return ROBOT;
    }

    /** The lens colour for a mode, named so callers need not know the order. */
    public static int lensFor(String modeName) {
        return switch (modeName) {
            case "ALERT" -> ALERT;
            case "HUNT" -> HUNT;
            default -> PATROL;
        };
    }

    public static Map<Character, Integer> paletteFor(String modeName) {
        int lens = lensFor(modeName);
        Map<Character, Integer> palette = new HashMap<>();
        palette.put('O', OUTLINE);
        palette.put('B', CHASSIS);
        palette.put('D', Critter.blend(CHASSIS, OUTLINE, 0.45));
        palette.put('W', Critter.blend(lens, 0xFFFFFF, 0.55));
        palette.put('E', lens);
        return palette;
    }
}
