package com.signallost;

import com.signallost.guard.GuardMode;
import com.signallost.guard.Hunter;
import com.signallost.guard.Patroller;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameMap;
import com.signallost.model.GameState;
import com.signallost.model.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for what happens when a guard sees you.
 *
 * <p>This is the answer to the fair complaint that being spotted appeared to do
 * nothing. A sighting is now broadcast, and every hunter on the map answers it,
 * so walking past the dumbest guard in the building has a cost.
 */
class AlarmTest {

    /**
     * A long hall. The player starts at the west end next to a patroller; a
     * hunter sits at the far east end, much too far away to see anything.
     */
    private static GameMap hall() {
        return LevelLoader.parse(List.of(
                "###############",
                "#@...........E#",
                "###############"));
    }

    private static Patroller sentryAt(int col) {
        Position post = new Position(1, col);
        return new Patroller("P", post, List.of(post));
    }

    private static Hunter hunterAt(int col) {
        Position post = new Position(1, col);
        return new Hunter("H", post, List.of(post));
    }

    @Test
    @DisplayName("A patroller cannot chase, but seeing you sets the hunters moving")
    void sightingByAPatrollerAlertsTheHunters() {
        Patroller sentry = sentryAt(4);
        Hunter hunter = hunterAt(13);
        GameState state = new GameState(hall(), List.of(sentry, hunter), 4);

        assertEquals(GuardMode.PATROL, hunter.mode());
        assertTrue(hunter.canSee(state.map(), state.player()) == false,
                "the hunter is far too far away to see anything itself");

        state.movePlayer(Direction.RIGHT);

        assertTrue(state.wasSpotted(), "the patroller sees the player at (1,2)");
        assertEquals(GuardMode.ALERT, hunter.mode(),
                "the hunter should answer the alarm from across the map");
        assertEquals(new Position(1, 2), hunter.state().lastSeenPlayer());
    }

    @Test
    @DisplayName("The patroller that raised the alarm still does not chase")
    void theAlarmDoesNotChangeAPatroller() {
        Patroller sentry = sentryAt(4);
        GameState state = new GameState(hall(), List.of(sentry), 4);
        Position post = sentry.position();

        state.movePlayer(Direction.RIGHT);

        assertTrue(state.wasSpotted());
        assertEquals(GuardMode.PATROL, sentry.mode(), "a patroller has no answer to an alarm");
        assertEquals(post, sentry.position());
    }

    @Test
    @DisplayName("An alerted hunter walks towards where the player was reported")
    void alertedHunterClosesIn() {
        Hunter hunter = hunterAt(13);
        GameState state = new GameState(hall(), List.of(sentryAt(4), hunter), 4);

        state.movePlayer(Direction.RIGHT);
        Position afterAlarm = hunter.position();

        state.movePlayer(Direction.WAIT);

        assertTrue(hunter.position().col() < afterAlarm.col(),
                "it should be heading west, towards the sighting");
    }

    @Test
    @DisplayName("Undo calls the alarm off")
    void undoTakesTheAlarmBack() {
        Hunter hunter = hunterAt(13);
        GameState state = new GameState(hall(), List.of(sentryAt(4), hunter), 4);

        state.movePlayer(Direction.RIGHT);
        assertEquals(GuardMode.ALERT, hunter.mode());

        state.undoLastMove();

        assertEquals(GuardMode.PATROL, hunter.mode(),
                "the facility should not stay alerted about a sighting that never happened");
        assertEquals(new Position(1, 13), hunter.position());
    }

    @Test
    @DisplayName("Several turns of alarms all unwind cleanly")
    void repeatedAlarmsUndoCleanly() {
        Hunter hunter = hunterAt(13);
        GameState state = new GameState(hall(), List.of(sentryAt(4), hunter), 4);
        Position hunterStart = hunter.position();

        for (int i = 0; i < 3; i++) {
            state.movePlayer(Direction.RIGHT);
        }
        assertNotEquals(hunterStart, hunter.position());

        while (state.canUndo()) {
            state.undoLastMove();
        }

        assertEquals(hunterStart, hunter.position());
        assertEquals(GuardMode.PATROL, hunter.mode());
        assertEquals(0, state.sightings());
    }

    @Test
    @DisplayName("A hunter that can already see you ignores the report")
    void huntingBeatsHearsay() {
        // The hunter is close enough to spot the player itself.
        Hunter hunter = hunterAt(4);
        GameState state = new GameState(hall(), List.of(hunter), 4);

        state.movePlayer(Direction.RIGHT);

        assertEquals(GuardMode.HUNT, hunter.mode(),
                "its own eyes are better information than the alarm");
    }
}
