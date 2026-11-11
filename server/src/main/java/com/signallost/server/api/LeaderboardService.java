package com.signallost.server.api;

import com.signallost.server.store.Player;
import com.signallost.server.store.PlayerRepository;
import com.signallost.server.store.Run;
import com.signallost.server.store.RunRepository;
import com.signallost.server.verify.RunVerifier;
import com.signallost.server.verify.VerifiedRun;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Accepts runs and answers leaderboards.
 *
 * <p>The order of work matters: a run is verified <em>before</em> anything is
 * written. A rejected submission leaves no trace, not even a player row, so a
 * stream of invalid requests cannot fill the database with names.
 */
@Service
public class LeaderboardService {

    private static final int MAX_LEADERBOARD = 50;

    private final RunVerifier verifier;
    private final RunRepository runs;
    private final PlayerRepository players;

    public LeaderboardService(RunVerifier verifier, RunRepository runs, PlayerRepository players) {
        this.verifier = verifier;
        this.runs = runs;
        this.players = players;
    }

    @Transactional
    public RunResponse submit(String playerName, String levelName, String moves) {
        // Replay first. If this throws, nothing below happens.
        VerifiedRun verified = verifier.verify(levelName, moves);

        String trimmedName = playerName.trim();
        Player player = players.findByDisplayNameIgnoreCase(trimmedName)
                .orElseGet(() -> players.save(new Player(trimmedName)));

        // Mapped here, inside the transaction. Handing a Run to the web layer
        // means its lazy player association is resolved after the session has
        // closed, which fails at serialisation time rather than anywhere useful.
        return RunResponse.of(runs.save(new Run(player, moves.trim(), verified)));
    }

    @Transactional(readOnly = true)
    public List<RunResponse> leaderboard(String levelName, int limit) {
        int capped = Math.max(1, Math.min(limit, MAX_LEADERBOARD));
        return runs.bestPerPlayerOn(levelName, Limit.of(capped)).stream()
                .map(RunResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RunResponse> historyFor(String playerName, int limit) {
        int capped = Math.max(1, Math.min(limit, MAX_LEADERBOARD));
        return runs.findByPlayerDisplayNameOrderBySubmittedAtDesc(playerName.trim(),
                        Limit.of(capped)).stream()
                .map(RunResponse::of)
                .toList();
    }

    /**
     * A player's saved progress: their best stars on every level finished.
     *
     * <p>This is what lets somebody sign in on a second device and find their
     * levels unlocked. An unknown name is not an error, it is a player with no
     * progress yet.
     */
    @Transactional(readOnly = true)
    public Map<String, Integer> progressOf(String playerName) {
        Map<String, Integer> stars = new TreeMap<>();
        for (RunRepository.LevelStars best : runs.bestStarsPerLevel(playerName.trim())) {
            stars.put(best.getLevelName(), best.getStars());
        }
        return stars;
    }

    /** The overall leaderboard: total stars across all levels, best first. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> standings(int limit) {
        int capped = Math.max(1, Math.min(limit, MAX_LEADERBOARD));
        return runs.standings(Limit.of(capped)).stream()
                .map(row -> Map.<String, Object>of(
                        "playerName", row.getPlayerName(),
                        "stars", row.getStars(),
                        "levelsFinished", row.getLevelsFinished()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long countOn(String levelName) {
        return runs.countByLevelName(levelName);
    }
}
