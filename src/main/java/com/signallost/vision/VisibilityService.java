package com.signallost.vision;

import com.signallost.model.GameMap;
import com.signallost.model.Position;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Works out which tiles can be seen from a given tile, using a breadth-first
 * flood fill that walls stop.
 *
 * <p>The rule is deliberately simple so it can be reasoned about and tested:
 * <ol>
 *   <li>Start at the viewer's tile at distance 0.</li>
 *   <li>Expand to orthogonal neighbours while the distance is under the radius.</li>
 *   <li>A wall is <em>seen</em> when the fill reaches it, because you can see the
 *       face of a wall you are standing next to, but the fill does not continue
 *       through it, so the wall hides everything behind it.</li>
 * </ol>
 *
 * <p>Because the fill spreads along walkable tiles rather than in straight lines,
 * sight bends around corners the way a corridor does, which is what makes the
 * fog feel like a facility rather than a spotlight.
 */
public final class VisibilityService {

    private VisibilityService() {
    }

    /**
     * The tiles visible from {@code origin} within {@code radius} steps.
     * The origin itself is always included.
     *
     * @param radius how many steps light travels; 0 means only the origin tile
     */
    public static Set<Position> visibleFrom(GameMap map, Position origin, int radius) {
        return sightDistancesFrom(map, origin, radius).keySet();
    }

    /**
     * The same flood fill, but keeping the step count at which each tile was
     * reached. The renderer uses those counts to fade light off with distance;
     * {@link #visibleFrom} is just the key set of this result.
     */
    public static Map<Position, Integer> sightDistancesFrom(GameMap map, Position origin, int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Sight radius cannot be negative: " + radius);
        }
        if (!map.contains(origin)) {
            return Collections.emptyMap();
        }

        Map<Position, Integer> distance = new HashMap<>();
        Queue<Position> frontier = new ArrayDeque<>();

        distance.put(origin, 0);
        frontier.add(origin);

        while (!frontier.isEmpty()) {
            Position current = frontier.remove();
            int currentDistance = distance.get(current);

            // Light has run out of range.
            if (currentDistance >= radius) {
                continue;
            }
            // A wall is visible but opaque: we stop rather than spread past it.
            if (map.blocksSight(current) && !current.equals(origin)) {
                continue;
            }

            for (Position neighbour : map.neighbours(current)) {
                if (distance.containsKey(neighbour)) {
                    continue;
                }
                distance.put(neighbour, currentDistance + 1);
                frontier.add(neighbour);
            }
        }
        return distance;
    }
}
