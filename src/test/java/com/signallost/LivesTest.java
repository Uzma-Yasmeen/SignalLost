package com.signallost;

import com.signallost.guard.Hunter;
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
 * Tests for the three attempts a run gets.
 *
 * <p>A life is a clean attempt rather than a wounded continuation: the player
 * goes back to the start, the keys go back on the floor, the guards go back to
 * their posts and the clock goes back to zero. Sightings are the exception,
 * because they are the run's score rather than the attempt's.
 */
class LivesTest {

    /** A key on the way, an exit at the end, and a hunter that will reach you. */
    private static GameMap hall() {
        return LevelLoader.parse(List.of(
                "##########",
                "#@K.....E#",
                "##########"));
    }

    private static GameState caughtOnce() {
        Hunter hunter = new Hunter("H", new Position(1, 5), List.of(new Position(1, 5)));
        GameState state = new GameState(hall(), List.of(hunter), 4);
        while (!state.isCaught() && state.turnsTaken() < 20) {
            state.movePlayer(Direction.WAIT);
        }
        return state;
    }

    @Test
    @DisplayName("A run starts with three lives")
    void threeLivesToStart() {
        GameState state = new GameState(hall(), 4);

        assertEquals(3, state.livesLeft());
        assertFalse(state.isRunOver());
    }

    @Test
    @DisplayName("Claiming the next life spends one and starts the level again")
    void nextLifeResetsTheAttempt() {
        GameState state = caughtOnce();
        assertTrue(state.isCaught());

        assertTrue(state.beginNextLife());

        assertEquals(2, state.livesLeft());
        assertFalse(state.isCaught());
        assertFalse(state.isAttemptOver());
        assertEquals(0, state.turnsTaken(), "the clock starts again");
    }

    @Test
    @DisplayName("A new life puts the keys back on the floor and the player at the start")
    void nextLifeRestoresTheLevel() {
        Hunter hunter = new Hunter("H", new Position(1, 6), List.of(new Position(1, 6)));
        GameState state = new GameState(hall(), List.of(hunter), 4);
        Position start = state.player();

        state.movePlayer(Direction.RIGHT);
        assertEquals(1, state.keysHeld(), "picked the key up");

        while (!state.isCaught() && state.turnsTaken() < 20) {
            state.movePlayer(Direction.WAIT);
        }
        assertTrue(state.isCaught());
        state.beginNextLife();

        assertEquals(start, state.player());
        assertEquals(0, state.keysHeld(), "the key goes back where it was");
        assertEquals(TileType.KEY, state.map().tileAt(new Position(1, 2)));
        assertEquals(new Position(1, 6), hunter.position(), "the guard goes back to its post");
    }

    @Test
    @DisplayName("Sightings carry across lives: they score the run, not the attempt")
    void sightingsSurviveALife() {
        GameState state = caughtOnce();
        int seen = state.sightings();
        assertTrue(seen > 0);

        state.beginNextLife();

        assertEquals(seen, state.sightings());
    }

    @Test
    @DisplayName("Undo cannot reach back past a life")
    void undoDoesNotCrossLives() {
        GameState state = caughtOnce();
        state.beginNextLife();

        assertFalse(state.canUndo(), "the previous attempt is gone, not rewindable");
    }

    @Test
    @DisplayName("The third capture ends the run for good")
    void thirdCaptureEndsTheRun() {
        GameState state = caughtOnce();

        assertTrue(state.beginNextLife(), "second attempt");
        runUntilCaught(state);
        assertTrue(state.beginNextLife(), "third attempt");
        runUntilCaught(state);

        assertFalse(state.beginNextLife(), "no fourth attempt");
        assertTrue(state.isRunOver());
        assertTrue(state.isFinished());
        assertEquals(0, state.livesLeft());
        assertFalse(state.movePlayer(Direction.WAIT), "the run is over");
    }

    private static void runUntilCaught(GameState state) {
        while (!state.isCaught() && state.turnsTaken() < 20) {
            state.movePlayer(Direction.WAIT);
        }
        assertTrue(state.isCaught(), "the hunter should have reached the player again");
    }
}
