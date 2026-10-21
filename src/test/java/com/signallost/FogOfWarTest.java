package com.signallost;

import com.signallost.level.LevelLoader;
import com.signallost.model.GameMap;
import com.signallost.model.Position;
import com.signallost.vision.FogOfWar;
import com.signallost.vision.TileVisibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the memory that accumulates on top of the BFS visibility. */
class FogOfWarTest {

    private static GameMap corridor() {
        return LevelLoader.parse(List.of(
                "#########",
                "#.......#",
                "#########"));
    }

    @Test
    @DisplayName("Tiles never in sight stay unexplored")
    void unseenTilesAreUnexplored() {
        FogOfWar fog = new FogOfWar(corridor(), 2);
        fog.update(new Position(1, 1));

        assertEquals(TileVisibility.UNEXPLORED, fog.visibilityOf(new Position(1, 7)));
    }

    @Test
    @DisplayName("A tile you walk away from becomes remembered, not unexplored")
    void leftBehindTilesAreRemembered() {
        FogOfWar fog = new FogOfWar(corridor(), 1);
        Position start = new Position(1, 1);

        fog.update(start);
        assertEquals(TileVisibility.VISIBLE, fog.visibilityOf(start));

        // Walk three tiles to the right, out of sight of the start.
        fog.update(new Position(1, 4));

        assertEquals(TileVisibility.REMEMBERED, fog.visibilityOf(start));
        assertEquals(TileVisibility.VISIBLE, fog.visibilityOf(new Position(1, 4)));
    }

    @Test
    @DisplayName("Explored tiles accumulate and are never forgotten")
    void exploredSetOnlyGrows() {
        FogOfWar fog = new FogOfWar(corridor(), 1);

        fog.update(new Position(1, 1));
        int afterFirstStep = fog.exploredTiles().size();

        fog.update(new Position(1, 5));

        assertTrue(fog.exploredTiles().size() > afterFirstStep);
        assertTrue(fog.exploredTiles().contains(new Position(1, 1)));
    }

    @Test
    @DisplayName("The developer toggle reveals the whole map")
    void revealAllShowsEverything() {
        GameMap map = corridor();
        FogOfWar fog = new FogOfWar(map, 1);
        fog.update(new Position(1, 1));

        fog.toggleRevealAll();

        assertTrue(fog.isRevealAll());
        for (int row = 0; row < map.rows(); row++) {
            for (int col = 0; col < map.cols(); col++) {
                assertEquals(TileVisibility.VISIBLE, fog.visibilityOf(new Position(row, col)));
            }
        }
    }
}
