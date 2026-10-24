package com.signallost;

import com.signallost.level.Level;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameMap;
import com.signallost.model.GameState;
import com.signallost.model.Score;
import com.signallost.pathfinding.RouteMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the rating a run earns.
 *
 * <p>One star for getting out, one for doing it briskly, one for doing it
 * unseen. Separate bars rather than a blended number, so a player who got two
 * can read which one they missed and go back for it.
 */
class ScoreTest {

    @Test
    @DisplayName("Not escaping earns nothing, however tidy the run was")
    void noEscapeNoStars() {
        Score score = Score.of(false, 10, 50, 0);

        assertEquals(0, score.stars());
        assertFalse(score.withinPar());
        assertFalse(score.unseen());
        assertEquals("no escape", score.summary());
    }

    @Test
    @DisplayName("Getting out at all is worth one star")
    void escapingIsOneStar() {
        Score score = Score.of(true, 80, 50, 4);

        assertEquals(1, score.stars());
        assertFalse(score.withinPar());
        assertFalse(score.unseen());
    }

    @Test
    @DisplayName("Beating par adds a star")
    void withinParAddsAStar() {
        assertEquals(2, Score.of(true, 50, 50, 3).stars(), "exactly par counts");
        assertEquals(2, Score.of(true, 30, 50, 3).stars());
        assertEquals(1, Score.of(true, 51, 50, 3).stars(), "one over does not");
    }

    @Test
    @DisplayName("Never being seen adds a star")
    void unseenAddsAStar() {
        Score score = Score.of(true, 80, 50, 0);

        assertEquals(2, score.stars());
        assertTrue(score.unseen());
    }

    @Test
    @DisplayName("Brisk and unseen is the full three")
    void briskAndUnseenIsThree() {
        Score score = Score.of(true, 40, 50, 0);

        assertEquals(Score.MAX_STARS, score.stars());
        assertTrue(score.withinPar());
        assertTrue(score.unseen());
        assertTrue(score.summary().contains("under par"));
        assertTrue(score.summary().contains("never seen"));
    }

    @Test
    @DisplayName("The summary names what was missed, so the player knows what to chase")
    void summaryExplainsTheRating() {
        String summary = Score.of(true, 80, 50, 2).summary();

        assertTrue(summary.contains("80 turns"), summary);
        assertTrue(summary.contains("par 50"), summary);
        assertTrue(summary.contains("seen 2 times"), summary);
    }

    // ------------------------------------------------------- against a level

    @ParameterizedTest(name = "{0}: par is reachable but not free")
    @ValueSource(strings = {"level1", "level2"})
    void parSitsBetweenTheRouteAndTheClock(String name) {
        Level level = LevelLoader.loadFromResource(name);
        GameState state = new GameState(level.map(), List.of(), 5, level.turnLimit());
        int clearRoute = RouteMetrics.shortestClearRoute(level.map());

        assertTrue(state.parTurns() >= clearRoute,
                name + ": par " + state.parTurns() + " is under the clear route "
                        + clearRoute + ", so three stars would be impossible");
        assertTrue(state.parTurns() < level.turnLimit(),
                name + ": par " + state.parTurns() + " is not under the turn limit "
                        + level.turnLimit() + ", so the star would be free");
    }

    @Test
    @DisplayName("A clean walk out of an empty level earns all three stars")
    void cleanRunEarnsEverything() {
        GameMap map = LevelLoader.parse(List.of(
                "#######",
                "#@K..E#",
                "#######"));
        GameState state = new GameState(map, 4);

        for (int i = 0; i < 4; i++) {
            state.movePlayer(Direction.RIGHT);
        }

        assertTrue(state.hasEscaped());
        assertEquals(Score.MAX_STARS, state.score().stars());
    }
}
