package com.signallost;

import com.signallost.level.Level;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameState;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hints are limited, and the limit has to survive the two things that would
 * otherwise make it meaningless: undo, and losing a life.
 */
class HintBudgetTest {

    private static GameState level1() {
        Level level = LevelLoader.loadFromResource("level1");
        return new GameState(level.map(), level.guards(), 5, level.turnLimit());
    }

    /**
     * Wanders until the attempt ends, which on level 1 means walking into the
     * hunter. Written as a loop over whatever move is legal rather than a fixed
     * sequence of keys, so it keeps working when the level is redrawn — a
     * scripted walk here quietly stopped ending the attempt at all, and the two
     * tests built on it passed without asserting anything.
     */
    private static void playUntilTheAttemptEnds(GameState state) {
        for (int guard = 0; guard < 2_000 && !state.isAttemptOver(); guard++) {
            boolean moved = false;
            for (Direction direction : Direction.values()) {
                if (state.movePlayer(direction)) {
                    moved = true;
                    break;
                }
            }
            if (!moved) {
                break;
            }
        }
        assertTrue(state.isAttemptOver(),
                "this test is pointless unless the attempt actually ends");
    }

    @Test
    @DisplayName("A level starts with three hints")
    void startsWithThree() {
        assertEquals(3, level1().hintsLeft());
        assertEquals(3, GameState.STARTING_HINTS);
    }

    @Test
    @DisplayName("Spending runs the budget down and then refuses")
    void spendingRunsOut() {
        GameState state = level1();

        for (int spent = 1; spent <= GameState.STARTING_HINTS; spent++) {
            assertTrue(state.canSpendHint(), "hint " + spent + " should be available");
            assertTrue(state.spendHint());
            assertEquals(GameState.STARTING_HINTS - spent, state.hintsLeft());
        }

        assertFalse(state.canSpendHint());
        assertFalse(state.spendHint(), "a fourth hint must not be given out");
        assertEquals(0, state.hintsLeft(), "a refused hint must not go negative");
    }

    @Test
    @DisplayName("Undo does not buy a hint back")
    void undoDoesNotRefund() {
        GameState state = level1();
        state.movePlayer(Direction.DOWN);
        state.spendHint();
        assertEquals(2, state.hintsLeft());

        assertTrue(state.undoLastMove());

        assertEquals(2, state.hintsLeft(),
                "if undo refunded hints, three would be unlimited: ask, undo, repeat");
    }

    @Test
    @DisplayName("A new life does not come with new hints")
    void livesDoNotRefill() {
        GameState state = level1();
        state.spendHint();
        state.spendHint();
        assertEquals(1, state.hintsLeft());

        playUntilTheAttemptEnds(state);
        assertTrue(state.beginNextLife(), "there should be lives left to take");

        assertEquals(1, state.hintsLeft(),
                "a fresh attempt is not a fresh set of hints");
        assertTrue(state.canSpendHint(), "the last hint should still be usable");
    }

    @Test
    @DisplayName("A finished attempt cannot be hinted at")
    void noHintsOnceTheAttemptIsOver() {
        GameState state = level1();

        playUntilTheAttemptEnds(state);

        assertFalse(state.canSpendHint(),
                "there is nothing to advise once the attempt has ended");
        assertFalse(state.spendHint());
        assertEquals(3, state.hintsLeft(), "a refused hint must not be charged for");
    }
}
