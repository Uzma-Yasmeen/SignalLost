package com.signallost;

import com.signallost.level.Level;
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

/**
 * Tests for an exit that wants more than one key.
 *
 * <p>One key made a level a straight line: find it, walk out, and after two
 * attempts you know the route. Several keys force the player to visit several
 * places, which is what makes the map worth exploring and the guards worth
 * avoiding on the way back.
 */
class MultipleKeysTest {

    /** Three keys down one corridor, with the exit at the far end. */
    private static GameMap corridor() {
        return LevelLoader.parse(List.of(
                "##########",
                "#@KK.K..E#",
                "##########"));
    }

    @Test
    @DisplayName("The level wants as many keys as it has on the floor")
    void keysRequiredIsCountedFromTheMap() {
        GameState state = new GameState(corridor(), 6);

        assertEquals(3, state.keysRequired());
        assertEquals(0, state.keysHeld());
        assertFalse(state.hasAllKeys());
    }

    @Test
    @DisplayName("The exit stays shut while any key is still out there")
    void exitNeedsEveryKey() {
        GameState state = new GameState(corridor(), 6);

        // Two of the three, then straight on to the exit.
        for (int i = 0; i < 7; i++) {
            state.movePlayer(Direction.RIGHT);
        }

        assertEquals(3, state.keysHeld(), "the route passes over all three");
        assertTrue(state.hasEscaped());
    }

    @Test
    @DisplayName("Reaching the exit holding only some keys does not finish the level")
    void partialKeysDoNotOpenTheExit() {
        GameMap map = LevelLoader.parse(List.of(
                "######",
                "#@K.E#",
                "#..K.#",
                "######"));
        GameState state = new GameState(map, 6);

        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.RIGHT);

        assertEquals(new Position(1, 4), state.player());
        assertEquals(1, state.keysHeld());
        assertFalse(state.hasAllKeys());
        assertFalse(state.hasEscaped(), "one of two keys is not enough");
    }

    @Test
    @DisplayName("Undo hands a key back and shuts the exit again")
    void undoReturnsAKey() {
        GameState state = new GameState(corridor(), 6);

        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.RIGHT);
        assertEquals(2, state.keysHeld());

        state.undoLastMove();

        assertEquals(1, state.keysHeld());
        assertEquals(TileType.KEY, state.map().tileAt(new Position(1, 3)));
        assertFalse(state.hasAllKeys());
    }

    @Test
    @DisplayName("The hint points at the nearest key still on the floor")
    void hintChasesTheNearestKey() {
        GameState state = new GameState(corridor(), 6);

        List<Position> first = state.hintRoute(20);
        assertEquals(new Position(1, 2), first.get(first.size() - 1));

        state.movePlayer(Direction.RIGHT);

        List<Position> second = state.hintRoute(20);
        assertEquals(new Position(1, 3), second.get(second.size() - 1),
                "it should move on to the next one, not back to the first");
    }

    @Test
    @DisplayName("Level 1 ships with three keys")
    void levelOneHasThreeKeys() {
        Level level = LevelLoader.loadFromResource("level1");

        assertEquals(3, level.map().count(TileType.KEY));
    }
}
