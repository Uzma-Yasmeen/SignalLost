package com.signallost;

import com.signallost.guard.Guard;
import com.signallost.guard.Hunter;
import com.signallost.level.Level;
import com.signallost.level.LevelLoader;
import com.signallost.model.GameMap;
import com.signallost.model.Position;
import com.signallost.pathfinding.AStarPathfinder;
import com.signallost.vision.VisibilityService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a level gives the player somewhere to hide.
 *
 * <p>A hunter surges, so it gains half a tile a turn. Spotted at the very edge
 * of its sight, the player has roughly {@code 2 × sightRadius} turns before it
 * reaches them. If the nearest tile out of that guard's view is further away
 * than that, the player is dead the moment they are seen, however well they
 * play — and no amount of skill or undo makes an impossible corridor possible.
 *
 * <p>This test is the reason the hunter surges rather than sprinting flat out.
 * At two tiles every turn it gained a whole tile a turn, and level 1 has spots
 * where cover is five steps away but capture would arrive in four. The measure
 * came first and the speed was set to fit it, rather than the other way round.
 */
class LevelFairnessTest {

    @ParameterizedTest(name = "{0}: there is always somewhere to hide")
    @ValueSource(strings = {"level1", "level2"})
    void coverIsAlwaysWithinReach(String name) {
        Level level = LevelLoader.loadFromResource(name);
        GameMap map = level.map();

        for (Guard guard : level.guards()) {
            // Only a hunter closes in, so only a hunter can corner anybody.
            if (!(guard instanceof Hunter)) {
                continue;
            }
            int allowed = turnsBeforeCapture(guard.sightRadius());

            for (Position post : patrolCircuit(map, guard)) {
                Set<Position> watched =
                        VisibilityService.visibleFrom(map, post, guard.sightRadius());

                for (Position tile : watched) {
                    if (!map.isWalkable(tile)) {
                        continue;
                    }
                    int toCover = stepsToCover(map, tile, watched);
                    assertTrue(toCover <= allowed,
                            name + ": standing on " + tile + " with the hunter at " + post
                                    + ", the nearest cover is " + toCover
                                    + " steps away but capture arrives in " + allowed
                                    + ". That tile is a death sentence, not a challenge.");
                }
            }
        }
    }

    /**
     * How long the player has, spotted at the edge of a guard's sight.
     *
     * <p>A surging hunter closes half a tile a turn, so the distance buys twice
     * its own value in turns.
     */
    private static int turnsBeforeCapture(int sightRadius) {
        return sightRadius * 2;
    }

    /** Every tile the guard walks over on one lap of its route. */
    private static List<Position> patrolCircuit(GameMap map, Guard guard) {
        List<Position> circuit = new ArrayList<>();
        List<Position> route = guard.route();
        for (int i = 0; i < route.size(); i++) {
            circuit.addAll(AStarPathfinder.findPath(
                    map, route.get(i), route.get((i + 1) % route.size())));
        }
        if (circuit.isEmpty()) {
            circuit.add(guard.position());
        }
        return circuit;
    }

    /** Steps from a tile to the nearest walkable one the guard cannot see. */
    private static int stepsToCover(GameMap map, Position from, Set<Position> watched) {
        Deque<Position> frontier = new ArrayDeque<>();
        Map<Position, Integer> distance = new HashMap<>();
        frontier.add(from);
        distance.put(from, 0);

        while (!frontier.isEmpty()) {
            Position current = frontier.remove();
            if (!watched.contains(current)) {
                return distance.get(current);
            }
            for (Position neighbour : map.walkableNeighbours(current)) {
                if (distance.containsKey(neighbour)) {
                    continue;
                }
                distance.put(neighbour, distance.get(current) + 1);
                frontier.add(neighbour);
            }
        }
        return Integer.MAX_VALUE;   // nowhere on the map is out of its view
    }
}
