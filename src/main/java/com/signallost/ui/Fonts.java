package com.signallost.ui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Picks the nicest font families actually installed, so the game looks right on
 * this machine but still runs on one with a bare font set.
 */
public final class Fonts {

    private static final Set<String> AVAILABLE = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));

    private static final String DISPLAY_FAMILY =
            firstAvailable("Segoe UI Semibold", "Segoe UI", "Inter", Font.SANS_SERIF);
    private static final String BODY_FAMILY =
            firstAvailable("Segoe UI", "Inter", Font.SANS_SERIF);
    private static final String MONO_FAMILY =
            firstAvailable("Cascadia Mono", "Consolas", Font.MONOSPACED);

    private Fonts() {
    }

    /** Headings and the title, with wide tracking applied by the caller. */
    public static Font display(float size) {
        return new Font(DISPLAY_FAMILY, Font.BOLD, Math.round(size));
    }

    public static Font body(float size) {
        return new Font(BODY_FAMILY, Font.PLAIN, Math.round(size));
    }

    public static Font bodyBold(float size) {
        return new Font(BODY_FAMILY, Font.BOLD, Math.round(size));
    }

    /** Numbers that should not jiggle as they change. */
    public static Font mono(float size) {
        return new Font(MONO_FAMILY, Font.BOLD, Math.round(size));
    }

    private static String firstAvailable(String... families) {
        for (String family : families) {
            if (AVAILABLE.contains(family)) {
                return family;
            }
        }
        return Font.SANS_SERIF;
    }
}
