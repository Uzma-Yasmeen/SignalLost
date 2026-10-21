package com.signallost.guard;

import com.signallost.model.GameMap;
import com.signallost.model.Position;
import com.signallost.pathfinding.AStarPathfinder;

/**
 * Closes in on the player by the shortest route.
 *
 * <p>The path is recomputed every single turn rather than walked once, which is
 * what makes a chase feel alive: doubling back does not shake the guard off,
 * because next turn it simply solves the new, shorter problem.
 */
public final class HuntBehaviour implements GuardBehaviour {

    public static final HuntBehaviour INSTANCE = new HuntBehaviour();

    private HuntBehaviour() {
    }

    @Override
    public GuardMode mode() {
        return GuardMode.HUNT;
    }

    @Override
    public GuardState takeTurn(Guard guard, GuardState current, GameMap map, Position player) {
        Position step = AStarPathfinder.nextStepTowards(map, current.position(), player)
                .orElse(current.position());

        // The player's tile is recorded every turn, so the moment sight breaks
        // the guard already knows the freshest place to start searching.
        return new GuardState(step, GuardMode.HUNT, current.routeIndex(), player, 0);
    }
}
