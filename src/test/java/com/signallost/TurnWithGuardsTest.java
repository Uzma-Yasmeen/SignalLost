package com.signallost;

import com.signallost.guard.Guard;
import com.signallost.guard.GuardMode;
import com.signallost.guard.Hunter;
import com.signallost.guard.Patroller;
import com.signallost.level.Level;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameMap;
import com.signallost.model.GameState;
import com.signallost.model.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that guards take their turn inside the player's, and that undo rewinds
 * the whole turn rather than only the player's half.
 */
class TurnWithGuardsTest {

    private static GameMap hall() {
        return LevelLoader.parse(List.of(
                "##########",
                "#@.......#",
                "#........#",
                "#.......E#",
                "##########"));
    }

    @Test
    @DisplayName("Guards move after the player, within the same turn")
    void guardsTakeTheirTurnWithThePlayer() {
        Patroller guard = new Patroller("P", new Position(3, 1),
                List.of(new Position(3, 1), new Position(3, 7)));
        GameState state = new GameState(hall(), List.of(guard), 3);

        state.movePlayer(Direction.RIGHT);

        assertEquals(new Position(1, 2), state.player());
        assertEquals(new Position(3, 2), guard.position(), "the guard also stepped");
        assertEquals(1, state.turnsTaken(), "but it is still one turn");
    }

    @Test
    @DisplayName("A refused player move does not let the guards move either")
    void blockedMoveStopsTheWholeTurn() {
        Patroller guard = new Patroller("P", new Position(3, 1),
                List.of(new Position(3, 1), new Position(3, 7)));
        GameState state = new GameState(hall(), List.of(guard), 3);
        Position guardStart = guard.position();

        assertFalse(state.movePlayer(Direction.UP), "a wall is above the player");

        assertEquals(guardStart, guard.position());
        assertFalse(state.canUndo());
    }

    @Test
    @DisplayName("Undo rewinds the guards as well as the player")
    void undoRewindsGuardsToo() {
        Patroller guard = new Patroller("P", new Position(3, 1),
                List.of(new Position(3, 1), new Position(3, 7)));
        GameState state = new GameState(hall(), List.of(guard), 3);
        Position playerStart = state.player();
        Position guardStart = guard.position();

        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.RIGHT);
        assertNotEquals(guardStart, guard.position());

        state.undoLastMove();
        state.undoLastMove();

        assertEquals(playerStart, state.player());
        assertEquals(guardStart, guard.position());
        assertEquals(0, state.turnsTaken());
    }

    @Test
    @DisplayName("Undo rewinds a hunter's mode, not just its position")
    void undoRewindsGuardMode() {
        // Far enough that it spots the player and gives chase without reaching
        // them: a capture would end the attempt and undo would rightly refuse.
        Hunter hunter = new Hunter("H", new Position(1, 6), List.of(new Position(1, 6)));
        GameState state = new GameState(hall(), List.of(hunter), 3);

        assertEquals(GuardMode.PATROL, hunter.mode());
        state.movePlayer(Direction.RIGHT);
        assertEquals(GuardMode.HUNT, hunter.mode(), "it should have spotted the player");

        state.undoLastMove();

        assertEquals(GuardMode.PATROL, hunter.mode(), "undo takes the alarm back too");
    }

    @Test
    @DisplayName("Undoing many turns of several guards leaves them exactly where they began")
    void undoRewindsEveryGuardOverManyTurns() {
        // Two patrollers: neither can capture, so the run survives long enough
        // for the point of this test, which is that undo rewinds all of them.
        Guard patroller = new Patroller("P1", new Position(3, 1),
                List.of(new Position(3, 1), new Position(3, 8)));
        Guard hunter = new Patroller("P2", new Position(2, 8),
                List.of(new Position(2, 8), new Position(2, 2)));
        GameState state = new GameState(hall(), List.of(patroller, hunter), 3);

        Position patrollerStart = patroller.position();
        Position hunterStart = hunter.position();

        for (int i = 0; i < 5; i++) {
            state.movePlayer(Direction.RIGHT);
        }
        while (state.canUndo()) {
            state.undoLastMove();
        }

        assertEquals(patrollerStart, patroller.position());
        assertEquals(hunterStart, hunter.position());
        assertEquals(GuardMode.PATROL, hunter.mode());
        assertEquals(0, state.turnsTaken());
    }

    @Test
    @DisplayName("The level file puts the guards it names into the game")
    void levelFileSuppliesGuards() {
        Level level = LevelLoader.loadFromResource("level1");

        assertTrue(level.guards().stream().anyMatch(g -> g.type().equals("Hunter")),
                "level 1 should introduce a hunter");
        assertTrue(level.guards().stream().anyMatch(g -> g.type().equals("Patroller")),
                "level 1 should introduce a patroller");
        level.guards().forEach(g ->
                assertTrue(level.map().isWalkable(g.position()),
                        g.name() + " spawned inside a wall"));
    }

    @Test
    @DisplayName("A guard posted inside a wall is rejected when the level loads")
    void guardInAWallIsRejectedAtLoadTime() {
        List<String> file = List.of(
                "#####",
                "#@.E#",
                "#####",
                "---",
                "patroller 0,0");

        try {
            LevelLoader.parseLevel("broken", file);
            throw new AssertionError("expected the bad waypoint to be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("not walkable"));
        }
    }
}
