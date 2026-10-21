package com.signallost.model;

/** The four moves a player or guard can make, plus staying put. */
public enum Direction {

    UP(-1, 0),
    DOWN(1, 0),
    LEFT(0, -1),
    RIGHT(0, 1),
    WAIT(0, 0);

    private final int rowDelta;
    private final int colDelta;

    Direction(int rowDelta, int colDelta) {
        this.rowDelta = rowDelta;
        this.colDelta = colDelta;
    }

    public Position applyTo(Position from) {
        return new Position(from.row() + rowDelta, from.col() + colDelta);
    }
}
