package com.signallost.server.verify;

import com.signallost.level.Level;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The verifier is the only reason this service is worth running, so it is worth
 * testing from both ends: that a genuine run is accepted and scored exactly as
 * the game would score it, and that everything else is refused.
 */
class RunVerifierTest {

    private static final String LEVEL = "level1";

    private final RunVerifier verifier = new RunVerifier();

    /** A real winning sequence, found once by search and shared by every test. */
    private static String winningMoves;

    @BeforeAll
    static void findAWinningRun() {
        winningMoves = shortestEscape(LEVEL);
        assertNotNull(winningMoves, "no escape found, so none of these tests mean anything");
    }

    // --------------------------------------------------------------- accepts

    @Test
    @DisplayName("A genuine run is accepted")
    void acceptsARealRun() {
        VerifiedRun verified = verifier.verify(LEVEL, winningMoves);

        assertEquals(LEVEL, verified.levelName());
        assertEquals(winningMoves.length(), verified.moveCount());
        assertTrue(verified.stars() >= 1, "escaping is worth at least one star");
        assertEquals(winningMoves.length(), verified.turns(),
                "every move in a clean run costs exactly one turn");
    }

    @Test
    @DisplayName("The server reaches the same verdict the game does")
    void agreesWithTheGame() {
        // What the game itself would say, playing the same moves.
        Level level = LevelLoader.loadFromResource(LEVEL);
        GameState played = new GameState(level.map(), level.guards(),
                GameState.DEFAULT_SIGHT_RADIUS, level.turnLimit());
        for (Direction move : MoveSequence.parse(winningMoves)) {
            played.movePlayer(move);
        }

        VerifiedRun verified = verifier.verify(LEVEL, winningMoves);

        assertEquals(played.score().stars(), verified.stars());
        assertEquals(played.score().turns(), verified.turns());
        assertEquals(played.score().par(), verified.par());
        assertEquals(played.sightings(), verified.sightings());
    }

    @Test
    @DisplayName("Verifying twice gives the same answer, because the game is deterministic")
    void isRepeatable() {
        assertEquals(verifier.verify(LEVEL, winningMoves), verifier.verify(LEVEL, winningMoves));
    }

    // --------------------------------------------------------------- refuses

    @Test
    @DisplayName("A run that stops short of the exit is refused")
    void refusesAnIncompleteRun() {
        String truncated = winningMoves.substring(0, winningMoves.length() - 1);

        RunRejectedException thrown = assertThrows(RunRejectedException.class,
                () -> verifier.verify(LEVEL, truncated));

        assertTrue(thrown.getMessage().contains("without reaching the exit"), thrown.getMessage());
    }

    @Test
    @DisplayName("A run with an illegal move is refused, and says which one")
    void refusesAnIllegalMove() {
        // Level 1 starts in a corner, so the first move cannot be upwards.
        RunRejectedException thrown = assertThrows(RunRejectedException.class,
                () -> verifier.verify(LEVEL, "U" + winningMoves));

        assertTrue(thrown.getMessage().contains("Move 1"), thrown.getMessage());
    }

    @Test
    @DisplayName("A level nobody has heard of is refused")
    void refusesAnUnknownLevel() {
        assertThrows(RunRejectedException.class, () -> verifier.verify("level99", "RR"));
    }

    @Test
    @DisplayName("An empty submission is refused")
    void refusesNothingAtAll() {
        assertThrows(RunRejectedException.class, () -> verifier.verify(LEVEL, "   "));
        assertThrows(RunRejectedException.class, () -> verifier.verify(LEVEL, null));
    }

    @Test
    @DisplayName("A submission longer than any level allows is refused before it is replayed")
    void refusesAnAbsurdlyLongRun() {
        String tooLong = "W".repeat(MoveSequence.MAX_MOVES + 1);

        RunRejectedException thrown = assertThrows(RunRejectedException.class,
                () -> verifier.verify(LEVEL, tooLong));

        assertTrue(thrown.getMessage().contains("longer than any level allows"),
                thrown.getMessage());
    }

    @Test
    @DisplayName("A letter that is not a move is refused")
    void refusesNonsense() {
        RunRejectedException thrown = assertThrows(RunRejectedException.class,
                () -> verifier.verify(LEVEL, "RRXRR"));

        assertTrue(thrown.getMessage().contains("'X'"), thrown.getMessage());
    }

    @Test
    @DisplayName("Who submits a run makes no difference to what it scores")
    void scoringDoesNotDependOnTheCaller() {
        // There is no parameter in which a client could put a score, and no
        // state in the verifier that a caller could influence. Two submissions
        // of the same moves are therefore the same run, whoever sent them.
        VerifiedRun first = verifier.verify(LEVEL, winningMoves);
        VerifiedRun second = new RunVerifier().verify(LEVEL, winningMoves);

        assertEquals(first, second);
    }

    // ------------------------------------------------------------ the search

    /**
     * Breadth-first search for any escape at all, so the tests above have a
     * genuine run to work with rather than a hard-coded one that would rot the
     * first time the level is redrawn.
     */
    private static String shortestEscape(String levelName) {
        Deque<List<Direction>> frontier = new ArrayDeque<>();
        frontier.add(new ArrayList<>());
        Set<String> seen = new HashSet<>();

        while (!frontier.isEmpty()) {
            List<Direction> prefix = frontier.poll();
            GameState state = build(levelName);
            for (Direction move : prefix) {
                state.movePlayer(move);
            }
            if (prefix.size() > 120) {
                continue;
            }
            // One state per prefix, stepped forward and back for each candidate
            // move. Rebuilding it per candidate instead turned this search into
            // a minute of wall clock on its own.
            for (Direction move : Direction.values()) {
                if (!state.movePlayer(move)) {
                    continue;
                }
                if (state.hasEscaped()) {
                    List<Direction> winner = new ArrayList<>(prefix);
                    winner.add(move);
                    return MoveSequence.format(winner);
                }
                boolean worthExploring = !state.isAttemptOver() && seen.add(key(state));
                state.undoLastMove();
                if (!worthExploring) {
                    continue;
                }
                List<Direction> child = new ArrayList<>(prefix);
                child.add(move);
                frontier.add(child);
            }
        }
        return null;
    }

    private static GameState build(String levelName) {
        Level level = LevelLoader.loadFromResource(levelName);
        return new GameState(level.map(), level.guards(),
                GameState.DEFAULT_SIGHT_RADIUS, level.turnLimit());
    }

    private static String key(GameState state) {
        return state.player() + "|" + state.turnsTaken() + "|" + state.keysHeld();
    }
}
