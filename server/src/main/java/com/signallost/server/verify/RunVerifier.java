package com.signallost.server.verify;

import com.signallost.level.Level;
import com.signallost.level.LevelCatalog;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameState;
import com.signallost.model.Score;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Works out what a submitted run actually scored, by playing it.
 *
 * <p>A leaderboard that takes the client's word for a score is a leaderboard
 * anybody can write to with a proxy and a text editor. The usual answers —
 * obfuscation, a signing key in the client, a plausibility heuristic — all
 * amount to making it inconvenient rather than impossible, because the number
 * still originates on a machine the player controls.
 *
 * <p>This server does not accept a score at all. It accepts the moves, replays
 * them against the same rules the game runs, and computes the score itself. To
 * claim three stars you have to submit a sequence of moves that actually earns
 * three stars, which is to say you have to have played the level.
 *
 * <p>Two properties of the game make that possible, and both are enforced by
 * tests in the game's own build. The rules carry no user interface, so they can
 * run inside a web request. And the game is deterministic, so replaying the
 * same moves from the same start reaches the same end — otherwise a legitimate
 * run would verify differently here than it played there.
 */
@Service
public class RunVerifier {

    private final Set<String> knownLevels;

    public RunVerifier() {
        this(LevelCatalog.load().stream()
                .map(LevelCatalog.Entry::name)
                .collect(Collectors.toUnmodifiableSet()));
    }

    RunVerifier(Set<String> knownLevels) {
        this.knownLevels = knownLevels;
    }

    /**
     * Replays a run and reports what it earned.
     *
     * @param levelName which level, as named in the catalogue
     * @param moves     one letter per turn
     * @throws RunRejectedException if the moves are not a legal, winning run
     */
    public VerifiedRun verify(String levelName, String moves) {
        if (!knownLevels.contains(levelName)) {
            throw new RunRejectedException("There is no level called '" + levelName + "'");
        }

        List<Direction> played = MoveSequence.parse(moves);
        Level level = LevelLoader.loadFromResource(levelName);
        GameState state = new GameState(
                level.map(), level.guards(), GameState.DEFAULT_SIGHT_RADIUS, level.turnLimit());

        for (int turn = 0; turn < played.size(); turn++) {
            if (state.isAttemptOver()) {
                throw new RunRejectedException(
                        "The run continues past turn " + turn + ", but the attempt had ended");
            }
            if (!state.movePlayer(played.get(turn))) {
                throw new RunRejectedException(
                        "Move " + (turn + 1) + " (" + played.get(turn)
                                + ") is not legal from where the player was standing");
            }
        }

        if (state.isCaught()) {
            throw new RunRejectedException("The run ends with the player caught");
        }
        if (!state.hasEscaped()) {
            throw new RunRejectedException(
                    "The run ends without reaching the exit after " + played.size() + " moves");
        }

        Score score = state.score();
        return new VerifiedRun(levelName, score.stars(), score.turns(), score.par(),
                score.sightings(), played.size());
    }
}
