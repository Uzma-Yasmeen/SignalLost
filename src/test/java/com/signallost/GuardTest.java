package com.signallost;

import com.signallost.guard.Guard;
import com.signallost.guard.GuardMode;
import com.signallost.guard.GuardState;
import com.signallost.guard.Hunter;
import com.signallost.guard.Patroller;
import com.signallost.level.LevelLoader;
import com.signallost.model.GameMap;
import com.signallost.model.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for how the two guard types decide their turns. */
class GuardTest {

    private static final Position FAR_AWAY = new Position(99, 99);

    private static GameMap corridor() {
        return LevelLoader.parse(List.of(
                "#########",
                "#.......#",
                "#########"));
    }

    /** Runs a guard's turn and applies the result, as the turn system would. */
    private static void step(Guard guard, GameMap map, Position player) {
        guard.applyState(guard.decideTurn(map, player));
    }

    // ------------------------------------------------------------ patroller

    @Test
    @DisplayName("A patroller walks towards its next waypoint")
    void patrollerWalksItsRoute() {
        GameMap map = corridor();
        Patroller guard = new Patroller("P", new Position(1, 1),
                List.of(new Position(1, 1), new Position(1, 7)));

        step(guard, map, FAR_AWAY);

        assertEquals(new Position(1, 2), guard.position());
        assertEquals(GuardMode.PATROL, guard.mode());
    }

    @Test
    @DisplayName("A patroller turns round at the end and comes back")
    void patrollerLoopsItsRoute() {
        GameMap map = corridor();
        Patroller guard = new Patroller("P", new Position(1, 1),
                List.of(new Position(1, 1), new Position(1, 4)));

        // Three steps east reaches the far waypoint.
        for (int i = 0; i < 3; i++) {
            step(guard, map, FAR_AWAY);
        }
        assertEquals(new Position(1, 4), guard.position());

        // The next turn should start heading back, not stall.
        step(guard, map, FAR_AWAY);
        assertEquals(new Position(1, 3), guard.position());
    }

    @Test
    @DisplayName("A patroller ignores the player even when standing in its sight")
    void patrollerNeverReacts() {
        GameMap map = corridor();
        Patroller guard = new Patroller("P", new Position(1, 1),
                List.of(new Position(1, 1), new Position(1, 7)));
        Position playerInPlainView = new Position(1, 3);

        assertTrue(guard.canSee(map, playerInPlainView), "it really can see them");
        step(guard, map, playerInPlainView);

        assertEquals(GuardMode.PATROL, guard.mode(), "but it does not care");
    }

    @Test
    @DisplayName("A guard with no route holds its post")
    void routelessGuardStandsStill() {
        GameMap map = corridor();
        Patroller guard = new Patroller("P", new Position(1, 4), List.of(new Position(1, 4)));

        step(guard, map, FAR_AWAY);

        assertEquals(new Position(1, 4), guard.position());
    }

    // --------------------------------------------------------------- hunter

    @Test
    @DisplayName("A hunter that sees the player covers two tiles a turn")
    void hunterChasesWhatItSees() {
        GameMap map = corridor();
        Hunter guard = new Hunter("H", new Position(1, 1), List.of(new Position(1, 1)));
        Position player = new Position(1, 4);

        step(guard, map, player);

        assertEquals(GuardMode.HUNT, guard.mode());
        assertEquals(new Position(1, 3), guard.position(),
                "a hunting guard is faster than the player");
    }

    @Test
    @DisplayName("You cannot outrun a hunter: running in a straight line gets you caught")
    void runningAwayDoesNotWork() {
        GameMap map = LevelLoader.parse(List.of(
                "###############",
                "#.............#",
                "###############"));
        Hunter guard = new Hunter("H", new Position(1, 1), List.of(new Position(1, 1)));

        // The player flees east, one tile a turn, starting three tiles ahead.
        Position player = new Position(1, 4);
        for (int turn = 0; turn < 6 && !guard.position().equals(player); turn++) {
            player = new Position(1, Math.min(13, player.col() + 1));
            step(guard, map, player);
        }

        assertEquals(player, guard.position(), "it should have run the player down");
    }

