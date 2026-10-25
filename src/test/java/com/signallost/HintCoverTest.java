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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the hint gives tactical advice, not just directions.
 *
 * <p>Pointing at the key is no use to a player standing in a guard's sight: the
 * progress they would make is progress they are about to be caught making. When
 * the player is seen, the hint points at the quickest way out of view instead.
 */
class HintCoverTest {

    /** A corridor with a side pocket the sentry cannot see into. */
    private static GameMap corridorWithPocket() {
        return LevelLoader.parse(List.of(
                "##########",
                "#@......E#",
                "#.##.#####",
                "#K#######K",
                "##########"));
    }

    @Test
    @DisplayName("Out of sight, the hint heads for what the player needs")
    void unwatchedHintChasesTheObjective() {
        GameMap map = LevelLoader.parse(List.of(
                "########",
                "#@..K.E#",
                "########"));
        GameState state = new GameState(map, 4);

        assertFalse(state.hintSeeksCover(), "nobody is watching");
        List<Position> hint = state.hintRoute(1);
        assertTrue(hint.get(0).col() > state.player().col(), "the key is to the east");
    }

    @Test
    @DisplayName("Seen, the hint points at the nearest way out of view")
    void watchedHintHeadsForCover() {
        // The sentry watches the top corridor; the pocket below is hidden.
        GameMap map = LevelLoader.parse(List.of(
                "########",
                "#@....E#",
                "#.######",
                "#K######",
                "########"));
        Patroller sentry = new Patroller("P", new Position(1, 4), List.of(new Position(1, 4)));
        GameState state = new GameState(map, List.of(sentry), 4);

        state.movePlayer(Direction.RIGHT);
        assertTrue(state.wasSpotted(), "the sentry can see the corridor");
        assertTrue(state.hintSeeksCover(), "so the hint should be about hiding");

        List<Position> hint = state.hintRoute(1);
        assertFalse(hint.isEmpty());
        assertTrue(hint.get(0).col() < state.player().col()
                        || hint.get(0).row() > state.player().row(),
                "it should head back or down towards the hidden pocket, not on into view");
    }

    @Test
    @DisplayName("The hint stops once the level is over")
    void finishedRunHasNoHint() {
        GameMap map = LevelLoader.parse(List.of(
                "#####",
                "#@KE#",
                "#####"));
        GameState state = new GameState(map, 3);

        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.RIGHT);

        assertTrue(state.hasEscaped());
        assertTrue(state.hintRoute(1).isEmpty());
    }

    @Test
    @DisplayName("Every suggested step is a legal move")
    void hintOnlySuggestsLegalMoves() {
        GameMap map = corridorWithPocket();
        GameState state = new GameState(map, 4);

        for (int turn = 0; turn < 12 && !state.isFinished(); turn++) {
            List<Position> hint = state.hintRoute(1);
            if (hint.isEmpty()) {
                break;
            }
            Position step = hint.get(0);
            assertTrue(map.isWalkable(step), step + " is not walkable");
            assertTrue(step.manhattanDistanceTo(state.player()) == 1,
                    step + " is not next to the player");
            state.movePlayer(towards(state.player(), step));
        }
    }

    private static Direction towards(Position from, Position to) {
        if (to.row() < from.row()) {
            return Direction.UP;
        }
        if (to.row() > from.row()) {
            return Direction.DOWN;
        }
        return to.col() < from.col() ? Direction.LEFT : Direction.RIGHT;
    }
}
