package com.signallost;

import com.signallost.level.LevelCatalog;
import com.signallost.level.LevelLoader;
import com.signallost.model.GameMap;
import com.signallost.model.Position;
import com.signallost.model.TileType;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that finding the exit is part of the game.
 *
 * <p>Every level once put its exit one tile in from the bottom wall and one in
 * from the right, so the last leg of all ten was the same leg: collect the
 * keys, then walk diagonally. A player said it was not exciting. The fog is
 * supposed to make a level something you learn, and an exit you can assume the
 * position of without looking is a room of the map nobody needs to explore.
 */
class ExitPlacementTest {

    /** How close to a wall still counts as hugging it. */
    private static final int WALL_MARGIN = 2;

    @Test
    void noExitIsTuckedIntoACorner() {
        List<String> corners = new ArrayList<>();

        for (LevelCatalog.Entry entry : LevelCatalog.load()) {
            GameMap map = LevelLoader.loadFromResource(entry.name()).map();
            Position exit = map.findFirst(TileType.EXIT);

            boolean nearTopOrBottom = exit.row() <= WALL_MARGIN
                    || exit.row() >= map.rows() - 1 - WALL_MARGIN;
            boolean nearLeftOrRight = exit.col() <= WALL_MARGIN
                    || exit.col() >= map.cols() - 1 - WALL_MARGIN;
            if (nearTopOrBottom && nearLeftOrRight) {
                corners.add(entry.name() + " at " + exit);
            }
        }

        assertTrue(corners.isEmpty(),
                "These exits sit in a corner, where a player can head without looking:\n  "
                        + String.join("\n  ", corners));
    }

    @Test
    void theExitsAreNotAllInTheSamePlace() {
        Set<Position> places = new HashSet<>();
        for (LevelCatalog.Entry entry : LevelCatalog.load()) {
            GameMap map = LevelLoader.loadFromResource(entry.name()).map();
            places.add(map.findFirst(TileType.EXIT));
        }

        // Boards differ in size, so identical coordinates are not required to
        // be identical places — but ten levels sharing a handful of spots means
        // the exit has a usual location, which is the thing being fixed.
        assertTrue(places.size() >= 7,
                "Ten levels put their exits in only " + places.size() + " distinct places: "
                        + places + ". Once a player knows where the exit usually is, the fog"
                        + " stops hiding anything that matters.");
    }
}
