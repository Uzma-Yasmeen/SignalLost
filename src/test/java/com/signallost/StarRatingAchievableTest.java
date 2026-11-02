package com.signallost;

import com.signallost.guard.Guard;
import com.signallost.level.Level;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameState;
import com.signallost.model.TileType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that all three stars can actually be earned.
 *
 * <p>Two of the stars pull against each other: one wants speed, the other wants
 * never being seen, and avoiding a guard costs turns. If par is set from the
 * route that ignores guards, the two can become mutually exclusive and the
 * third star is a promise the level cannot keep.
 *
 * <p>That is exactly what happened. Level 1's fastest escape took 38 turns and
 * its fastest <em>unseen</em> escape took 56, against a par of 51 — so no
 * amount of skill could ever produce three stars, and the only way to find out
 * was to keep failing at it. This test searches for the shortest unseen escape
 * and insists par leaves room for it.
 *
 * <p><strong>What this file does and does not prove.</strong> The exhaustive
 * search is affordable on the smallest level and nowhere else, so it runs on
 * level 1 alone. For a long time the requirement said three stars were
 * achievable on <em>every</em> level and pointed here as the proof, which was
 * an overclaim: four levels began with the player inside a patroller's sight on
 * turn one, making the stealth star impossible, and nothing noticed. The cheap
 * check below now covers all ten. It is a necessary condition rather than a
 * sufficient one — an unseen opening does not guarantee an unseen escape — but
 * it is the condition those four failed.
 */
class StarRatingAchievableTest {

    private static final Direction[] MOVES = {
        Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT, Direction.WAIT};

    /** Searching the whole tree is expensive; past this we stand aside rather than lie. */
    private static final int STATE_BUDGET = 400_000;

    @ParameterizedTest(name = "{0}: three stars is actually reachable")
    @ValueSource(strings = {"level1"})
    void threeStarsIsReachable(String name) {
        Level level = LevelLoader.loadFromResource(name);
        int par = new GameState(level.map(), List.of(), 5, level.turnLimit()).parTurns();

        int unseenBest = shortestUnseenEscape(name, level.turnLimit());
        assertTrue(unseenBest != Integer.MIN_VALUE,
                name + ": the search outgrew its " + STATE_BUDGET + " state budget, so three"
                        + " stars can no longer be verified here. Raise the budget or simplify"
                        + " the level, but do not leave the claim unchecked.");

        assertTrue(unseenBest > 0,
                name + ": there is no unseen escape at all, so the stealth star is unearnable");
        assertTrue(unseenBest <= par,
                name + ": the fastest unseen escape takes " + unseenBest
                        + " turns but par is " + par
                        + ", so the speed star and the stealth star cannot both be had");
    }

    /**
     * Fewest turns to escape having never been seen.
     *
     * <p>Breadth-first over turns, pruning any branch the moment the player is
     * spotted, which is what keeps the search small enough to be worth running.
     *
     * @return the turn count, 0 when no unseen escape exists, or
     *         {@link Integer#MIN_VALUE} when the search ran out of budget
     */
    /**
     * Every level must allow a first turn that ends unseen.
     *
     * <p>This is the cheap half of the claim and it runs on all ten levels in
     * milliseconds. If a guard begins within sight of the player's own starting
     * tile, the player is seen on turn one whatever they do, the stealth star
     * is gone before they have made a decision, and three stars is arithmetic
     * rather than difficulty.
     */
    @ParameterizedTest(name = "{0}: the player can survive turn one unseen")
    @ValueSource(strings = {"level1", "level2", "level3", "level4", "level5",
                            "level6", "level7", "level8", "level9", "level10"})
    void anUnseenOpeningExists(String name) {
        List<Direction> safeOpenings = new ArrayList<>();
        int legalOpenings = 0;

        for (Direction move : MOVES) {
            GameState state = build(name);
            if (!state.movePlayer(move)) {
                continue;
            }
            legalOpenings++;
            if (state.sightings() == 0) {
                safeOpenings.add(move);
            }
        }

        assertTrue(legalOpenings > 0, name + ": the player cannot move at all on turn one");
        assertTrue(!safeOpenings.isEmpty(),
                name + ": every one of the " + legalOpenings + " legal opening moves ends inside"
                        + " a guard's sight, so the stealth star cannot be earned. Start the"
                        + " nearest guard further from the player's own starting tile.");
    }

    private static int shortestUnseenEscape(String levelName, int turnLimit) {
        List<List<Direction>> frontier = new ArrayList<>();
        frontier.add(new ArrayList<>());
        Set<String> seen = new HashSet<>();
        seen.add(stateKey(build(levelName)));

        for (int turn = 1; turn <= turnLimit; turn++) {
            List<List<Direction>> next = new ArrayList<>();
            for (List<Direction> prefix : frontier) {
                GameState state = build(levelName);
                for (Direction move : prefix) {
                    state.movePlayer(move);
                }
                for (Direction move : MOVES) {
                    // Undo deliberately no longer refunds a sighting, so the
                    // count does not return to where it was when this search
                    // steps back. Asking whether *this* move added one is still
                    // exact, and is all the search needs, because a prefix only
                    // ever grows by moves that added none.
                    int sightingsBefore = state.sightings();
                    if (!state.movePlayer(move)) {
                        continue;
                    }
                    boolean spotted = state.sightings() > sightingsBefore;
                    if (state.hasEscaped() && !spotted) {
                        return turn;
                    }
                    if (!state.isAttemptOver() && !spotted && seen.add(stateKey(state))) {
                        List<Direction> child = new ArrayList<>(prefix);
                        child.add(move);
                        next.add(child);
                    }
                    state.undoLastMove();
                }
            }
            if (next.isEmpty()) {
                return 0;
            }
            if (seen.size() > STATE_BUDGET) {
                return Integer.MIN_VALUE;
            }
            frontier = next;
        }
        return 0;
    }

    private static GameState build(String levelName) {
        Level level = LevelLoader.loadFromResource(levelName);
        return new GameState(level.map(), level.guards(), 5, level.turnLimit());
    }

    /**
     * Which keys are still on the floor, not how many are held: counting them
     * makes holding the first key and holding the last look identical.
     */
    private static String stateKey(GameState state) {
        StringBuilder key = new StringBuilder();
        key.append(state.player()).append('|');
        key.append(state.map().findAll(TileType.KEY)).append('|');
        for (Guard guard : state.guards()) {
            key.append(guard.state()).append(';');
        }
        return key.toString();
    }
}
