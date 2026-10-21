package com.signallost.guard;

import com.signallost.model.GameMap;
import com.signallost.model.Position;
import com.signallost.pathfinding.AStarPathfinder;

import java.util.List;

/**
 * Walks the guard's route, one waypoint at a time, looping back to the start.
 *
 * <p>The route is a handful of corners rather than every tile of the circuit:
 * A* fills in the walking between them. That means a level author marks four
 * points instead of forty, and a route still works if the level's walls move.
 */
public final class PatrolBehaviour implements GuardBehaviour {

    public static final PatrolBehaviour INSTANCE = new PatrolBehaviour();

    private PatrolBehaviour() {
    }

    @Override
    public GuardMode mode() {
        return GuardMode.PATROL;
    }

    @Override
    public GuardState takeTurn(Guard guard, GuardState current, GameMap map, Position player) {
        List<Position> route = guard.route();
        if (route.isEmpty()) {
            // A guard with no route simply holds its post.
            return current.withMode(GuardMode.PATROL);
        }

        int waypoint = Math.floorMod(current.routeIndex(), route.size());
        // Standing on the current waypoint means it is time to head for the next.
        if (current.position().equals(route.get(waypoint))) {
            waypoint = (waypoint + 1) % route.size();
        }
        Position target = route.get(waypoint);
        Position step = AStarPathfinder.nextStepTowards(map, current.position(), target)
                .orElse(current.position());

        return new GuardState(step, GuardMode.PATROL, waypoint, current.lastSeenPlayer(), 0);
    }
}
