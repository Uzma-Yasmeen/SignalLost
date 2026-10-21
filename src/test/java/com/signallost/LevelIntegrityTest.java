package com.signallost;

import com.signallost.level.Level;
import com.signallost.level.LevelLoader;
import com.signallost.model.GameMap;
import com.signallost.model.Position;
import com.signallost.model.TileType;
import com.signallost.pathfinding.AStarPathfinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the shipped levels can actually be finished.
 *
 * <p>A level is hand-drawn text, so one misplaced {@code #} can wall the key off
 * and make the game quietly impossible — the sort of bug that is invisible until
 * somebody plays for ten minutes and gives up. A* already knows how to answer
 * "can you get there from here", so the same code the guards chase with checks
 * the level at build time.
 */
class LevelIntegrityTest {

    @Test
    @DisplayName("Level 1 can be finished: start to key to exit")
    void levelOneIsCompletable() {
        Level level = LevelLoader.loadFromResource("level1");
        GameMap map = level.map();

        Position start = map.findFirst(TileType.START);
        Position key = map.findFirst(TileType.KEY);
        Position exit = map.findFirst(TileType.EXIT);

        assertNotNull(start, "no start tile");
        assertNotNull(key, "no key tile");
        assertNotNull(exit, "no exit tile");

        assertFalse(AStarPathfinder.findPath(map, start, key).isEmpty(),
                "the key cannot be reached from the start");
        assertFalse(AStarPathfinder.findPath(map, key, exit).isEmpty(),
                "the exit cannot be reached from the key");
    }

    @Test
    @DisplayName("Every guard in level 1 can walk its whole route")
    void everyGuardRouteIsWalkable() {
        Level level = LevelLoader.loadFromResource("level1");
        GameMap map = level.map();

        level.guards().forEach(guard -> {
            var route = guard.route();
            for (int i = 0; i < route.size(); i++) {
                Position from = route.get(i);
                Position to = route.get((i + 1) % route.size());
                assertTrue(from.equals(to) || !AStarPathfinder.findPath(map, from, to).isEmpty(),
                        guard.name() + " cannot get from " + from + " to " + to);
            }
        });
    }
}
