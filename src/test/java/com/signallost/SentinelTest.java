package com.signallost;

import com.signallost.guard.Guard;
import com.signallost.guard.GuardFactory;
import com.signallost.guard.GuardMode;
import com.signallost.guard.GuardState;
import com.signallost.guard.Sentinel;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameMap;
import com.signallost.model.Position;
import com.signallost.vision.VisibilityService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sentinel sees a quarter of the room at a time and never leaves its post,
 * so the two things worth pinning down are the shape of the cone and the fact
 * that it still obeys walls.
 */
class SentinelTest {

    private static final Position FAR_AWAY = new Position(99, 99);

    /** An open room, so the cone is the only thing limiting sight. */
    private static GameMap room() {
        return LevelLoader.parse(List.of(
                "#########",
                "#.......#",
                "#.......#",
                "#.......#",
                "#.......#",
                "#.......#",
                "#########"));
    }

    private static void step(Guard guard, GameMap map) {
        guard.applyState(guard.decideTurn(map, FAR_AWAY));
    }

    // ------------------------------------------------------------- the cone

    @Test
    @DisplayName("It sees the quarter it faces and not the one behind it")
    void seesOnlyForwards() {
        GameMap map = room();
        Sentinel guard = new Sentinel("S", new Position(3, 4), Direction.UP);

        Set<Position> watched = guard.watchedTiles(map);

        assertTrue(watched.contains(new Position(2, 4)), "directly in front");
        assertTrue(watched.contains(new Position(1, 4)), "further in front");
        assertFalse(watched.contains(new Position(4, 4)), "directly behind");
        assertFalse(watched.contains(new Position(5, 4)), "further behind");
    }

    @Test
    @DisplayName("The cone widens by one tile for every tile of distance")
    void coneWidensWithDistance() {
        GameMap map = room();
        Sentinel guard = new Sentinel("S", new Position(4, 4), Direction.UP);

        Set<Position> watched = guard.watchedTiles(map);

        // One ahead: one to the side is on the edge of the wedge, two is outside.
        assertTrue(watched.contains(new Position(3, 5)));
        assertFalse(watched.contains(new Position(3, 6)));
        // Two ahead: the wedge has widened enough to take two to the side.
        assertTrue(watched.contains(new Position(2, 6)));
    }

    @Test
    @DisplayName("Turning to face a different quarter changes what it sees")
    void turningChangesWhatItSees() {
        GameMap map = room();
        Position post = new Position(3, 4);
        Position toTheRight = new Position(3, 6);

        Sentinel facingUp = new Sentinel("S", post, Direction.UP);
        assertFalse(facingUp.watchedTiles(map).contains(toTheRight));

        Sentinel facingRight = new Sentinel("S", post, Direction.RIGHT);
        assertTrue(facingRight.watchedTiles(map).contains(toTheRight));
    }

    @Test
    @DisplayName("The cone can only narrow what the flood fill already reaches")
    void coneNeverSeesMoreThanTheFloodFill() {
        GameMap map = LevelLoader.parse(List.of(
                "#######",
                "#.....#",
                "#.###.#",
                "#.....#",
                "#######"));
        Position post = new Position(3, 3);
        Sentinel guard = new Sentinel("S", post, Direction.UP);

        Set<Position> reachable =
                VisibilityService.visibleFrom(map, post, Sentinel.DEFAULT_SIGHT);

        for (Position watched : guard.watchedTiles(map)) {
            assertTrue(reachable.contains(watched),
                    watched + " is in the cone but not reachable by sight at all");
        }
    }

    @Test
    @DisplayName("A sealed room is never watched, whichever way it turns")
    void cannotSeeIntoASealedRoom() {
        GameMap map = LevelLoader.parse(List.of(
                "#########",
                "#...#...#",
                "#...#...#",
                "#...#...#",
                "#########"));
        Sentinel guard = new Sentinel("S", new Position(2, 2), Direction.RIGHT);
        Position otherRoom = new Position(2, 6);

        // Straight ahead in the wedge, and still behind a wall with no way round.
        for (int turn = 0; turn < 4; turn++) {
            assertFalse(guard.watchedTiles(map).contains(otherRoom),
                    "facing " + guard.state().facing() + ", it must not see through the wall");
            step(guard, map);
        }
    }

    // ---------------------------------------------------------- the rotation

    @Test
    @DisplayName("It sweeps a quarter-turn each turn and repeats after four")
    void sweepsFullCircle() {
        GameMap map = room();
        Sentinel guard = new Sentinel("S", new Position(3, 4), Direction.UP);

        step(guard, map);
        assertEquals(Direction.RIGHT, guard.state().facing());
        step(guard, map);
        assertEquals(Direction.DOWN, guard.state().facing());
        step(guard, map);
        assertEquals(Direction.LEFT, guard.state().facing());
        step(guard, map);
        assertEquals(Direction.UP, guard.state().facing(), "back where it started");
    }

