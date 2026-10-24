package com.signallost.pathfinding;

import com.signallost.model.GameMap;
import com.signallost.model.Position;
import com.signallost.model.TileType;

import java.util.ArrayList;
import java.util.List;

/**
 * How long a level takes to walk, ignoring the guards.
 *
 * <p>This is the number a level's par time and its turn limit are both built
 * from, so both come from the level's own geometry rather than from a figure
 * somebody typed in and forgot to update when the walls moved.
 */
public final class RouteMetrics {

    /** Returned when the level cannot be finished at all. */
    public static final int UNREACHABLE = -1;

    private RouteMetrics() {
    }

    /**
     * Steps to collect every key and reach the exit, taking the nearest key each
     * time and ignoring guards entirely.
     *
     * <p>Nearest-first is a greedy tour rather than a true shortest one, so it
     * can overestimate slightly. That is the right direction to be wrong in:
     * everything built on it gives the player a little more room, not less.
     *
     * @return the step count, or {@link #UNREACHABLE} when some key or the exit
     *         cannot be reached
     */
    public static int shortestClearRoute(GameMap map) {
        Position at = map.findFirst(TileType.START);
        Position exit = map.findFirst(TileType.EXIT);
        if (at == null || exit == null) {
            return UNREACHABLE;
        }

        List<Position> remaining = new ArrayList<>(map.findAll(TileType.KEY));
        int steps = 0;

        while (!remaining.isEmpty()) {
            Position nearest = null;
            int best = Integer.MAX_VALUE;
            for (Position key : remaining) {
                int distance = stepsBetween(map, at, key);
                if (distance != UNREACHABLE && distance < best) {
                    best = distance;
                    nearest = key;
                }
            }
            if (nearest == null) {
                return UNREACHABLE;
            }
            steps += best;
            at = nearest;
            remaining.remove(nearest);
        }

        int toExit = stepsBetween(map, at, exit);
        return toExit == UNREACHABLE ? UNREACHABLE : steps + toExit;
    }

    /** Steps from one tile to another, or {@link #UNREACHABLE}. */
    public static int stepsBetween(GameMap map, Position from, Position to) {
        List<Position> path = AStarPathfinder.findPath(map, from, to);
        return path.isEmpty() ? UNREACHABLE : path.size() - 1;
    }
}
