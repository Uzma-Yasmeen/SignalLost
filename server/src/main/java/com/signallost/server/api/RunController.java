package com.signallost.server.api;

import com.signallost.level.LevelCatalog;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RunController {

    private final LeaderboardService leaderboards;

    public RunController(LeaderboardService leaderboards) {
        this.leaderboards = leaderboards;
    }

    /**
     * Submits a finished run.
     *
     * <p>Returns 201 with the score the server computed, or 422 when the moves
     * do not amount to a winning run. The distinction between "you sent
     * nonsense" and "you are claiming something you did not do" is not drawn,
     * because from here they are the same event.
     */
    @PostMapping("/runs")
    public ResponseEntity<RunResponse> submit(@Valid @RequestBody SubmitRunRequest request) {
        RunResponse stored = leaderboards.submit(
                request.playerName(), request.levelName(), request.moves());

        return ResponseEntity
                .created(URI.create("/api/runs/" + stored.id()))
                .body(stored);
    }

    /** Each player's best run on one level, best first. */
    @GetMapping("/leaderboard/{levelName}")
    public Map<String, Object> leaderboard(
            @PathVariable String levelName,
            @RequestParam(defaultValue = "10") int limit) {

        List<RunResponse> entries = leaderboards.leaderboard(levelName, limit);

        return Map.of(
                "level", levelName,
                "totalRuns", leaderboards.countOn(levelName),
                "entries", entries);
    }

    /** Everything one player has submitted, most recent first. */
    @GetMapping("/players/{playerName}/runs")
    public List<RunResponse> history(
            @PathVariable String playerName,
            @RequestParam(defaultValue = "20") int limit) {

        return leaderboards.historyFor(playerName, limit);
    }

    /**
     * The levels this server will accept runs for.
     *
     * <p>Read from the game's own catalogue rather than kept in a table, so the
     * server and the game can never disagree about what exists.
     */
    @GetMapping("/levels")
    public List<Map<String, Object>> levels() {
        return LevelCatalog.load().stream()
                .map(entry -> Map.<String, Object>of(
                        "name", entry.name(),
                        "displayName", entry.displayName(),
                        "starsToUnlock", entry.starsToUnlock()))
                .toList();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

}
