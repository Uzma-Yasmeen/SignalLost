package com.signallost.model;

/**
 * What a single map tile is. The character is the symbol used in the plain-text
 * level files under {@code src/main/resources/levels}.
 */
public enum TileType {

    FLOOR('.', true),
    WALL('#', false),
    KEY('K', true),
    EXIT('E', true),
    START('@', true);

    private final char symbol;
    private final boolean walkable;

    TileType(char symbol, boolean walkable) {
        this.symbol = symbol;
        this.walkable = walkable;
    }

    public char symbol() {
        return symbol;
    }

    /** Whether the player or a guard may stand here. */
    public boolean isWalkable() {
        return walkable;
    }

    /** Whether this tile stops light, and therefore stops the fog-of-war BFS. */
    public boolean blocksSight() {
        return this == WALL;
    }

    public static TileType fromSymbol(char symbol) {
        for (TileType type : values()) {
            if (type.symbol == symbol) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown tile symbol: '" + symbol + "'");
    }
}
