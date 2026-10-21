package com.signallost.vision;

/** How the player currently perceives a tile. Drives the three fog shades. */
public enum TileVisibility {

    /** In sight right now: drawn brightly. */
    VISIBLE,

    /** Seen earlier but not now: drawn dim, from memory. */
    REMEMBERED,

    /** Never seen: drawn as solid darkness. */
    UNEXPLORED
}
