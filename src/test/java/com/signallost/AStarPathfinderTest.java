package com.signallost;

import com.signallost.level.LevelLoader;
import com.signallost.model.GameMap;
import com.signallost.model.Position;
import com.signallost.pathfinding.AStarPathfinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the A* route finding the guards chase with. */
class AStarPathfinderTest {

    @Test
    @DisplayName("A straight corridor gives a path of exactly the right length")
    void straightCorridorIsShortest() {
        GameMap map = LevelLoader.parse(List.of(
                "#######",
                "#.....#",
                "#######"));

        List<Position> path = AStarPathfinder.findPath(
                map, new Position(1, 1), new Position(1, 5));

        // Five tiles: the start, three steps, and the goal.
        assertEquals(5, path.size());
        assertEquals(new Position(1, 1), path.get(0));
        assertEquals(new Position(1, 5), path.get(path.size() - 1));
    }

    @Test
    @DisplayName("Every step of a path is next to the one before it")
    void pathIsContinuous() {
        GameMap map = LevelLoader.parse(List.of(
                "#########",
                "#...#...#",
                "#.#.#.#.#",
                "#.#...#.#",
                "#.#####.#",
                "#.......#",
                "#########"));

        List<Position> path = AStarPathfinder.findPath(
                map, new Position(1, 1), new Position(1, 7));

        assertTrue(path.size() > 1, "the goal should be reachable");
        for (int i = 1; i < path.size(); i++) {
            assertEquals(1, path.get(i).manhattanDistanceTo(path.get(i - 1)),
                    "step " + i + " jumped");
            assertTrue(map.isWalkable(path.get(i)), "step " + i + " went through a wall");
        }
    }

    @Test
    @DisplayName("A wall in the way makes the route go round, not through")
    void routeGoesAroundAWall() {
        //                            col: 01234
        GameMap map = LevelLoader.parse(List.of(
                "#####",
                "#...#",
                "#.#.#",
                "#...#",
                "#####"));

        List<Position> path = AStarPathfinder.findPath(
                map, new Position(1, 1), new Position(3, 1));

        assertEquals(3, path.size(), "straight down the left-hand side is two steps");
        assertTrue(path.stream().noneMatch(p -> p.equals(new Position(2, 2))));
    }

    @Test
    @DisplayName("A sealed goal gives no path at all")
    void unreachableGoalGivesEmptyPath() {
        GameMap map = LevelLoader.parse(List.of(
                ".......",
                ".#####.",
                ".#...#.",
                ".#####.",
                "......."));

        List<Position> path = AStarPathfinder.findPath(
                map, new Position(0, 0), new Position(2, 3));

        assertTrue(path.isEmpty());
    }

    @Test
    @DisplayName("A wall as the goal gives no path")
    void wallGoalGivesEmptyPath() {
        GameMap map = LevelLoader.parse(List.of(
                "#####",
                "#...#",
                "#####"));

        assertTrue(AStarPathfinder.findPath(map, new Position(1, 1), new Position(0, 0)).isEmpty());
    }

    @Test
    @DisplayName("Standing on the goal is a path of one tile and no next step")
    void alreadyAtTheGoal() {
        GameMap map = LevelLoader.parse(List.of(
                "#####",
                "#...#",
                "#####"));
        Position here = new Position(1, 2);

        assertEquals(List.of(here), AStarPathfinder.findPath(map, here, here));
        assertTrue(AStarPathfinder.nextStepTowards(map, here, here).isEmpty());
    }

    @Test
    @DisplayName("The next step moves one tile closer to the goal")
    void nextStepClosesTheGap() {
        GameMap map = LevelLoader.parse(List.of(
                "#######",
                "#.....#",
                "#######"));
        Position start = new Position(1, 1);
        Position goal = new Position(1, 5);

        Optional<Position> step = AStarPathfinder.nextStepTowards(map, start, goal);

        assertTrue(step.isPresent());
        assertEquals(new Position(1, 2), step.get());
    }

    @Test
    @DisplayName("The same board always produces the same route")
    void routesAreDeterministic() {
        GameMap map = LevelLoader.parse(List.of(
                "#######",
                "#.....#",
                "#.....#",
                "#.....#",
                "#######"));
        Position start = new Position(1, 1);
        Position goal = new Position(3, 5);

        List<Position> first = AStarPathfinder.findPath(map, start, goal);
        List<Position> second = AStarPathfinder.findPath(map, start, goal);

        assertEquals(first, second, "a guard that wanders differently each run is untestable");
    }
}
