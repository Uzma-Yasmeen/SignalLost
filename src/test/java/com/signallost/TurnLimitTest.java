package com.signallost;

import com.signallost.level.Level;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameMap;
import com.signallost.model.GameState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the clock that ends a run. */
class TurnLimitTest {

    private static GameMap hall() {
        return LevelLoader.parse(List.of(
                "########",
                "#@....E#",
                "########"));
    }

    private static GameState timed(int limit) {
        return new GameState(hall(), List.of(), 4, limit);
    }

    @Test
    @DisplayName("A level with no limit is untimed and never runs out")
    void noLimitMeansNoClock() {
        GameState state = new GameState(hall(), 4);

        assertFalse(state.hasTurnLimit());
        assertEquals(-1, state.turnsRemaining());

        for (int i = 0; i < 50; i++) {
            state.movePlayer(Direction.WAIT);
        }
        assertFalse(state.hasFailed());
    }

    @Test
    @DisplayName("Turns remaining counts down as the player moves")
    void remainingTurnsCountDown() {
        GameState state = timed(5);

        assertEquals(5, state.turnsRemaining());
        state.movePlayer(Direction.RIGHT);
        assertEquals(4, state.turnsRemaining());
    }

    @Test
    @DisplayName("Using the last turn without escaping fails the run")
    void runningOutOfTurnsFails() {
        GameState state = timed(3);

        state.movePlayer(Direction.RIGHT);
        assertFalse(state.hasFailed());
        state.movePlayer(Direction.RIGHT);
        assertFalse(state.hasFailed());
        state.movePlayer(Direction.RIGHT);

        assertTrue(state.hasFailed());
        assertTrue(state.isAttemptOver(), "this attempt is over");
        assertFalse(state.isFinished(), "but the run is not: there are lives left");
        assertEquals(0, state.turnsRemaining());
    }

    @Test
    @DisplayName("A failed run accepts no further moves")
    void failedRunIsOver() {
        GameState state = timed(1);

        state.movePlayer(Direction.RIGHT);
        assertTrue(state.hasFailed());

        assertFalse(state.movePlayer(Direction.RIGHT));
        assertEquals(1, state.turnsTaken());
    }

    @Test
    @DisplayName("Running the clock out is final: undo cannot take it back")
    void undoCannotReopenAFailedRun() {
        GameState state = timed(1);

        state.movePlayer(Direction.RIGHT);
        assertTrue(state.hasFailed());

        assertFalse(state.canUndo(), "an attempt that ended is not a mistake to rewind");
        assertFalse(state.undoLastMove());
        assertTrue(state.hasFailed());
    }

    @Test
    @DisplayName("Spending a life gives the clock back")
    void anotherLifeResetsTheClock() {
        GameState state = timed(2);

        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.RIGHT);
        assertTrue(state.hasFailed());

        assertTrue(state.beginNextLife());

        assertFalse(state.hasFailed());
        assertEquals(0, state.turnsTaken(), "a life is a clean attempt");
        assertEquals(2, state.turnsRemaining());
    }

    @Test
    @DisplayName("Escaping on the very last turn is a win, not a failure")
    void escapingOnTheFinalTurnWins() {
        GameMap map = LevelLoader.parse(List.of(
                "#####",
                "#@KE#",
                "#####"));
        GameState state = new GameState(map, List.of(), 4, 2);

        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.RIGHT);

        assertTrue(state.hasEscaped());
        assertFalse(state.hasFailed());
    }

    // ------------------------------------------------------- the level files

    @Test
    @DisplayName("A turns line in the level file sets the limit")
    void levelFileSetsTheLimit() {
        Level level = LevelLoader.parseLevel("timed", List.of(
                "#####",
                "#@.E#",
                "#####",
                "---",
                "turns 40",
                "patroller 1,1 1,3"));

        assertEquals(40, level.turnLimit());
        assertEquals(1, level.guards().size(), "the turns line is not a guard");
    }

    @Test
    @DisplayName("A level file with no turns line is untimed")
    void levelFileWithoutALimitIsUntimed() {
        Level level = LevelLoader.parseLevel("untimed", List.of(
                "#####",
                "#@.E#",
                "#####"));

        assertEquals(GameState.NO_TURN_LIMIT, level.turnLimit());
    }

    @Test
    @DisplayName("A malformed turns line is rejected when the level loads")
    void malformedLimitIsRejected() {
        List<String> notANumber = List.of("#####", "#@.E#", "#####", "---", "turns soon");
        List<String> notPositive = List.of("#####", "#@.E#", "#####", "---", "turns 0");

        assertThrows(IllegalArgumentException.class,
                () -> LevelLoader.parseLevel("bad", notANumber));
        assertThrows(IllegalArgumentException.class,
                () -> LevelLoader.parseLevel("bad", notPositive));
    }

}
