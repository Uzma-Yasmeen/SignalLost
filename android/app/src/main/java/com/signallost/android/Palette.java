package com.signallost.android;

import android.graphics.Color;

/**
 * The game's colours, in one place.
 *
 * <p>The desktop has its own {@code Palette} in the Swing package, which this
 * module deliberately excludes. Rather than reach across for it and drag AWT
 * along, the handful of colours are restated here. They are the only thing in
 * the project duplicated between the two front ends, and the only thing it
 * would not hurt to let drift.
 */
final class Palette {

    static final int BACKDROP = Color.rgb(10, 12, 18);
    static final int PANEL = Color.rgb(20, 24, 36);
    static final int BUTTON = Color.rgb(30, 36, 54);
    static final int BUTTON_EDGE = Color.rgb(58, 70, 100);

    static final int WALL_SEEN = Color.rgb(128, 148, 196);
    static final int WALL_REMEMBERED = Color.rgb(44, 52, 76);
    static final int FLOOR_SEEN = Color.rgb(26, 32, 48);
    static final int FLOOR_REMEMBERED = Color.rgb(16, 20, 30);
    static final int WATCHED = Color.argb(115, 220, 60, 70);

    static final int KEY = Color.rgb(246, 196, 70);
    static final int EXIT_OPEN = Color.rgb(92, 214, 140);
    static final int EXIT_SHUT = Color.rgb(90, 100, 128);
    static final int PLAYER = Color.rgb(244, 164, 96);
    static final int HINT = Color.rgb(96, 170, 255);

    static final int GUARD_PATROL = Color.rgb(96, 136, 212);
    static final int GUARD_ALERT = Color.rgb(240, 176, 64);
    static final int GUARD_HUNT = Color.rgb(232, 70, 84);

    static final int TEXT = Color.rgb(232, 236, 246);
    static final int TEXT_DIM = Color.rgb(140, 150, 175);
    static final int ACCENT = Color.rgb(96, 170, 255);
    static final int DANGER = Color.rgb(232, 70, 84);
    static final int LOCKED = Color.rgb(54, 62, 86);

    private Palette() {
    }

    static int withAlpha(int colour, int alpha) {
        return Color.argb(alpha, Color.red(colour), Color.green(colour), Color.blue(colour));
    }
}
