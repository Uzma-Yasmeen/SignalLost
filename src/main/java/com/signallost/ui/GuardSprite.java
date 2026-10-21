package com.signallost.ui;

import com.signallost.guard.GuardMode;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

/**
 * The guards, drawn as 16×16 pixel art to match the characters.
 *
 * <p>One little robot, recoloured by what it is doing: its lens and the glow
 * around it are calm blue on patrol, amber while searching and red while
 * hunting. Shape stays the same so a guard is always recognisably a guard; only
 * the colour says how much trouble you are in.
 */
public final class GuardSprite {

    private static final Color OUTLINE = new Color(0x161B24);

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

    private GuardSprite() {
    }

    /** Paints a guard centred on {@code (cx, cy)}, coloured by its mode. */
    public static void paint(Graphics2D g, GuardMode mode, double cx, double cy, double size) {
        PixelSprite.paint(g, ROBOT, paletteFor(mode), cx, cy, size);
    }

    /** The colour that says what this guard is up to. */
    public static Color colourOf(GuardMode mode) {
        return switch (mode) {
            case PATROL -> Palette.GUARD_PATROL;
            case ALERT -> Palette.GUARD_ALERT;
            case HUNT -> Palette.GUARD_HUNT;
        };
    }

    private static Map<Character, Color> paletteFor(GuardMode mode) {
        Color lens = colourOf(mode);

        Map<Character, Color> palette = new HashMap<>();
        palette.put('O', OUTLINE);
        palette.put('B', Palette.GUARD_CHASSIS);
        palette.put('D', PixelSprite.blend(Palette.GUARD_CHASSIS, OUTLINE, 0.45));
        palette.put('W', PixelSprite.blend(lens, Color.WHITE, 0.55));
        palette.put('E', lens);
        return palette;
    }
}
