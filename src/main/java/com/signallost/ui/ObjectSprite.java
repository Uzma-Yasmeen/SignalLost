package com.signallost.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

/**
 * The things lying on the floor — the keys and the exit — as 16×16 pixel art,
 * drawn the same way as the characters and guards.
 *
 * <p>They used to be a plain diamond and an outlined rectangle, which read as
 * placeholder markers rather than objects. Everything else on the board is
 * pixel art, so these were the only pieces that did not look like they belonged
 * to the same game.
 *
 * <p>The letters in each grid name colours: {@code O} outline, {@code B} body,
 * {@code H} highlight, and a dot is transparent.
 */
public final class ObjectSprite {

    private static final Color OUTLINE = new Color(0x1C222C);

    /** A key, bow at the top and teeth down the right of the shaft. */
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

    /** A door. Drawn hollow when locked and filled when every key is held. */
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

    /** The same door with the opening lit up, used once the exit unlocks. */
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

    /** An earned star on the escape screen. */
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

    /** The hint arrow, drawn pointing up and rotated to suit. */
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

    private ObjectSprite() {
    }

    /**
     * An arrow saying which way to go from here.
     *
     * <p>One arrow on the next tile rather than a trail across the map. A route
     * drawn start to finish is a solution, and reading it is a different job
     * from playing; a single direction is something the player can act on this
     * turn and then ask again.
     *
     * @param quarterTurns 0 up, 1 right, 2 down, 3 left
     */
    public static void paintArrow(Graphics2D g, double cx, double cy, double size,
                                  int quarterTurns) {
        Graphics2D rotated = (Graphics2D) g.create();
        rotated.rotate(Math.PI / 2 * quarterTurns, cx, cy);
        PixelSprite.paint(rotated, ARROW, palette(Palette.HINT, 1.0), cx, cy, size);
        rotated.dispose();
    }

    /**
     * One star of a rating.
     *
     * <p>An unearned star is drawn in the same shape, dimmed rather than
     * omitted, so the player can see at a glance how many they missed.
     */
    public static void paintStar(Graphics2D g, double cx, double cy, double size,
                                 boolean earned) {
        Color body = earned ? Palette.KEY : Palette.TEXT_DIM;
        PixelSprite.paint(g, STAR, palette(body, earned ? 1.0 : 0.35), cx, cy, size);
    }

    public static void paintKey(Graphics2D g, double cx, double cy, double size, double dim) {
        PixelSprite.paint(g, KEY, palette(Palette.KEY, dim), cx, cy, size);
    }

    /**
     * @param open true once the player holds every key, which fills the doorway
     *             and switches it to the go-this-way green
     */
    public static void paintExit(Graphics2D g, double cx, double cy, double size,
                                 boolean open, double dim) {
        Color body = open ? Palette.EXIT_OPEN : Palette.EXIT_LOCKED;
        PixelSprite.paint(g, open ? EXIT_OPEN : EXIT, palette(body, dim), cx, cy, size);
    }

    /** {@code dim} scales brightness, so a remembered object looks remembered. */
    private static Map<Character, Color> palette(Color body, double dim) {
        Map<Character, Color> palette = new HashMap<>();
        palette.put('O', OUTLINE);
        palette.put('B', Palette.shade(body, dim));
        palette.put('H', Palette.shade(PixelSprite.blend(body, Color.WHITE, 0.45), dim));
        return palette;
    }
}
