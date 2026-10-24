package com.signallost;

import com.signallost.guard.Patroller;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameMap;
import com.signallost.model.GameState;
import com.signallost.model.Position;
import com.signallost.model.TileType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the suggested route. */
class HintTest {

    /** Key to the east along the top, exit in the far corner. */
    private static GameMap room() {
        return LevelLoader.parse(List.of(
                "########",
                "#@....K#",
                "#......#",
                "#......#",
                "#.....E#",
                "########"));
    }

    @Test
    @DisplayName("The hint starts on a tile next to the player")
    void hintStartsAdjacent() {
        GameState state = new GameState(room(), 4);

        List<Position> hint = state.hintRoute(5);

        assertFalse(hint.isEmpty());
        assertEquals(1, hint.get(0).manhattanDistanceTo(state.player()));
    }

    @Test
    @DisplayName("The hint points at the key first, then at the exit")
    void hintFollowsTheObjective() {
        GameState state = new GameState(room(), 4);
        Position key = new Position(1, 6);
        Position exit = new Position(4, 6);

        List<Position> towardsKey = state.hintRoute(20);
        assertEquals(key, towardsKey.get(towardsKey.size() - 1),
                "without the key, the route should end on it");

        // Walk the whole suggested route to pick the key up.
        for (int i = 0; i < 5; i++) {
            state.movePlayer(Direction.RIGHT);
        }
        assertTrue(state.hasAllKeys());

        List<Position> towardsExit = state.hintRoute(20);
        assertEquals(exit, towardsExit.get(towardsExit.size() - 1),
                "with the key, the route should end on the exit");
    }

    @Test
    @DisplayName("The hint only gives away the number of steps asked for")
    void hintIsLimitedInLength() {
        GameState state = new GameState(room(), 4);

        assertEquals(3, state.hintRoute(3).size());
        assertTrue(state.hintRoute(0).isEmpty());
    }

    @Test
    @DisplayName("The hint routes around a tile a guard is watching")
    void hintAvoidsGuardSight() {
        // A patroller parked on the direct route along the top row.
        Patroller guard = new Patroller("P", new Position(1, 3), List.of(new Position(1, 3)));
        GameState state = new GameState(room(), List.of(guard), 4);

        List<Position> hint = state.hintRoute(20);

        assertFalse(hint.contains(guard.position()),
                "the suggestion should not walk into the guard");
        assertTrue(hint.stream().anyMatch(tile -> tile.row() > 1),
                "it should drop out of the watched top row to get past");
    }

    @Test
    @DisplayName("There is no hint once the level is finished")
    void finishedLevelHasNoHint() {
        GameMap map = LevelLoader.parse(List.of(
                "#####",
                "#@KE#",
                "#####"));
        GameState state = new GameState(map, 3);

        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.RIGHT);
        assertTrue(state.hasEscaped());

        assertTrue(state.hintRoute(5).isEmpty());
    }

    @Test
    @DisplayName("Every tile the hint suggests can actually be walked on")
    void hintStaysOnWalkableTiles() {
        GameMap map = LevelLoader.parse(List.of(
                "#########",
                "#@..#...#",
                "#.#.#.#.#",
                "#.#...#K#",
                "#.#####.#",
                "#......E#",
                "#########"));
        GameState state = new GameState(map, 4);

        List<Position> hint = state.hintRoute(30);

        assertFalse(hint.isEmpty());
        hint.forEach(tile -> assertTrue(map.isWalkable(tile), tile + " is not walkable"));
        assertEquals(TileType.KEY, map.tileAt(hint.get(hint.size() - 1)));
    }
}
