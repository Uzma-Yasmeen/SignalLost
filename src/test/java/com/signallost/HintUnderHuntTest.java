package com.signallost;

import com.signallost.guard.Guard;
import com.signallost.guard.GuardMode;
import com.signallost.guard.Hunter;
import com.signallost.level.Level;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameState;
import com.signallost.model.Position;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a hint says while something is chasing you.
 *
 * <p>A hint is one tile, never a route, and three per level. The question is
 * which tile, and the answer used to change for the wrong reason: the hint
 * switched from "the key is this way" to "get out of sight" only while a guard
 * could see the player <em>that turn</em>. A hunter that had lost them for a
 * single turn is still coming and still knows roughly where they are, but for
 * that turn the hint cheerfully pointed back towards the key, which on a
 * corridor meant pointing straight at the hunter.
 */
class HintUnderHuntTest {

    /**
     * A corridor with the hunter on one side and the key on the other.
     *
     * <p>The player stands in the middle. Towards the key is towards the
     * hunter; away from the hunter is away from the key. There is no clever
     * third option, so the hint has to actually choose.
     */
    private static final String[] CORRIDOR = {
        "###############",
        "#E............#",
        "#....@......K.#",
        "#.............#",
        "###############",
    };

    /** Far enough that the hunter cannot see the player, close enough to matter. */
    private static final Position HUNTER_TILE = new Position(2, 10);

    @Test
    void theHintPointsAwayFromAHunterThatHasLostSight() {
        GameState state = corridorWithHunterAt(HUNTER_TILE);

        // Out of sight this turn, but hunting and close: the dangerous moment
        // the old rule did not cover.
        assertTrue(!state.isPlayerWatched(),
                "this test only means something while the hunter cannot see the player");
        assertTrue(state.hintSeeksCover(),
                "a hunter two tiles away is still hunting, so the hint should be about escaping");

        List<Position> route = state.hintRoute(1);
        assertEquals(1, route.size(), "a hint is one tile, never a route");

        // Further from the hunter, rather than any particular direction: in a
        // corridor three rows deep, stepping aside is as good an escape as
        // stepping back, and demanding one of them would be testing the route
        // finder's taste rather than whether the hint understands the danger.
        int before = state.player().manhattanDistanceTo(HUNTER_TILE);
        int after = route.get(0).manhattanDistanceTo(HUNTER_TILE);
        assertTrue(after > before,
                "the hint pointed to " + route.get(0) + ", which is " + after + " from the hunter"
                        + " at " + HUNTER_TILE + " when the player at " + state.player()
                        + " was already " + before + " away. The key lies past the hunter, so"
                        + " that is the hint walking the player into it.");
    }

    @Test
    void theHintIsStillOneTileWhenNothingIsChasing() {
        Level level = LevelLoader.parseLevel("corridor", List.of(CORRIDOR));
        GameState state = new GameState(level.map(), List.of(), 5, level.turnLimit());

        List<Position> route = state.hintRoute(1);
        assertEquals(1, route.size(), "a hint is one tile, never a route");
        assertTrue(!state.hintSeeksCover(), "nothing is chasing, so the hint is about the key");
        assertTrue(route.get(0).col() > state.player().col(),
                "with nothing chasing, the hint should head for the key");
    }

    /** The corridor, with a hunter placed and already in full chase. */
    private static GameState corridorWithHunterAt(Position where) {
        Level level = LevelLoader.parseLevel("corridor", List.of(CORRIDOR));
        Guard hunter = new Hunter("chaser", where, List.of(where));
        hunter.applyState(hunter.state().withMode(GuardMode.HUNT));
        return new GameState(level.map(), List.of(hunter), 5, level.turnLimit());
    }
}
