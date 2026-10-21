package com.signallost.guard;

import com.signallost.model.GameMap;
import com.signallost.model.Position;

/**
 * One way of spending a guard's turn, in the Strategy pattern.
 *
 * <p>There is one implementation per {@link GuardMode}: walk the route, search
 * the last known spot, or close in. A {@link Hunter} swaps between them as it
 * gains and loses sight of the player, which is what makes the set of
 * behaviours a state machine rather than a fixed choice.
 *
 * <p>Implementations are stateless and take the state to work from as an
 * argument, so the same instance is shared by every guard and a behaviour can be
 * tested on its own without building a game around it.
 */
public interface GuardBehaviour {

    /** The mode this behaviour represents. */
    GuardMode mode();

    /**
     * Works out the guard's next state. It changes nothing: the caller decides
     * whether to apply the result, which is what keeps the turn undoable.
     *
     * @param guard   the guard, for its route and sight radius
     * @param current the state to work from, which may differ from the guard's
     *                own state when a mode change is being seeded this turn
     * @param map     the level
     * @param player  where the player is standing
     */
    GuardState takeTurn(Guard guard, GuardState current, GameMap map, Position player);
}
