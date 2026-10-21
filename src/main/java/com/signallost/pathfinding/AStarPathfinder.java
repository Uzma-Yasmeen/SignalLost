package com.signallost.pathfinding;

import com.signallost.model.GameMap;
import com.signallost.model.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.function.ToIntFunction;

/**
 * Shortest walkable route between two tiles, using A* with a priority queue.
 *
 * <p>A* is breadth-first search that guesses. Plain BFS, which the fog uses,
 * spreads evenly in every direction; A* keeps a queue ordered by
 * <em>cost so far + estimated cost remaining</em> and always expands the most
 * promising tile next, so it walks almost straight at the goal instead of
 * flooding the whole level.
 *
 * <p>The estimate is Manhattan distance. It never overestimates, because no
 * four-directional route can be shorter than the straight-line step count, and
 * that is exactly the condition under which A* is guaranteed to return a
 * shortest path rather than merely a path.
 *
 * <p>Ties are broken deterministically — closer to the goal first, then by row
 * and column — so the same board always produces the same route. Guards that
 * wander differently on every run would be untestable.
 */
public final class AStarPathfinder {

    /** A tile waiting in the open set, with its cost so far and total estimate. */
    private record Node(Position position, int costSoFar, int estimatedTotal) {
    }

    private static final Comparator<Node> CHEAPEST_FIRST =
            Comparator.comparingInt(Node::estimatedTotal)
                    .thenComparingInt(node -> node.estimatedTotal() - node.costSoFar())
                    .thenComparingInt(node -> node.position().row())
                    .thenComparingInt(node -> node.position().col());

    private AStarPathfinder() {
    }

    /**
     * The shortest route from {@code start} to {@code goal}, including both ends.
     *
     * @return the tiles to walk in order, or an empty list when the goal cannot
     *         be reached, is a wall, or lies off the map
     */
    public static List<Position> findPath(GameMap map, Position start, Position goal) {
        return findPath(map, start, goal, tile -> 0);
    }

    /**
     * The same search, but where some tiles cost more to walk than others.
     *
     * <p>The hint system uses this to prefer a longer route that stays out of the
     * guards' sight over a short one that walks straight through it. Every step
     * still costs at least 1 and the surcharge is never negative, so Manhattan
     * distance remains an underestimate and the result is still a genuinely
     * cheapest route rather than a guess.
     *
     * @param extraCost additional cost for stepping onto a tile, 0 for none
     */
    public static List<Position> findPath(GameMap map, Position start, Position goal,
                                          ToIntFunction<Position> extraCost) {
        if (!map.isWalkable(start) || !map.isWalkable(goal)) {
            return List.of();
        }
        if (start.equals(goal)) {
            return List.of(start);
        }

        Map<Position, Integer> cheapestKnown = new HashMap<>();
        Map<Position, Position> arrivedFrom = new HashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>(CHEAPEST_FIRST);

        cheapestKnown.put(start, 0);
        open.add(new Node(start, 0, estimate(start, goal)));

        while (!open.isEmpty()) {
            Node current = open.remove();

            if (current.position().equals(goal)) {
                return reconstruct(arrivedFrom, goal);
            }
            // A cheaper route to this tile was queued after it, so skip the stale copy.
            if (current.costSoFar() > cheapestKnown.getOrDefault(current.position(), Integer.MAX_VALUE)) {
                continue;
            }

            for (Position neighbour : map.walkableNeighbours(current.position())) {
                int costToNeighbour = current.costSoFar() + 1 + extraCost.applyAsInt(neighbour);
                if (costToNeighbour >= cheapestKnown.getOrDefault(neighbour, Integer.MAX_VALUE)) {
                    continue;
                }
                cheapestKnown.put(neighbour, costToNeighbour);
                arrivedFrom.put(neighbour, current.position());
                open.add(new Node(neighbour, costToNeighbour,
                        costToNeighbour + estimate(neighbour, goal)));
            }
        }
        return List.of();
    }

    /**
     * The single tile to step onto when heading for {@code goal}.
     *
     * <p>This is what a guard actually asks for: it recomputes the route every
     * turn, so a chase keeps up with a player who changes direction.
     *
     * @return the next tile, or empty when there is no route or the guard is
     *         already standing on the goal
     */
    public static Optional<Position> nextStepTowards(GameMap map, Position start, Position goal) {
        List<Position> path = findPath(map, start, goal);
        return path.size() < 2 ? Optional.empty() : Optional.of(path.get(1));
    }

    /** Manhattan distance: the optimistic guess that keeps A* honest. */
    private static int estimate(Position from, Position to) {
        return from.manhattanDistanceTo(to);
    }

    private static List<Position> reconstruct(Map<Position, Position> arrivedFrom, Position goal) {
        List<Position> path = new ArrayList<>();
        Position step = goal;
        while (step != null) {
            path.add(step);
            step = arrivedFrom.get(step);
        }
        Collections.reverse(path);
        return path;
    }
}
