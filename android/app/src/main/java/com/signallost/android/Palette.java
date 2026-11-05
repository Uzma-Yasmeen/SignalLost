package com.signallost.android;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;

/**
 * The game's colours, in one place.
 *
 * <p>The desktop has its own {@code Palette} in the Swing package, which this
 * module deliberately excludes: reaching across for it would drag AWT onto the
 * phone. So the values are restated here — but they are restated <em>exactly</em>,
 * copied from {@code com.signallost.ui.Palette}.
 *
 * <p>They were once only roughly alike, on the theory that this was the one
 * duplicated thing in the project it would not hurt to let drift. It hurt: a
 * dozen near-miss colours are plainly visible side by side, and the phone looked
 * like a different game. If a value here changes, change it there too.
 */
final class Palette {

    static final int BACKDROP = Color.rgb(0x05, 0x07, 0x0C);
    static final int BACKDROP_TOP = Color.rgb(0x17, 0x20, 0x3C);
    static final int BACKDROP_BOTTOM = Color.rgb(0x08, 0x0B, 0x12);
    static final int PANEL = Color.rgb(0x14, 0x18, 0x24);
    static final int BUTTON = Color.rgb(0x1E, 0x24, 0x36);
    static final int BUTTON_EDGE = Color.rgb(0x3A, 0x46, 0x64);

    static final int WALL_SEEN = Color.rgb(0x6E, 0x86, 0xB2);
    static final int WALL_REMEMBERED = Color.rgb(0x3D, 0x4D, 0x70);
    static final int FLOOR_SEEN = Color.rgb(0x24, 0x32, 0x4B);
    static final int FLOOR_REMEMBERED = Color.rgb(0x12, 0x18, 0x28);
    static final int WATCHED = Color.argb(115, 0xEF, 0x44, 0x44);

    static final int KEY = Color.rgb(0xFB, 0xBF, 0x24);
    static final int EXIT_OPEN = Color.rgb(0x4A, 0xDE, 0x80);
    static final int EXIT_SHUT = Color.rgb(0x9A, 0xA8, 0xBF);
    static final int PLAYER = Color.rgb(0x5E, 0xEA, 0xD4);
    static final int HINT = Color.rgb(0x7D, 0xD3, 0xFC);

    static final int GUARD_PATROL = Color.rgb(0x93, 0xA8, 0xCC);
    static final int GUARD_ALERT = Color.rgb(0xFB, 0xBF, 0x24);
    static final int GUARD_HUNT = Color.rgb(0xF8, 0x71, 0x71);

    static final int TEXT = Color.rgb(0xE6, 0xED, 0xF7);
    static final int TEXT_DIM = Color.rgb(0x75, 0x84, 0x9E);
    static final int ACCENT = Color.rgb(0x38, 0xBD, 0xF8);
    static final int DANGER = Color.rgb(0xEF, 0x44, 0x44);
    static final int LOCKED = Color.rgb(0x36, 0x3E, 0x56);

    private Palette() {
    }

    /** The vertical wash the desktop paints behind every screen. */
    static void backdrop(Canvas canvas, Paint paint, int width, int height) {
        paint.setShader(new LinearGradient(0, 0, 0, height,
                BACKDROP_TOP, BACKDROP_BOTTOM, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(null);
    }

    /** Dims a colour towards black, as the desktop does for distant light. */
    static int shade(int colour, double factor) {
        double f = Math.max(0, Math.min(1, factor));
        return Color.rgb((int) (Color.red(colour) * f), (int) (Color.green(colour) * f),
                (int) (Color.blue(colour) * f));
    }

    static int withAlpha(int colour, int alpha) {
        return Color.argb(alpha, Color.red(colour), Color.green(colour), Color.blue(colour));
    }
}
