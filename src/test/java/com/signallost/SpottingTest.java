package com.signallost;

import com.signallost.guard.Patroller;
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

/** Tests for ending a turn inside a guard's line of sight. */
class SpottingTest {

    /** The player starts out of sight; a sentry stands four tiles along. */
    private static GameMap corridor() {
        return LevelLoader.parse(List.of(
                "########",
                "#@....E#",
                "########"));
    }

    private static GameState withSentryAt(Position post) {
        Patroller sentry = new Patroller("P", post, List.of(post));
        return new GameState(corridor(), List.of(sentry), 4);
    }

    @Test
    @DisplayName("Nobody is spotted before the first move")
    void notSpottedAtTheStart() {
        GameState state = withSentryAt(new Position(1, 5));

        assertFalse(state.wasSpotted());
        assertEquals(0, state.sightings());
    }

    @Test
    @DisplayName("Ending a turn in a guard's sight counts as being spotted")
    void steppingIntoSightIsSpotted() {
        GameState state = withSentryAt(new Position(1, 5));

        // (1,2) is three steps from the sentry, inside its sight of 3.
        state.movePlayer(Direction.RIGHT);

        assertTrue(state.wasSpotted());
        assertEquals(1, state.sightings());
    }

    @Test
    @DisplayName("Staying out of sight is not spotted")
    void waitingOutOfSightIsSafe() {
        GameState state = withSentryAt(new Position(1, 5));

        // The start tile is four steps away, one further than the sentry can see.
        state.movePlayer(Direction.WAIT);

        assertFalse(state.wasSpotted());
        assertEquals(0, state.sightings());
    }

    @Test
    @DisplayName("A wall between you and a guard keeps you hidden")
    void wallsPreventSpotting() {
        GameMap map = LevelLoader.parse(List.of(
                "########",
                "#@.#..E#",
                "########"));
        Patroller sentry = new Patroller("P", new Position(1, 4), List.of(new Position(1, 4)));
        GameState state = new GameState(map, List.of(sentry), 4);

        state.movePlayer(Direction.RIGHT);

        assertFalse(state.wasSpotted(), "the wall at (1,3) blocks the sentry's sight");
    }

    @Test
    @DisplayName("Each turn spent in sight is counted separately")
    void sightingsAccumulate() {
        GameState state = withSentryAt(new Position(1, 5));

        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.WAIT);
        state.movePlayer(Direction.WAIT);

        assertEquals(3, state.sightings());
    }

    @Test
    @DisplayName("Being spotted does not end the run")
    void spottingIsNotFatal() {
        GameState state = withSentryAt(new Position(1, 5));

        state.movePlayer(Direction.RIGHT);
        assertTrue(state.wasSpotted());

        assertFalse(state.isFinished());
        assertTrue(state.movePlayer(Direction.RIGHT), "the player can keep going");
    }

    @Test
    @DisplayName("Undo takes back the sighting as well as the move")
    void undoTakesBackASighting() {
        GameState state = withSentryAt(new Position(1, 5));

        state.movePlayer(Direction.RIGHT);
        assertEquals(1, state.sightings());

        state.undoLastMove();

        assertFalse(state.wasSpotted());
        assertEquals(0, state.sightings());
    }

    @Test
    @DisplayName("Walking back out of sight clears the spotted flag but keeps the count")
    void leavingSightClearsTheFlagOnly() {
        GameState state = withSentryAt(new Position(1, 5));

        state.movePlayer(Direction.RIGHT);
        assertTrue(state.wasSpotted());

        state.movePlayer(Direction.LEFT);

        assertFalse(state.wasSpotted(), "no longer in sight this turn");
        assertEquals(1, state.sightings(), "but it still happened");
    }

    @Test
    @DisplayName("Reaching the exit while being watched still counts as escaping")
    void escapingUnderWatchStillWins() {
        // The sentry watches the corridor from the row below rather than
        // standing in it: walking onto a guard is being caught, not watched.
        GameMap map = LevelLoader.parse(List.of(
                "#######",
                "#@K..E#",
                "#.....#",
                "#######"));
        Patroller sentry = new Patroller("P", new Position(2, 3), List.of(new Position(2, 3)));
        GameState state = new GameState(map, List.of(sentry), 4);

        for (int i = 0; i < 4; i++) {
            state.movePlayer(Direction.RIGHT);
        }

        assertTrue(state.hasEscaped());
        assertFalse(state.wasSpotted(), "getting away wins outright");
    }
}
