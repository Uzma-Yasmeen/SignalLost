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
 * Tests for a guard actually reaching the player.
 *
 * <p>Before this, a hunter could run the player down, end its turn standing on
 * top of them, and nothing happened — the two simply overlapped and the player
 * walked away. That made the whole stealth layer decorative, because there was
 * no reason not to be seen. Being reached now ends the run.
 */
class CaptureTest {

    private static GameMap hall() {
        return LevelLoader.parse(List.of(
                "##########",
                "#@......E#",
                "##########"));
    }

    @Test
    @DisplayName("A hunter that reaches the player ends the run")
    void beingReachedEndsTheRun() {
        // Three tiles away and in plain sight. Standing still lets it walk in.
        Hunter hunter = new Hunter("H", new Position(1, 4), List.of(new Position(1, 4)));
        GameState state = new GameState(hall(), List.of(hunter), 4);

        state.movePlayer(Direction.WAIT);
        assertEquals(GuardMode.HUNT, hunter.mode(), "it has seen the player");
        assertFalse(state.isCaught(), "it has closed one tile, not three");

        state.movePlayer(Direction.WAIT);
        state.movePlayer(Direction.WAIT);

        assertTrue(state.isCaught());
        assertTrue(state.isFinished());
        assertEquals(state.player(), hunter.position(), "it is standing on the player");
    }

    @Test
    @DisplayName("A caught run accepts no further moves")
    void caughtRunIsOver() {
        Hunter hunter = new Hunter("H", new Position(1, 2), List.of(new Position(1, 2)));
        GameState state = new GameState(hall(), List.of(hunter), 4);

        state.movePlayer(Direction.RIGHT);
        assertTrue(state.isCaught());

        int turns = state.turnsTaken();
        assertFalse(state.movePlayer(Direction.RIGHT), "there is nowhere to run to");
        assertEquals(turns, state.turnsTaken());
    }

    @Test
    @DisplayName("Being caught counts as being seen")
    void captureCountsAsASighting() {
        Hunter hunter = new Hunter("H", new Position(1, 2), List.of(new Position(1, 2)));
        GameState state = new GameState(hall(), List.of(hunter), 4);

        state.movePlayer(Direction.RIGHT);

        assertTrue(state.wasSpotted());
        assertEquals(1, state.sightings());
    }

    @Test
    @DisplayName("Undo takes back the capture and puts the guard where it was")
    void undoReopensACaughtRun() {
        Hunter hunter = new Hunter("H", new Position(1, 2), List.of(new Position(1, 2)));
        GameState state = new GameState(hall(), List.of(hunter), 4);
        Position hunterStart = hunter.position();

        state.movePlayer(Direction.RIGHT);
        assertTrue(state.isCaught());

        state.undoLastMove();

        assertFalse(state.isCaught());
        assertFalse(state.isFinished());
        assertEquals(hunterStart, hunter.position());
        assertEquals(0, state.sightings());
        assertTrue(state.movePlayer(Direction.DOWN) || state.movePlayer(Direction.WAIT),
                "the player can try something else");
    }

    @Test
    @DisplayName("A patroller blundering into you catches you just the same")
    void contactIsContact() {
        // It never chases, but its route walks straight over the player.
        Patroller patroller = new Patroller("P", new Position(1, 5),
                List.of(new Position(1, 5), new Position(1, 1)));
        GameState state = new GameState(hall(), List.of(patroller), 4);

        // Stand still and let it walk the length of the hall onto you.
        for (int i = 0; i < 6 && !state.isCaught(); i++) {
            state.movePlayer(Direction.WAIT);
        }

        assertTrue(state.isCaught(), "it should have reached the player's tile");
    }

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
        assertFalse(state.isCaught(), "getting out is getting out");
    }
}
