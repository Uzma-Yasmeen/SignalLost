package com.signallost.server;

import com.signallost.level.Level;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameState;
import com.signallost.server.verify.MoveSequence;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds a genuine winning run, for tests that need one.
 *
 * <p>Hard-coding a sequence would be quicker and would rot the first time a
 * level is redrawn, leaving a test that fails for reasons nothing to do with
 * the code under test. Searching for one costs a few seconds and keeps working.
 */
public final class WinningRun {

    private static final int MAX_TURNS = 120;

    private WinningRun() {
    }

    /** The shortest sequence of moves that escapes, as letters. */
    public static String on(String levelName) {
        Deque<List<Direction>> frontier = new ArrayDeque<>();
        frontier.add(new ArrayList<>());
        Set<String> seen = new HashSet<>();

        while (!frontier.isEmpty()) {
            List<Direction> prefix = frontier.poll();
            if (prefix.size() > MAX_TURNS) {
                continue;
            }
            GameState state = build(levelName);
            for (Direction move : prefix) {
                state.movePlayer(move);
            }
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
                if (worthExploring) {
                    List<Direction> child = new ArrayList<>(prefix);
                    child.add(move);
                    frontier.add(child);
                }
            }
        }
        throw new IllegalStateException("No escape found on " + levelName);
    }

    public static GameState build(String levelName) {
        Level level = LevelLoader.loadFromResource(levelName);
        return new GameState(level.map(), level.guards(),
                GameState.DEFAULT_SIGHT_RADIUS, level.turnLimit());
    }

    private static String key(GameState state) {
        return state.player() + "|" + state.turnsTaken() + "|" + state.keysHeld();
    }
}
