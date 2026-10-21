package com.signallost.ui;

import java.awt.Color;

/**
 * Every colour the game draws, in one place.
 *
 * <p>The scheme is a cold facility at night: near-black voids, slate-blue stone
 * and a single warm accent for the key, so the one thing you are looking for is
 * also the one warm colour on screen.
 */
public final class Palette {

    private Palette() {
    }

    /** Behind everything. */
    public static final Color BACKDROP = new Color(0x080B12);
    public static final Color HUD_BACKGROUND = new Color(0x0C1220);
    public static final Color HUD_EDGE = new Color(0x1C2638);

    /** Tiles the player has never seen. */
    public static final Color UNEXPLORED = new Color(0x05070C);

    /**
     * Tiles in sight right now. Floor sits well below wall in brightness so the
     * layout stays readable with the developer reveal on, where every tile is
     * drawn at full light and shading can no longer separate them.
     */
    public static final Color FLOOR_LIT = new Color(0x24324B);
    public static final Color WALL_LIT = new Color(0x6E86B2);
    public static final Color WALL_LIT_TOP = new Color(0x90A6CE);

    /** Tiles seen earlier and drawn from memory. */
    public static final Color FLOOR_REMEMBERED = new Color(0x161D2B);
    public static final Color WALL_REMEMBERED = new Color(0x252F43);

    /** Actors and objectives. */
    public static final Color PLAYER = new Color(0x5EEAD4);
    public static final Color PLAYER_GLOW = new Color(0x5EEAD4);
    public static final Color KEY = new Color(0xFBBF24);
    public static final Color EXIT_LOCKED = new Color(0x9AA8BF);
    public static final Color EXIT_OPEN = new Color(0x4ADE80);

    /** Guards, coloured by what they are doing. */
    public static final Color GUARD_PATROL = new Color(0x93A8CC);
    public static final Color GUARD_ALERT = new Color(0xFBBF24);
    public static final Color GUARD_HUNT = new Color(0xF87171);
    public static final Color GUARD_CHASSIS = new Color(0x3A4663);

    /** The suggested-route trail. */
    public static final Color HINT = new Color(0x7DD3FC);

    /** The wash over tiles a guard is watching. */
    public static final Color DANGER = new Color(0xEF4444);

    /** Text. */
    public static final Color TEXT = new Color(0xE6EDF7);
    public static final Color TEXT_DIM = new Color(0x75849E);
    public static final Color ACCENT = new Color(0x38BDF8);

    /** Multiplies a colour's brightness, used for the light falloff. */
    public static Color shade(Color base, double factor) {
        double clamped = Math.max(0.0, Math.min(1.0, factor));
        return new Color(
                (int) Math.round(base.getRed() * clamped),
                (int) Math.round(base.getGreen() * clamped),
                (int) Math.round(base.getBlue() * clamped));
    }

    /** The same colour at a given alpha, for glows and overlays. */
    public static Color withAlpha(Color base, int alpha) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(),
                Math.max(0, Math.min(255, alpha)));
    }
}
