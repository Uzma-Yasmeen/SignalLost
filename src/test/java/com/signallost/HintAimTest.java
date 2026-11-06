package com.signallost;

import com.signallost.level.Level;
import com.signallost.level.LevelCatalog;
import com.signallost.level.LevelLoader;
import com.signallost.model.GameMap;
import com.signallost.model.GameState;
import com.signallost.model.Position;
import com.signallost.model.TileType;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a hint points at a key the player can actually reach soon.
 *
 * <p>The hint used to choose its key by straight-line distance, which knows
 * nothing about walls. In a building made of walls that is a guess, and a
 * sweep of every standable tile found it guessing wrong on nine of the ten
 * levels — aiming at a key 27 steps further off than the near one on level 5,
 * and 56 on level 10. Spending one of only three hints to be sent into a dead
 * end is worse than having no hint at all, and level 5 is the level players
 * reported being unable to finish.
 */
class HintAimTest {

    /**
     * A wall between the player and a key that is nearer as the crow flies.
     *
     * <p>The near key is two tiles east behind a wall with no way through on
     * this row; reaching it means walking the whole way round. The far key is
     * straight down the open corridor. Straight-line distance prefers the first
     * and is wrong.
     */
    private static final String[] WALLED_OFF = {
        "#########",
        "#@.#K...#",
        "#..#....#",
        "#..#....#",
        "#K.#....#",
        "#...#####",
        "#......E#",
        "#########",
    };

    @Test
    void aWallMakesTheNearerKeyTheFurtherOne() {
        GameState state = stateOf(WALLED_OFF);
        List<Position> route = state.hintRoute(40);

        assertTrue(!route.isEmpty(), "the hint gave up on a level with two reachable keys");

        Position aimedAt = route.get(route.size() - 1);
        assertEquals(new Position(4, 1), aimedAt,
                "the hint aimed at " + aimedAt + ". The key at (1,4) is nearer in a straight"
                        + " line but walled off; the one at (4,1) is three steps down an open"
                        + " corridor.");
    }

    @Test
    void everyLevelAimsSensiblyFromWhereThePlayerStarts() {
        List<String> complaints = new ArrayList<>();

        for (LevelCatalog.Entry entry : LevelCatalog.load()) {
            Level level = LevelLoader.loadFromResource(entry.name());
            GameMap map = level.map();
            if (map.findAll(TileType.KEY).size() < 2) {
                continue;
            }

            // No guards: the only thing steering the hint is the geometry.
            GameState state = new GameState(map, List.of(), 5, level.turnLimit());
            List<Position> route = state.hintRoute(60);
            if (route.isEmpty()) {
                complaints.add(entry.name() + ": the hint has nothing to say on turn one");
                continue;
            }

            Map<Position, Integer> steps = stepsFrom(map, state.player());
            int toAimed = steps.getOrDefault(route.get(route.size() - 1), 0);
            int toNearest = nearestKeySteps(map, steps);
            if (toAimed - toNearest > ALLOWED_DETOUR) {
                complaints.add(entry.name() + ": the hint heads " + (toAimed - toNearest)
                        + " steps further than the nearest reachable key");
            }
        }

        assertTrue(complaints.isEmpty(),
                "The hint is aiming at keys the player cannot reach soon:\n  "
                        + String.join("\n  ", complaints));
    }

    /**
     * Extra steps the hint may accept to stay out of sight.
     *
     * <p>Generous, because this is not here to police route quality. It is here
     * to catch the hint aiming at the wrong side of the building, which showed
     * up as detours of 27 and 56 steps rather than three or four.
     */
    private static final int ALLOWED_DETOUR = 8;

    private static GameState stateOf(String[] rows) {
        Level level = LevelLoader.parseLevel("walled-off", List.of(rows));
        return new GameState(level.map(), List.of(), 5, level.turnLimit());
    }

    private static int nearestKeySteps(GameMap map, Map<Position, Integer> steps) {
        int best = Integer.MAX_VALUE;
        for (Position key : map.findAll(TileType.KEY)) {
            Integer distance = steps.get(key);
            if (distance != null && distance < best) {
                best = distance;
            }
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }

    /** Step counts to every reachable tile, which is what "near" has to mean. */
    private static Map<Position, Integer> stepsFrom(GameMap map, Position start) {
        Map<Position, Integer> steps = new HashMap<>();
        steps.put(start, 0);
        Deque<Position> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            Position at = queue.removeFirst();
            for (Position next : map.walkableNeighbours(at)) {
                if (!steps.containsKey(next)) {
                    steps.put(next, steps.get(at) + 1);
                    queue.addLast(next);
                }
            }
        }
        return steps;
    }
}
