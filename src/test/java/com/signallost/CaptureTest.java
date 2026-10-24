package com.signallost;

import com.signallost.guard.GuardMode;
import com.signallost.guard.Hunter;
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

/**
 * Tests for being caught, and for everything that is not being caught.
 *
 * <p>Only a guard that is actively hunting can end an attempt. Everyone else is
 * a moving wall. The earlier rule — any guard touching you is fatal — made a
 * one-tile corridor a coin toss rather than a decision, and a solver run proved
 * it left the level with no winning line at all.
 */
class CaptureTest {

    private static GameMap hall() {
        return LevelLoader.parse(List.of(
                "##########",
                "#@......E#",
                "##########"));
    }

    private static Hunter hunterAt(int col) {
        Position post = new Position(1, col);
        return new Hunter("H", post, List.of(post));
    }

    // --------------------------------------------------------- being caught

    @Test
    @DisplayName("A hunter that reaches the player ends the attempt")
    void beingReachedEndsTheAttempt() {
        Hunter hunter = hunterAt(4);
        GameState state = new GameState(hall(), List.of(hunter), 4);

        state.movePlayer(Direction.WAIT);
        assertEquals(GuardMode.HUNT, hunter.mode(), "it has seen the player");
        assertFalse(state.isCaught(), "it surged two tiles, and needed three");

        state.movePlayer(Direction.WAIT);

        assertTrue(state.isCaught());
        assertTrue(state.isAttemptOver());
        assertEquals(state.player(), hunter.position(), "it is standing on the player");
    }

    @Test
    @DisplayName("Being caught ends the attempt, not the run, while lives remain")
    void captureEndsTheAttemptNotTheRun() {
        GameState state = new GameState(hall(), List.of(hunterAt(4)), 4);

        state.movePlayer(Direction.WAIT);
        state.movePlayer(Direction.WAIT);
        assertTrue(state.isCaught());

        assertFalse(state.isFinished(), "there are lives left");
        assertEquals(GameState.STARTING_LIVES, state.livesLeft(), "unspent until claimed");
    }

    @Test
    @DisplayName("A caught attempt accepts no further moves")
    void caughtAttemptAcceptsNoMoves() {
        GameState state = new GameState(hall(), List.of(hunterAt(4)), 4);

        state.movePlayer(Direction.WAIT);
        state.movePlayer(Direction.WAIT);
        assertTrue(state.isCaught());

        int turns = state.turnsTaken();
        assertFalse(state.movePlayer(Direction.RIGHT), "there is nowhere to run to");
        assertEquals(turns, state.turnsTaken());
    }

    @Test
    @DisplayName("Being caught counts as being seen")
    void captureCountsAsASighting() {
        GameState state = new GameState(hall(), List.of(hunterAt(4)), 4);

        state.movePlayer(Direction.WAIT);
        state.movePlayer(Direction.WAIT);

        assertTrue(state.wasSpotted());
        assertTrue(state.sightings() > 0);
    }

    @Test
    @DisplayName("A capture cannot be undone: that is what makes a life cost something")
    void captureCannotBeUndone() {
        GameState state = new GameState(hall(), List.of(hunterAt(4)), 4);

        state.movePlayer(Direction.WAIT);
        state.movePlayer(Direction.WAIT);
        assertTrue(state.isCaught());

        assertFalse(state.canUndo());
        assertFalse(state.undoLastMove());
        assertTrue(state.isCaught(), "still caught");
    }

    // -------------------------------------------------- guards as obstacles

    @Test
    @DisplayName("A patroller cannot catch you, however long it stands on your doorstep")
    void aPatrollerNeverCatchesYou() {
        // Its route walks the length of the hall, straight at the player.
        Patroller patroller = new Patroller("P", new Position(1, 6),
                List.of(new Position(1, 6), new Position(1, 1)));
        GameState state = new GameState(hall(), List.of(patroller), 4);

        for (int turn = 0; turn < 12; turn++) {
            state.movePlayer(Direction.WAIT);
        }

        assertFalse(state.isCaught(), "a patroller is an obstacle, not a hazard");
        assertFalse(state.isAttemptOver());
    }

    @Test
    @DisplayName("A guard blocks a tile: walking into one is not a move, and not a death")
    void guardsBlockLikeWalls() {
        Patroller patroller = new Patroller("P", new Position(1, 2), List.of(new Position(1, 2)));
        GameState state = new GameState(hall(), List.of(patroller), 4);

        assertFalse(state.movePlayer(Direction.RIGHT), "the guard is in the way");
        assertEquals(0, state.turnsTaken(), "a refused move costs nothing");
        assertFalse(state.isCaught());
    }

    @Test
    @DisplayName("A patroller stops rather than walking through the player")
    void aPatrollerStopsAtThePlayer() {
        Patroller patroller = new Patroller("P", new Position(1, 3),
                List.of(new Position(1, 3), new Position(1, 1)));
        GameState state = new GameState(hall(), List.of(patroller), 4);

        state.movePlayer(Direction.WAIT);
        state.movePlayer(Direction.WAIT);
        state.movePlayer(Direction.WAIT);

        assertEquals(new Position(1, 2), patroller.position(),
                "it should be stopped on the tile beside the player");
        assertFalse(state.isCaught());
    }

    // ---------------------------------------------------------------- escape

    @Test
    @DisplayName("Escaping beats being caught on the same turn")
    void escapingWinsOverCapture() {
        // A patroller walks onto the exit on the very turn the player reaches
        // it. Getting out should still win the tie.
        GameMap map = LevelLoader.parse(List.of(
                "#######",
                "#@KE..#",
                "#######"));
        Patroller patroller = new Patroller("P", new Position(1, 5),
                List.of(new Position(1, 5), new Position(1, 3)));
        GameState state = new GameState(map, List.of(patroller), 4);

        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.RIGHT);

        assertTrue(state.hasEscaped());
        assertFalse(state.isCaught());
    }
}
