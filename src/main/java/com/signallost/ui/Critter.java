package com.signallost.ui;

import java.awt.Color;

/**
 * The animal the player picks to play as.
 *
 * <p>Each one is drawn from shapes by {@link CritterRenderer} rather than loaded
 * from an image file, so they stay sharp at any size, need no artwork in the
 * repository, and a new character is a few lines here instead of a sprite sheet.
 *
 * <p>The choice is pure decoration: it changes nothing about movement, sight or
 * the rules, which keeps the game logic testable without a window.
 */
public enum Critter {

    CAT("Mochi", "the quiet one",
            new Color(0xFFB454), new Color(0xE08A2B), new Color(0xFF8FA3), EarShape.POINTED),

    FOX("Kit", "quick on corners",
            new Color(0xFF7A45), new Color(0xD9542A), new Color(0xFFE8D6), EarShape.POINTED),

    PANDA("Bao", "soft but stubborn",
            new Color(0xF2F6FB), new Color(0xC7D2E0), new Color(0x2B3444), EarShape.ROUND),

    FROG("Pip", "small and springy",
            new Color(0x6EE7A5), new Color(0x3FB984), new Color(0xFFD9E0), EarShape.EYES_ON_TOP),

    OWL("Hoot", "sees in the dark",
            new Color(0xA78BFA), new Color(0x7C5CE0), new Color(0xFFCC66), EarShape.TUFTED),

    BUNNY("Momo", "all ears",
            new Color(0xFBC2D9), new Color(0xE493B5), new Color(0xFF7CA8), EarShape.TALL);

    /** How the ears are drawn, which is most of what tells the animals apart. */
    public enum EarShape {
        POINTED, ROUND, TALL, TUFTED, EYES_ON_TOP
    }

    private final String name;
    private final String tagline;
    private final Color fur;
    private final Color furShadow;
    private final Color accent;
    private final EarShape earShape;

    Critter(String name, String tagline, Color fur, Color furShadow, Color accent,
            EarShape earShape) {
        this.name = name;
        this.tagline = tagline;
        this.fur = fur;
        this.furShadow = furShadow;
        this.accent = accent;
        this.earShape = earShape;
    }

    public String displayName() {
        return name;
    }

    /** A line of flavour for the select screen. None of it affects play. */
    public String tagline() {
        return tagline;
    }

    public Color fur() {
        return fur;
    }

    /** The darker fur tone, used for ear insides and the shadow under the chin. */
    public Color furShadow() {
        return furShadow;
    }

    /** Nose, cheeks and other highlights. */
    public Color accent() {
        return accent;
    }

    public EarShape earShape() {
        return earShape;
    }

    /** The colour of the light this character carries through the fog. */
    public Color glow() {
        return fur;
    }

    public static Critter defaultChoice() {
        return CAT;
    }
}
