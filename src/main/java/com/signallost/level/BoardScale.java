package com.signallost.level;

/**
 * The board size the tiles are calibrated to, so the character is the same size
 * on every level.
 *
 * <p>A front end that sizes its tiles to fit the level in front of it draws a
 * small level large and a large level small, and the character changes size
 * between them. Players noticed: level 5 is the first board taller than the
 * four before it, and the character shrank on reaching it.
 *
 * <p>The desktop never had the problem because it keeps a preferred tile size
 * and resizes its <em>window</em> to suit. A browser and a phone cannot do
 * that, so they size their tiles once against the numbers here and keep them,
 * whatever level is loaded.
 *
 * <p>These are the dimensions that fit without scrolling, not the largest level
 * that ships. Calibrating to the largest would mean every level drawn at the
 * size the biggest one needs, which shrinks levels 1 to 4 to fix level 5 —
 * fixing the inconsistency by making everything worse. Boards taller or wider
 * than this scroll instead, with the camera following the player, which is what
 * the extra space in a big level is for.
 */
public final class BoardScale {

    /** Rows that fit on screen at the preferred tile size; taller boards scroll. */
    public static final int ROWS_THAT_FIT = 13;

    /** Columns that fit on screen at the preferred tile size; wider boards scroll. */
    public static final int COLS_THAT_FIT = 19;

    private BoardScale() {
    }
}
