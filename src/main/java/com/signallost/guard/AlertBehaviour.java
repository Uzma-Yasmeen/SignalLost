package com.signallost.guard;

import com.signallost.model.GameMap;
import com.signallost.model.Position;
import com.signallost.pathfinding.AStarPathfinder;

/**
 * Heads for the tile where the player was last seen, and gives up after a while.
 *
 * <p>This is the behaviour that makes hiding worth doing. Without it a guard
 * would snap back to its route the instant it lost sight, so breaking line of
 * sight would be a complete escape; with it, the guard keeps coming to your last
 * known position and you have to actually move somewhere else.
 *
 * <p>It deliberately walks to the remembered tile rather than the player's real
 * one. A searching guard is working from a memory, not from knowledge.
 */
public final class AlertBehaviour implements GuardBehaviour {

    public static final AlertBehaviour INSTANCE = new AlertBehaviour();

    private AlertBehaviour() {
    }

    @Override
    public GuardMode mode() {
        return GuardMode.ALERT;
    }

    @Override
    public GuardState takeTurn(Guard guard, GuardState current, GameMap map, Position player) {
        Position target = current.lastSeenPlayer();
        if (target == null) {
            return current.withMode(GuardMode.ALERT);
        }
        Position step = AStarPathfinder.nextStepTowards(map, current.position(), target)
                .orElse(current.position());

        return new GuardState(step, GuardMode.ALERT, current.routeIndex(), target,
                Math.max(0, current.alertTurnsLeft() - 1));
    }
}
