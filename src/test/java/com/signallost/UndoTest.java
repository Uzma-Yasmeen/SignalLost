package com.signallost;

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

/** Tests for the Command stack behind undo. */
class UndoTest {

    private static GameMap tinyLevel() {
        return LevelLoader.parse(List.of(
                "#####",
                "#@KE#",
                "#####"));
    }

    @Test
    @DisplayName("There is nothing to undo before the first move")
    void undoIsEmptyAtTheStart() {
        GameState state = new GameState(tinyLevel(), 3);

        assertFalse(state.canUndo());
        assertFalse(state.undoLastMove());
        assertEquals(0, state.undoDepth());
    }

    @Test
    @DisplayName("Undo restores the position and gives the turn back")
    void undoRestoresPositionAndTurn() {
        GameState state = new GameState(tinyLevel(), 3);
        Position start = state.player();

        state.movePlayer(Direction.RIGHT);
        assertTrue(state.undoLastMove());

        assertEquals(start, state.player());
        assertEquals(0, state.turnsTaken());
        assertFalse(state.canUndo());
    }

    @Test
    @DisplayName("Undoing the move that took the key puts the key back on the map")
    void undoReturnsTheKeyToTheMap() {
        GameState state = new GameState(tinyLevel(), 3);
        Position keyTile = new Position(1, 2);

        state.movePlayer(Direction.RIGHT);
        assertTrue(state.hasAllKeys());
        assertEquals(TileType.FLOOR, state.map().tileAt(keyTile));

        state.undoLastMove();

        assertFalse(state.hasAllKeys());
        assertEquals(TileType.KEY, state.map().tileAt(keyTile));
    }

    @Test
    @DisplayName("Undo unwinds several moves one at a time, newest first")
    void undoUnwindsInReverseOrder() {
        GameMap map = LevelLoader.parse(List.of(
                "######",
                "#@..E#",
                "######"));
        GameState state = new GameState(map, 3);

        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.RIGHT);
        assertEquals(new Position(1, 4), state.player());
        assertEquals(3, state.undoDepth());

        state.undoLastMove();
        assertEquals(new Position(1, 3), state.player());

        state.undoLastMove();
        assertEquals(new Position(1, 2), state.player());

        state.undoLastMove();
        assertEquals(new Position(1, 1), state.player());
        assertEquals(0, state.turnsTaken());
    }

    @Test
    @DisplayName("A refused move leaves nothing on the undo stack")
    void blockedMovesAreNotRecorded() {
        GameState state = new GameState(tinyLevel(), 3);

        assertFalse(state.movePlayer(Direction.UP));

        assertFalse(state.canUndo());
    }

    @Test
    @DisplayName("Undo takes back a completed escape")
    void undoReopensAFinishedLevel() {
        GameState state = new GameState(tinyLevel(), 3);

        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.RIGHT);
        assertTrue(state.hasEscaped());

        state.undoLastMove();

        assertFalse(state.hasEscaped());
        assertEquals(new Position(1, 2), state.player());
    }

    @Test
    @DisplayName("Undo gives back the step but not the knowledge of the map")
    void exploredTilesSurviveAnUndo() {
        GameMap map = LevelLoader.parse(List.of(
                "########",
                "#@....E#",
                "########"));
        GameState state = new GameState(map, 2);

        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.RIGHT);
        int seenAfterWalking = state.fog().exploredTiles().size();

        state.undoLastMove();
        state.undoLastMove();

        assertEquals(seenAfterWalking, state.fog().exploredTiles().size(),
                "walking back does not unsee what was seen");
    }

    @Test
    @DisplayName("The stack reports what undo would take back")
    void undoDescribesTheTopOfTheStack() {
        GameState state = new GameState(tinyLevel(), 3);

        state.movePlayer(Direction.WAIT);
        assertEquals("wait", state.nextUndoDescription());

        state.movePlayer(Direction.RIGHT);
        assertEquals("move right", state.nextUndoDescription());
    }
}
