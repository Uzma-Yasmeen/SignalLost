package com.signallost.guard;

import com.signallost.model.Direction;
import com.signallost.model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds guards from the one-line descriptions in a level file.
 *
 * <p>The format is a type, then the tiles it walks, as {@code row,col} pairs:
 *
 * <pre>
 *   patroller 1,7 1,15 2,15 2,7
 *   hunter 13,10
 *   sentinel 5,7 left
 * </pre>
 *
 * <p>The first tile is where the guard starts; all of them together are its
 * patrol route, looping back to the first. A guard given a single tile stands
 * there, which is how a hunter gets a post to return to after a chase.
 *
 * <p>A sentinel takes one tile and, optionally, the way it starts out facing.
 * It is the only guard that cares, because it is the only one whose sight is a
 * cone rather than a circle.
 *
 * <p>This is the Factory in the project: level files name guards as text and
 * never mention a Java class, so a new guard type is a new {@code case} here
 * rather than a change to the loader or the game.
 */
public final class GuardFactory {

    private GuardFactory() {
    }

    /**
     * Creates one guard from its line of a level file.
     *
     * @param specification e.g. {@code "patroller 1,7 1,15 2,15 2,7"}
     * @param ordinal       position in the file, used to name the guard
     * @throws IllegalArgumentException when the type or the tiles are malformed
     */
    public static Guard fromSpecification(String specification, int ordinal) {
        String[] parts = specification.trim().split("\\s+");
        if (parts.length < 2) {
            throw new IllegalArgumentException(
                    "A guard needs a type and at least one tile: '" + specification + "'");
        }

        String type = parts[0].toLowerCase();

        // A sentinel has a post and a way to face rather than a route, so the
        // last word may be a compass direction instead of a tile.
        Direction facing = null;
        int lastTile = parts.length - 1;
        if (!parts[lastTile].contains(",")) {
            facing = parseFacing(parts[lastTile], specification);
            lastTile--;
        }

        List<Position> route = new ArrayList<>();
        for (int i = 1; i <= lastTile; i++) {
            route.add(parseTile(parts[i], specification));
        }
        if (route.isEmpty()) {
            throw new IllegalArgumentException(
                    "A guard needs at least one tile: '" + specification + "'");
        }
        Position spawn = route.get(0);

        return switch (type) {
            case "patroller" -> new Patroller("Unit " + ordinal, spawn, route);
            case "hunter" -> new Hunter("Unit " + ordinal, spawn, route);
            case "sentinel" -> new Sentinel("Unit " + ordinal, spawn,
                    facing == null ? Direction.UP : facing);
            default -> throw new IllegalArgumentException("Unknown guard type: '" + type + "'");
        };
    }

    private static Direction parseFacing(String text, String specification) {
        return switch (text.toLowerCase()) {
            case "up", "north" -> Direction.UP;
            case "down", "south" -> Direction.DOWN;
            case "left", "west" -> Direction.LEFT;
            case "right", "east" -> Direction.RIGHT;
            default -> throw new IllegalArgumentException(
                    "'" + text + "' is not a tile or a facing (up, down, left, right) in: "
                            + specification);
        };
    }

    private static Position parseTile(String text, String specification) {
        String[] coordinates = text.split(",");
        if (coordinates.length != 2) {
            throw new IllegalArgumentException(
                    "Expected a tile as row,col but found '" + text + "' in: " + specification);
        }
        try {
            return new Position(
                    Integer.parseInt(coordinates[0].trim()),
                    Integer.parseInt(coordinates[1].trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Tile '" + text + "' is not a pair of numbers in: " + specification, e);
        }
    }
}
