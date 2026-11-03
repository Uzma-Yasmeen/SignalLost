package com.signallost.art;

/**
 * The six characters, as data rather than as drawing.
 *
 * <p>This lived in the Swing package and held {@code java.awt.Color}, which
 * meant the phone could not have the same characters without either dragging
 * AWT onto Android or drawing a second set that would drift. Colours are plain
 * packed RGB here, so the desktop wraps them in a {@code Color} and Android
 * passes them straight to a {@code Paint}.
 *
 * <p>Nothing in this package draws anything. It says what a cat looks like;
 * each front end decides how to put that on a screen.
 */
public enum Critter {

    CAT("Mochi", "the quiet one", 0xFFB454, 0xE08A2B, 0xFF8FA3, EarShape.POINTED),
    FOX("Kit", "quick on corners", 0xFF7A45, 0xD9542A, 0xFFE8D6, EarShape.POINTED),
    PANDA("Bao", "soft but stubborn", 0xF2F6FB, 0xC7D2E0, 0x2B3444, EarShape.ROUND),
    FROG("Pip", "small and springy", 0x6EE7A5, 0x3FB984, 0xFFD9E0, EarShape.EYES_ON_TOP),
    OWL("Hoot", "sees in the dark", 0xA78BFA, 0x7C5CE0, 0xFFCC66, EarShape.TUFTED),
    BUNNY("Momo", "all ears", 0xFBC2D9, 0xE493B5, 0xFF7CA8, EarShape.TALL);

    public enum EarShape {
        POINTED, ROUND, TALL, TUFTED, EYES_ON_TOP
    }

    private final String displayName;
    private final String tagline;
    private final int fur;
    private final int furShadow;
    private final int accent;
    private final EarShape earShape;

    Critter(String displayName, String tagline, int fur, int furShadow, int accent,
            EarShape earShape) {
        this.displayName = displayName;
        this.tagline = tagline;
        this.fur = fur;
        this.furShadow = furShadow;
        this.accent = accent;
        this.earShape = earShape;
    }

    public String displayName() {
        return displayName;
    }

    public String tagline() {
        return tagline;
    }

    /** Packed 0xRRGGBB. */
    public int fur() {
        return fur;
    }

    public int furShadow() {
        return furShadow;
    }

    public int accent() {
        return accent;
    }

    public EarShape earShape() {
        return earShape;
    }

    /** The colour a character lights the floor with; the same as its fur. */
    public int glow() {
        return fur;
    }

    /** Who you play as before choosing anybody. */
    public static Critter defaultChoice() {
        return CAT;
    }

    /** Mixes two packed colours, for the cheek blush and similar. */
    public static int blend(int first, int second, double towardsSecond) {
        double keep = 1 - towardsSecond;
        int red = (int) (((first >> 16) & 0xFF) * keep + ((second >> 16) & 0xFF) * towardsSecond);
        int green = (int) (((first >> 8) & 0xFF) * keep + ((second >> 8) & 0xFF) * towardsSecond);
        int blue = (int) ((first & 0xFF) * keep + (second & 0xFF) * towardsSecond);
        return (red << 16) | (green << 8) | blue;
    }
}
