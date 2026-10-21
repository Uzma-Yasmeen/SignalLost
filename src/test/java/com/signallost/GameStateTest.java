package com.signallost;

import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameMap;
import com.signallost.model.GameState;
import com.signallost.model.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for player movement, the key and the exit. */
class GameStateTest {

    /** A tiny level: start, key and exit in one short corridor. */
    private static GameMap tinyLevel() {
        return LevelLoader.parse(List.of(
                "#####",
                "#@KE#",
                "#####"));
    }

    @Test
    @DisplayName("Walking into a wall is refused and costs no turn")
    void wallBlocksMovement() {
        GameState state = new GameState(tinyLevel(), 3);
        Position before = state.player();

        boolean moved = state.movePlayer(Direction.UP);

        assertFalse(moved);
        assertEquals(before, state.player());
        assertEquals(0, state.turnsTaken());
    }

    @Test
    @DisplayName("A legal move costs exactly one turn")
    void legalMoveCostsOneTurn() {
        GameState state = new GameState(tinyLevel(), 3);

        assertTrue(state.movePlayer(Direction.RIGHT));

        assertEquals(new Position(1, 2), state.player());
        assertEquals(1, state.turnsTaken());
    }

    @Test
    @DisplayName("Waiting costs a turn without moving")
    void waitingCostsATurn() {
        GameState state = new GameState(tinyLevel(), 3);
        Position before = state.player();

        assertTrue(state.movePlayer(Direction.WAIT));

        assertEquals(before, state.player());
        assertEquals(1, state.turnsTaken());
    }

    @Test
    @DisplayName("Reaching the exit without the key does not finish the level")
    void exitStaysLockedWithoutTheKey() {
        // Start two steps from the exit, with the key off to one side.
        GameMap map = LevelLoader.parse(List.of(
                "#####",
                "#@.E#",
                "#.K.#",
                "#####"));
        GameState state = new GameState(map, 3);

        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.RIGHT);

        assertEquals(state.exit(), state.player());
        assertFalse(state.hasKey());
        assertFalse(state.hasEscaped());
    }

    @Test
    @DisplayName("Key then exit completes the level")
    void keyThenExitEscapes() {
        GameState state = new GameState(tinyLevel(), 3);

        state.movePlayer(Direction.RIGHT);
        assertTrue(state.hasKey(), "stepping on the key picks it up");

        state.movePlayer(Direction.RIGHT);

        assertTrue(state.hasEscaped());
        assertEquals(2, state.turnsTaken());
    }
}
