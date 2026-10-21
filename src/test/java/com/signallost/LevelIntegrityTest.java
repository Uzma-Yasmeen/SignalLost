package com.signallost;

import com.signallost.level.Level;
import com.signallost.level.LevelLoader;
import com.signallost.model.GameMap;
import com.signallost.model.Position;
import com.signallost.model.TileType;
import com.signallost.pathfinding.AStarPathfinder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that every shipped level can actually be finished.
 *
 * <p>A level is hand-drawn text, so one misplaced {@code #} can wall the key off
 * and make the game quietly impossible — the sort of bug that is invisible until
 * somebody plays for ten minutes and gives up. It happened: level 2 shipped with
 * its key sealed in a room that had no door, and this is the test that found it.
 *
 * <p>A* already knows how to answer "can you get there from here", so the same
 * code the guards chase with checks the levels at build time. Every level is run
 * through the same checks, so a new level file is validated the moment it is
 * added rather than when a player hits the wall.
 */
class LevelIntegrityTest {

    /** Every level the game ships. Add new ones here. */
    private static final String[] SHIPPED_LEVELS = {"level1", "level2"};

    @ParameterizedTest(name = "{0} can be finished: start to key to exit")
    @ValueSource(strings = {"level1", "level2"})
    void levelIsCompletable(String name) {
        Level level = LevelLoader.loadFromResource(name);
        GameMap map = level.map();

        Position start = map.findFirst(TileType.START);
        Position key = map.findFirst(TileType.KEY);
        Position exit = map.findFirst(TileType.EXIT);

        assertNotNull(start, name + " has no start tile");
        assertNotNull(key, name + " has no key tile");
        assertNotNull(exit, name + " has no exit tile");

        assertFalse(AStarPathfinder.findPath(map, start, key).isEmpty(),
                name + ": the key cannot be reached from the start");
        assertFalse(AStarPathfinder.findPath(map, key, exit).isEmpty(),
                name + ": the exit cannot be reached from the key");
    }

    @ParameterizedTest(name = "{0}: every guard can walk its whole route")
    @ValueSource(strings = {"level1", "level2"})
    void everyGuardRouteIsWalkable(String name) {
        Level level = LevelLoader.loadFromResource(name);
        GameMap map = level.map();

        level.guards().forEach(guard -> {
            var route = guard.route();
            for (int i = 0; i < route.size(); i++) {
                Position from = route.get(i);
                Position to = route.get((i + 1) % route.size());
                assertTrue(from.equals(to) || !AStarPathfinder.findPath(map, from, to).isEmpty(),
                        name + ": " + guard.name() + " cannot get from " + from + " to " + to);
            }
        });
    }

    @ParameterizedTest(name = "{0}: the turn limit leaves room to actually play")
    @ValueSource(strings = {"level1", "level2"})
    void turnLimitIsWorkable(String name) {
        Level level = LevelLoader.loadFromResource(name);

        assertTrue(level.turnLimit() > 0, name + " should be timed");

        int optimal = optimalRoute(level);
        assertTrue(level.turnLimit() >= optimal * 1.4,
                name + ": a player who cannot see the map needs slack. The optimal route is "
                        + optimal + " turns but the limit is only " + level.turnLimit());
    }

    @ParameterizedTest(name = "{0}: has a guard the player will actually meet")
    @ValueSource(strings = {"level1", "level2"})
    void levelHasGuards(String name) {
        Level level = LevelLoader.loadFromResource(name);

        assertFalse(level.guards().isEmpty(), name + " has nobody in it");
    }

    /** Start to key to exit, walked optimally, using the guards' own pathfinder. */
    static int optimalRoute(Level level) {
        GameMap map = level.map();
        Position start = map.findFirst(TileType.START);
        Position key = map.findFirst(TileType.KEY);
        Position exit = map.findFirst(TileType.EXIT);

        return AStarPathfinder.findPath(map, start, key).size() - 1
                + AStarPathfinder.findPath(map, key, exit).size() - 1;
    }

    static String[] shippedLevels() {
        return SHIPPED_LEVELS;
    }
}
