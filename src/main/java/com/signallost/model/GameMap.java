package com.signallost.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The static level geometry: a rectangular grid of {@link TileType}.
 *
 * <p>This class knows nothing about the player, the guards or the fog. It only
 * answers questions about shape and terrain, which keeps it easy to unit-test
 * and lets the visibility and pathfinding code depend on a tiny interface.
 */
public final class GameMap {

    private final TileType[][] tiles;
    private final int rows;
    private final int cols;

    public GameMap(TileType[][] tiles) {
        if (tiles.length == 0 || tiles[0].length == 0) {
            throw new IllegalArgumentException("A map needs at least one tile");
        }
        this.rows = tiles.length;
        this.cols = tiles[0].length;
        for (TileType[] row : tiles) {
            if (row.length != cols) {
                throw new IllegalArgumentException("All map rows must be the same length");
            }
        }
        this.tiles = tiles;
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    public boolean contains(Position position) {
        return position.row() >= 0 && position.row() < rows
                && position.col() >= 0 && position.col() < cols;
    }

    public TileType tileAt(Position position) {
        requireInside(position);
        return tiles[position.row()][position.col()];
    }

    /** Replaces a tile, used when the key is picked up and the tile becomes floor. */
    public void setTileAt(Position position, TileType type) {
        requireInside(position);
        tiles[position.row()][position.col()] = type;
    }

    public boolean isWalkable(Position position) {
        return contains(position) && tileAt(position).isWalkable();
    }

    public boolean blocksSight(Position position) {
        return !contains(position) || tileAt(position).blocksSight();
    }

    /** In-bounds orthogonal neighbours, walkable or not. */
    public List<Position> neighbours(Position position) {
        List<Position> result = new ArrayList<>(4);
        for (Position candidate : position.orthogonalNeighbours()) {
            if (contains(candidate)) {
                result.add(candidate);
            }
        }
        return result;
    }

    /** In-bounds orthogonal neighbours that can actually be stepped on. */
    public List<Position> walkableNeighbours(Position position) {
        List<Position> result = new ArrayList<>(4);
        for (Position candidate : position.orthogonalNeighbours()) {
            if (isWalkable(candidate)) {
                result.add(candidate);
            }
        }
        return result;
    }

    /** The first tile of the given type, or null when the map has none. */
    public Position findFirst(TileType type) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (tiles[row][col] == type) {
                    return new Position(row, col);
                }
            }
        }
        return null;
    }

    private void requireInside(Position position) {
        if (!contains(position)) {
            throw new IndexOutOfBoundsException("Position " + position + " is outside the map");
        }
    }
}