    @Test
    @DisplayName("It never leaves its post")
    void neverMoves() {
        GameMap map = room();
        Position post = new Position(3, 4);
        Sentinel guard = new Sentinel("S", post, Direction.UP);

        for (int turn = 0; turn < 12; turn++) {
            step(guard, map);
            assertEquals(post, guard.position(), "a sentinel is fixed to its post");
        }
    }

    @Test
    @DisplayName("It never hunts, so it can never capture anybody")
    void neverHunts() {
        GameMap map = room();
        Sentinel guard = new Sentinel("S", new Position(3, 4), Direction.UP);

        // Even told the player is right beside it, and even after an alarm.
        guard.applyState(guard.onAlarmRaised(guard.state(), new Position(3, 5)));
        for (int turn = 0; turn < 10; turn++) {
            guard.applyState(guard.decideTurn(map, new Position(3, 5)));
            assertFalse(guard.mode() == GuardMode.HUNT,
                    "only a hunting guard captures, and a camera cannot hunt");
        }
    }

    // -------------------------------------------------------------- the alarm

    @Test
    @DisplayName("An alarm does not interrupt the sweep")
    void keepsSweepingThroughAnAlarm() {
        GameMap map = room();
        Sentinel guard = new Sentinel("S", new Position(3, 4), Direction.UP);

        // The alarm is raised again on every turn the player stays in sight, so
        // this is the realistic case, not an unusual one. A sentinel that broke
        // off to stare would never turn again.
        for (int turn = 0; turn < 8; turn++) {
            guard.applyState(guard.onAlarmRaised(guard.state(), new Position(5, 4)));
            step(guard, map);
        }

        assertEquals(GuardMode.PATROL, guard.mode(), "an alarm must not change its mode");
        assertEquals(Direction.UP, guard.state().facing(),
                "eight quarter-turns is two full sweeps, back to the start");
    }

    @Test
    @DisplayName("A sentinel under constant alarm still looks away three turns in four")
    void sweepKeepsLettingThePlayerPast() {
        GameMap map = room();
        Position hidingPlace = new Position(1, 4);          // only in the UP cone
        Sentinel guard = new Sentinel("S", new Position(3, 4), Direction.UP);

        int seen = 0;
        for (int turn = 0; turn < 8; turn++) {
            guard.applyState(guard.onAlarmRaised(guard.state(), hidingPlace));
            if (guard.watchedTiles(map).contains(hidingPlace)) {
                seen++;
            }
            step(guard, map);
        }

        assertEquals(2, seen, "over two full sweeps a given tile is watched twice, not eight times");
    }

    // ------------------------------------------------------------------ undo

    @Test
    @DisplayName("Putting an old state back restores the facing")
    void undoRestoresFacing() {
        GameMap map = room();
        Sentinel guard = new Sentinel("S", new Position(3, 4), Direction.UP);
        GuardState before = guard.state();
        Set<Position> watchedBefore = guard.watchedTiles(map);

        step(guard, map);
        assertFalse(guard.state().facing() == before.facing());

        guard.applyState(before);

        assertEquals(Direction.UP, guard.state().facing());
        assertEquals(watchedBefore, guard.watchedTiles(map),
                "undo must put the cone back exactly, or the fog would lie");
    }

    // --------------------------------------------------------------- loading

    @Test
    @DisplayName("A level file can place a sentinel and say which way it looks")
    void loadsFromALevelFile() {
        Guard guard = GuardFactory.fromSpecification("sentinel 5,7 left", 1);

        assertInstanceOf(Sentinel.class, guard);
        assertEquals(new Position(5, 7), guard.position());
        assertEquals(Direction.LEFT, guard.state().facing());
        assertEquals("Sentinel", guard.type());
    }

    @Test
    @DisplayName("A sentinel with no facing given starts looking up")
    void defaultsToFacingUp() {
        Guard guard = GuardFactory.fromSpecification("sentinel 5,7", 1);

        assertEquals(Direction.UP, guard.state().facing());
    }

    @Test
    @DisplayName("A facing that is not a direction is refused, not guessed at")
    void rejectsNonsenseFacing() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> GuardFactory.fromSpecification("sentinel 5,7 sideways", 1));

        assertTrue(thrown.getMessage().contains("sideways"),
                "the message should name the offending word");
    }

    @Test
    @DisplayName("The other guards are unaffected by the new word")
    void patrollersStillLoad() {
        Guard guard = GuardFactory.fromSpecification("patroller 1,1 1,7", 1);

        assertEquals(new Position(1, 1), guard.position());
        assertEquals("Patroller", guard.type());
    }
}
