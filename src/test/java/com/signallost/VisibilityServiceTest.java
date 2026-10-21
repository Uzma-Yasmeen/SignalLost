package com.signallost;

import com.signallost.level.LevelLoader;
import com.signallost.model.GameMap;
import com.signallost.model.Position;
import com.signallost.vision.VisibilityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the BFS flood fill behind the fog of war. */
class VisibilityServiceTest {

    @Test
    @DisplayName("A radius of zero sees only the tile you stand on")
    void radiusZeroSeesOnlyOrigin() {
        GameMap map = LevelLoader.parse(List.of(
                "#####",
                "#...#",
                "#####"));
        Position origin = new Position(1, 2);

        Set<Position> visible = VisibilityService.visibleFrom(map, origin, 0);

        assertEquals(Set.of(origin), visible);
    }

    @Test
    @DisplayName("In an open room, sight reaches every tile within the radius")
    void openRoomSpreadsInAllDirections() {
        GameMap map = LevelLoader.parse(List.of(
                ".....",
                ".....",
                ".....",
                ".....",
                "....."));
        Position origin = new Position(2, 2);

        Set<Position> visible = VisibilityService.visibleFrom(map, origin, 2);

        // Every in-bounds tile within 2 Manhattan steps, and nothing further.
        for (int row = 0; row < map.rows(); row++) {
            for (int col = 0; col < map.cols(); col++) {
                Position p = new Position(row, col);
                boolean withinRange = p.manhattanDistanceTo(origin) <= 2;
                assertEquals(withinRange, visible.contains(p), "wrong visibility for " + p);
            }
        }
    }

    @Test
    @DisplayName("A wall is seen but blocks the tile behind it")
    void wallIsVisibleButOpaque() {
        //                            col: 0123456
        GameMap map = LevelLoader.parse(List.of(
                ".......",
                "...#...",
                "......."));
        Position origin = new Position(1, 1);
        Position wall = new Position(1, 3);
        Position behindWall = new Position(1, 4);

        Set<Position> visible = VisibilityService.visibleFrom(map, origin, 2);

        assertTrue(visible.contains(wall), "the face of an adjacent wall is visible");
        assertFalse(visible.contains(behindWall), "the tile behind a wall is hidden");
    }

    @Test
    @DisplayName("Sight follows a corridor instead of passing through its walls")
    void sightBendsAroundACorner() {
        //                            col: 01234
        GameMap map = LevelLoader.parse(List.of(
                "#####",   // row 0
                "#...#",   // row 1  corridor running right
                "###.#",   // row 2  it turns downward at col 3
                "#...#",   // row 3  and continues left
                "#####")); // row 4
        Position origin = new Position(1, 1);

        // Four steps is exactly enough to walk right 2, then down 2, along the corridor.
        Set<Position> visible = VisibilityService.visibleFrom(map, origin, 4);

        assertTrue(visible.contains(new Position(1, 3)), "end of the first leg");
        assertTrue(visible.contains(new Position(2, 3)), "the corner itself");
        assertTrue(visible.contains(new Position(3, 3)), "start of the second leg");
        assertFalse(visible.contains(new Position(3, 1)),
                "the far end of the second leg is 6 steps away, out of range");
    }

    @Test
    @DisplayName("A sealed room cannot be seen into from outside")
    void sealedRoomStaysHidden() {
        GameMap map = LevelLoader.parse(List.of(
                ".......",
                ".#####.",
                ".#...#.",
                ".#####.",
                "......."));
        Position origin = new Position(0, 0);

        Set<Position> visible = VisibilityService.visibleFrom(map, origin, 20);

        assertFalse(visible.contains(new Position(2, 2)), "inside of the sealed room");
        assertFalse(visible.contains(new Position(2, 3)), "inside of the sealed room");
    }

    @Test
    @DisplayName("An origin outside the map sees nothing")
    void originOutsideMapSeesNothing() {
        GameMap map = LevelLoader.parse(List.of("..."));

        assertTrue(VisibilityService.visibleFrom(map, new Position(-1, 0), 3).isEmpty());
    }

    @Test
    @DisplayName("A negative radius is rejected")
    void negativeRadiusIsRejected() {
        GameMap map = LevelLoader.parse(List.of("..."));

        assertThrows(IllegalArgumentException.class,
                () -> VisibilityService.visibleFrom(map, new Position(0, 0), -1));
    }
}
