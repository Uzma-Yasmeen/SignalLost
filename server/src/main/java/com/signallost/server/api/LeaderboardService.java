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

    @Transactional(readOnly = true)
    public long countOn(String levelName) {
        return runs.countByLevelName(levelName);
    }
}
