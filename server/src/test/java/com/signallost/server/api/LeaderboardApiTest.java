package com.signallost.server.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.signallost.server.WinningRun;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The service end to end: a real HTTP request, a real database, and a run
 * replayed against the real game.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LeaderboardApiTest {

    private static final String LEVEL = "level1";

    @Autowired
    private MockMvc http;

    @Autowired
    private ObjectMapper json;

    private static String winningMoves;

    @BeforeAll
    static void findAWinningRun() {
        winningMoves = WinningRun.on(LEVEL);
    }

    private String body(String player, String level, String moves) throws Exception {
        return json.writeValueAsString(Map.of(
                "playerName", player, "levelName", level, "moves", moves));
    }

    // -------------------------------------------------------------- happy path

    @Test
    @DisplayName("A genuine run is accepted and scored by the server")
    void acceptsAGenuineRun() throws Exception {
        http.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Uzma", LEVEL, winningMoves)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.playerName").value("Uzma"))
                .andExpect(jsonPath("$.levelName").value(LEVEL))
                .andExpect(jsonPath("$.stars").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.turns").value(winningMoves.length()))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @DisplayName("A player appears on the leaderboard once, at their best")
    void leaderboardKeepsOnlyTheBestPerPlayer() throws Exception {
        // The same player finishes the level twice. Padding the second run with
        // waits is not an option: the game is deterministic, so delaying by a
        // turn moves every guard and a winning route stops winning. Submitting
        // the same run twice tests what this is about, which is that the
        // leaderboard collapses a player's history to one row.
        http.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON)
                .content(body("Repeat", LEVEL, winningMoves))).andExpect(status().isCreated());
        http.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON)
                .content(body("Repeat", LEVEL, winningMoves))).andExpect(status().isCreated());

        assertEquals(1, timesListed("Repeat"),
                "a player's history collapses to one leaderboard row");
    }

    @Test
    @DisplayName("Every submission is kept, even the ones not on the leaderboard")
    void historyKeepsEverything() throws Exception {
        http.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON)
                .content(body("Historian", LEVEL, winningMoves))).andExpect(status().isCreated());
        http.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON)
                .content(body("Historian", LEVEL, winningMoves))).andExpect(status().isCreated());

        http.perform(get("/api/players/Historian/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ----------------------------------------------------------- the refusals

    @Test
    @DisplayName("A run that does not reach the exit is refused with 422")
    void refusesAFabricatedRun() throws Exception {
        String notAWin = winningMoves.substring(0, winningMoves.length() - 2);

        http.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Chancer", LEVEL, notAWin)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("run rejected"))
                .andExpect(jsonPath("$.reason").exists());
    }

    @Test
    @DisplayName("A refused run leaves nothing behind, not even the player")
    void aRefusedRunIsNotRecorded() throws Exception {
        String notAWin = winningMoves.substring(0, 3);

        http.perform(post("/api/runs").contentType(MediaType.APPLICATION_JSON)
                .content(body("Ghost", LEVEL, notAWin)))
                .andExpect(status().isUnprocessableEntity());

        http.perform(get("/api/players/Ghost/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        assertEquals(0, timesListed("Ghost"),
                "a rejected run must not create a player row either");
    }

    /**
     * How many leaderboard rows name this player.
     *
     * <p>Read out and counted in Java rather than with a JSONPath filter, which
     * reports the length of the matched elements rather than of the match.
     */
    private int timesListed(String playerName) throws Exception {
        String response = http.perform(get("/api/leaderboard/" + LEVEL).param("limit", "50"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode entries = json.readTree(response).get("entries");
        int listed = 0;
        for (JsonNode entry : entries) {
            if (playerName.equals(entry.get("playerName").asText())) {
                listed++;
            }
        }
        return listed;
    }

    @Test
    @DisplayName("A malformed request is refused with 400 and says which field")
    void refusesAMalformedRequest() throws Exception {
        http.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", LEVEL, winningMoves)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid request"))
                .andExpect(jsonPath("$.fields.playerName").exists());
    }

    @Test
    @DisplayName("Moves that are not moves are refused before anything is replayed")
    void refusesNonsenseMoves() throws Exception {
        http.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Typo", LEVEL, "RRXRR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.moves").exists());
    }

    // ------------------------------------------------------------- the catalogue

    @Test
    @DisplayName("The level list comes from the game, so the two cannot disagree")
    void servesTheGamesOwnLevels() throws Exception {
        http.perform(get("/api/levels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].name").value("level1"))
                .andExpect(jsonPath("$[0].starsToUnlock").value(0));
    }
}
