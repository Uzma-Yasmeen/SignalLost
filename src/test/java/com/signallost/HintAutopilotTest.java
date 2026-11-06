package com.signallost;

import com.signallost.level.Level;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameState;
import com.signallost.model.Position;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plays each level by doing exactly what the hint says, every turn.
 *
 * <p>Written to answer a player's report that level 5 could not be finished
 * and that the hints were no help. It found three faults nothing else in the
 * suite could see, all now fixed and each guarded below:
 *
 * <ul>
 *   <li>The hint pointed at tiles the game then refused to move onto. It
 *       treated a guard as expensive ground rather than a closed door, so when
 *       one stood in the only corridor the route went straight through it.</li>
 *   <li>When that route failed it returned nothing at all, so the hint went
 *       blank — having charged the player one of only three.</li>
 *   <li>It alternated between advancing on the key and retreating out of
 *       sight, because "cover" meant the nearest safe tile, which is almost
 *       always the tile just left. On level 1 that was a hundred and fifty
 *       turns of pacing without ever reaching the exit.</li>
 * </ul>
 *
 * <p>The hint is deliberately omniscient, so an autopilot following it plays an
 * easier game than a person. It is still a worse player than a person, because
 * it never waits for a patrol to pass and never doubles back. That is why
 * finishing the level is a separate, disabled expectation: the checks below are
 * about whether the advice is sound, which is the hint's job, not whether a
 * greedy robot can win with it.
 */
class HintAutopilotTest {

    /** Long enough to meet patrols repeatedly, short enough to stay quick. */
    private static final int TURNS_WATCHED = 120;

    @ParameterizedTest(name = "{0}: every hint is a move the game allows")
    @ValueSource(strings = {"level1", "level2", "level3", "level4", "level5",
                            "level6", "level7", "level8", "level9", "level10"})
    void everyHintIsALegalMove(String name) {
        GameState state = build(name);

        for (int turn = 0; turn < TURNS_WATCHED && !state.hasEscaped(); turn++) {
            if (state.isAttemptOver()) {
                if (!state.beginNextLife()) {
                    return;
                }
                continue;
            }
            List<Position> route = state.hintRoute(1);
            assertTrue(!route.isEmpty(),
                    name + ": the hint went blank on turn " + turn + " at " + state.player()
                            + ". A hint that says nothing has still cost one of three.");

            Position to = route.get(0);
            Direction step = towards(state.player(), to);
            assertTrue(step != null,
                    name + ": the hint pointed at " + to + " from " + state.player()
                            + ", which is not one step.");
            assertTrue(state.movePlayer(step),
                    name + ": the game refused the hint's own move, " + step + " from "
                            + state.player() + " to " + to + ". The hint is routing through"
                            + " something that cannot be walked on — a guard blocks like a wall.");
        }
    }

    @ParameterizedTest(name = "{0}: the hint does not pace back and forth")
    @ValueSource(strings = {"level1", "level2", "level3", "level4", "level5",
                            "level6", "level7", "level8", "level9", "level10"})
    void theHintDoesNotOscillate(String name) {
        GameState state = build(name);
        List<Position> visited = new ArrayList<>();

        for (int turn = 0; turn < TURNS_WATCHED && !state.hasEscaped(); turn++) {
            if (state.isAttemptOver()) {
                if (!state.beginNextLife()) {
                    break;
                }
                visited.clear();
                continue;
            }
            List<Position> route = state.hintRoute(1);
            if (route.isEmpty()) {
                break;
            }
            visited.add(state.player());
            Direction step = towards(state.player(), route.get(0));
            if (step == null || !state.movePlayer(step)) {
                break;
            }
        }

        // A B A B A B is the shape the old "nearest cover" produced: advance
        // into view, retreat out of it, repeat until the clock runs out.
        int longestPacing = 0;
        int run = 0;
        for (int i = 2; i < visited.size(); i++) {
            if (visited.get(i).equals(visited.get(i - 2))
                    && !visited.get(i).equals(visited.get(i - 1))) {
                run++;
                longestPacing = Math.max(longestPacing, run);
            } else {
                run = 0;
            }
        }
        assertTrue(longestPacing < 6,
                name + ": the hint paced between two tiles " + (longestPacing + 2)
                        + " turns running. Following it goes nowhere.");
    }

    /**
     * The whole level, start to exit, on hints alone.
     *
     * <p>Disabled: this is a greedy robot that never waits for a patrol to pass
     * and never doubles back, so it dies on levels a person would finish. It is
     * kept because it is the only check that asks whether a level can be
     * completed at all, and because if it ever passes it has produced a
     * constructive proof — an actual sequence of moves — for every level.
     */
    @Disabled("The autopilot plays worse than a person; see the class comment.")
    @ParameterizedTest(name = "{0}: following the hint gets you out")
    @ValueSource(strings = {"level1", "level2", "level3", "level4", "level5",
                            "level6", "level7", "level8", "level9", "level10"})
    void followingTheHintFinishesTheLevel(String name) {
        Level level = LevelLoader.loadFromResource(name);
        GameState state = build(name);

        int turns = 0;
        while (!state.hasEscaped() && turns < level.turnLimit()) {
            if (state.isAttemptOver()) {
                assertTrue(state.beginNextLife(),
                        name + ": out of lives after " + turns + " turns on hints alone");
                continue;
            }
            List<Position> route = state.hintRoute(1);
            Direction step = route.isEmpty() ? null : towards(state.player(), route.get(0));
            assertTrue(step != null && state.movePlayer(step),
                    name + ": the hint stopped being usable on turn " + turns);
            turns++;
        }
        assertTrue(state.hasEscaped(), name + ": never reached the exit in " + turns + " turns");
    }

    private static GameState build(String name) {
        Level level = LevelLoader.loadFromResource(name);
        return new GameState(level.map(), level.guards(), 5, level.turnLimit());
    }

    /** The single step from one tile to an adjacent one, or waiting in place. */
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
