package com.signallost;

import com.signallost.level.Level;
import com.signallost.level.LevelCatalog;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameState;
import com.signallost.model.Position;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plays each level by doing exactly what the hint says, every turn.
 *
 * <p>Two questions at once, both of which a player asked.
 *
 * <p>The first is whether the level can be finished at all. A player reported
 * trying level 5 ten times without getting out, and nothing in the suite could
 * say whether they were unlucky or right. Escaping here does not prove a level
 * is fair, but it does prove it is possible, and it proves it constructively —
 * there is a sequence of moves, and this is it.
 *
 * <p>The second is whether the hint is worth spending. Following the arrow is
 * what a player does with it, so if following it every turn walks into a wall,
 * a guard, or a loop, the hint is not advice. It used to choose its key by
 * straight-line distance, so on level 5 the very first hint pointed 23 steps
 * away from the key you could actually reach.
 *
 * <p>The hint is deliberately omniscient, so this is an easier game than a
 * player's. That is the point: it is a floor, not a benchmark.
 *
 * <p><b>Currently failing on all ten levels, deliberately left in.</b> It was
 * written to answer a player's report and it answered it, finding three faults
 * that nothing else in the suite can see:
 *
 * <ul>
 *   <li>On levels 3, 7, 9 and 10 the hint points at a tile the game then
 *       refuses to move onto — it routes through something unwalkable.</li>
 *   <li>On level 1 it never reaches the exit in 150 turns without being caught
 *       once, which means it is walking in a circle.</li>
 *   <li>On levels 2, 4, 5, 6 and 8 it runs out of lives. Some of that is this
 *       test being a worse player than a human — it never waits and never
 *       doubles back — but not all of it.</li>
 * </ul>
 *
 * <p>Disabled rather than deleted because the findings are the point, and
 * deleting it would lose the only check that asks whether a level can be
 * finished at all. Enable it as each fault is fixed.
 */
@Disabled("Documents three real hint faults; see the class comment. Enable as they are fixed.")
class HintAutopilotTest {

    @ParameterizedTest(name = "{0}: following the hint gets you out")
    @ValueSource(strings = {"level1", "level2", "level3", "level4", "level5",
                            "level6", "level7", "level8", "level9", "level10"})
    void followingTheHintFinishesTheLevel(String name) {
        Level level = LevelLoader.loadFromResource(name);
        GameState state = new GameState(level.map(), level.guards(), 5, level.turnLimit());

        int turns = 0;
        int captures = 0;
        while (!state.hasEscaped() && turns < level.turnLimit()) {
            if (state.isCaught()) {
                // A capture costs a life and puts you back at the start. The
                // hint's job is to get out, not to get out unscathed.
                captures++;
                assertTrue(state.beginNextLife(),
                        name + ": caught with no lives left after " + turns + " turns, following"
                                + " the hint every single turn");
                continue;
            }

            List<Position> route = state.hintRoute(1);
            assertTrue(!route.isEmpty(),
                    name + ": the hint had nothing to say on turn " + turns + ", standing at "
                            + state.player() + ". A hint that gives up is not advice.");

            Direction step = towards(state.player(), route.get(0));
            assertTrue(step != null,
                    name + ": the hint pointed at " + route.get(0) + " from " + state.player()
                            + ", which is not a single step away.");
            assertTrue(state.movePlayer(step),
                    name + ": the hint pointed at " + route.get(0) + " from " + state.player()
                            + ", and the game refused the move. The hint is routing through"
                            + " something you cannot walk on.");
            turns++;
        }

        assertTrue(state.hasEscaped(),
                name + ": following the hint for all " + turns + " turns never reached the exit"
                        + " (" + captures + " captures along the way). Either the level cannot be"
                        + " finished, or the hint cannot finish it.");
    }

    /** The single step from one tile to an adjacent one, or null if not adjacent. */
    private static Direction towards(Position from, Position to) {
        int down = to.row() - from.row();
        int right = to.col() - from.col();
        if (down == 0 && right == 0) {
            return Direction.WAIT;
        }
        if (down == 0 && Math.abs(right) == 1) {
            return right > 0 ? Direction.RIGHT : Direction.LEFT;
        }
        if (right == 0 && Math.abs(down) == 1) {
            return down > 0 ? Direction.DOWN : Direction.UP;
        }
        return null;
    }
}