    @Test
    @DisplayName("Breaking line of sight is the way out: a searching guard slows to walking pace")
    void losingSightSlowsTheHunter() {
        GameMap map = corridor();
        Hunter guard = new Hunter("H", new Position(1, 1), List.of(new Position(1, 1)));

        step(guard, map, new Position(1, 5));
        assertEquals(GuardMode.HUNT, guard.mode());
        Position whileHunting = guard.position();

        // Out of sight. It drops to searching, and searching is one tile a turn.
        step(guard, map, FAR_AWAY);

        assertEquals(GuardMode.ALERT, guard.mode());
        assertEquals(1, guard.position().manhattanDistanceTo(whileHunting),
                "searching covers one tile, not two");
    }

    @Test
    @DisplayName("A hunter cannot see through a wall")
    void wallsHideThePlayerFromAHunter() {
        GameMap map = LevelLoader.parse(List.of(
                "#########",
                "#..#....#",
                "#########"));
        Hunter guard = new Hunter("H", new Position(1, 1), List.of(new Position(1, 1)));
        Position playerBehindWall = new Position(1, 5);

        assertFalse(guard.canSee(map, playerBehindWall));
        step(guard, map, playerBehindWall);

        assertNotEquals(GuardMode.HUNT, guard.mode());
    }

    @Test
    @DisplayName("Losing sight sends a hunter to where the player last was")
    void hunterSearchesTheLastKnownTile() {
        GameMap map = corridor();
        Hunter guard = new Hunter("H", new Position(1, 1), List.of(new Position(1, 1)));

        // Seen at (1,5): the hunter locks on and remembers the tile.
        step(guard, map, new Position(1, 5));
        assertEquals(GuardMode.HUNT, guard.mode());

        // The player vanishes off the map. The hunter should search, not give up.
        step(guard, map, FAR_AWAY);

        assertEquals(GuardMode.ALERT, guard.mode());
        assertEquals(new Position(1, 5), guard.state().lastSeenPlayer());
        assertEquals(new Position(1, 4), guard.position(), "still heading for the last sighting");
    }

    @Test
    @DisplayName("A hunter gives up once it reaches the last known tile and finds nobody")
    void hunterReturnsToPatrolAfterSearching() {
        GameMap map = corridor();
        Hunter guard = new Hunter("H", new Position(1, 1), List.of(new Position(1, 1)));

        step(guard, map, new Position(1, 3));
        assertEquals(GuardMode.HUNT, guard.mode());

        // Walk it to the remembered tile with the player out of sight throughout.
        for (int i = 0; i < 6; i++) {
            step(guard, map, FAR_AWAY);
        }

        assertEquals(GuardMode.PATROL, guard.mode());
    }

    @Test
    @DisplayName("Seeing the player again during a search restarts the chase")
    void huntResumesWhenThePlayerReappears() {
        GameMap map = corridor();
        Hunter guard = new Hunter("H", new Position(1, 1), List.of(new Position(1, 1)));

        step(guard, map, new Position(1, 4));
        step(guard, map, FAR_AWAY);
        assertEquals(GuardMode.ALERT, guard.mode());

        step(guard, map, new Position(1, 5));

        assertEquals(GuardMode.HUNT, guard.mode());
    }

    @Test
    @DisplayName("A guard's state is a value, so it can be put back exactly")
    void guardStateCanBeRestored() {
        GameMap map = corridor();
        Hunter guard = new Hunter("H", new Position(1, 1), List.of(new Position(1, 1)));
        GuardState before = guard.state();

        step(guard, map, new Position(1, 4));
        assertNotEquals(before, guard.state());

        guard.applyState(before);

        assertEquals(before, guard.state());
        assertEquals(GuardMode.PATROL, guard.mode());
    }
}
