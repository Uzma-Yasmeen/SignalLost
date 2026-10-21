package com.signallost.model;

import java.util.List;

/**
 * An immutable grid coordinate. Rows grow downward, columns grow rightward,
 * so (0,0) is the top-left tile of the map.
 *
 * <p>Being a record gives us correct {@code equals} and {@code hashCode} for
 * free, which matters because positions are used as keys in the sets and maps
 * that the BFS and A* searches rely on.
 */
public record Position(int row, int col) {

    /** The four tiles orthogonally touching this one, in a fixed order. */
    public List<Position> orthogonalNeighbours() {
        return List.of(
                new Position(row - 1, col),
                new Position(row + 1, col),
                new Position(row, col - 1),
                new Position(row, col + 1));
    }

    /** Manhattan distance, the natural metric for 4-directional movement. */
    public int manhattanDistanceTo(Position other) {
        return Math.abs(row - other.row) + Math.abs(col - other.col);
    }

    @Override
    public String toString() {
        return "(" + row + "," + col + ")";
    }
}
