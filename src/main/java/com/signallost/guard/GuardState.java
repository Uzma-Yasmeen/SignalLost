package com.signallost.guard;

import com.signallost.model.Position;

/**
 * Everything about a guard that changes from turn to turn, in one immutable
 * value.
 *
 * <p>Holding it as a single record rather than as loose fields on the guard is
 * what makes undo cheap: a guard's turn is "work out the next state and swap it
 * in", so taking the turn back is swapping the old one straight back. Nothing
 * has to be unwound step by step.
 *
 * <p>Not every field matters to every guard — a patroller never searches, so its
 * {@code lastSeenPlayer} stays null — which is the small price for one shared
 * snapshot type instead of one per guard class.
 *
 * @param position        the tile the guard stands on
 * @param mode            what it is currently doing
 * @param routeIndex      which patrol waypoint it is heading for
 * @param lastSeenPlayer  where the player was last spotted, or null
 * @param alertTurnsLeft  how much longer it will keep searching before giving up
 * @param surgedLastTurn  whether a hunting guard sprinted on its last turn, so
 *                        that it walks on this one
 */
public record GuardState(
        Position position,
        GuardMode mode,
        int routeIndex,
        Position lastSeenPlayer,
        int alertTurnsLeft,
        boolean surgedLastTurn) {

    /** The state a guard starts a level in. */
    public static GuardState startingAt(Position spawn) {
        return new GuardState(spawn, GuardMode.PATROL, 0, null, 0, false);
    }

    public GuardState at(Position newPosition) {
        return new GuardState(newPosition, mode, routeIndex, lastSeenPlayer, alertTurnsLeft,
                surgedLastTurn);
    }

    public GuardState withMode(GuardMode newMode) {
        return new GuardState(position, newMode, routeIndex, lastSeenPlayer, alertTurnsLeft,
                surgedLastTurn);
    }

    /** Switches to searching for the player, with a fresh patience counter. */
    public GuardState searching(Position target, int patience) {
        // Searching is a walk, so any sprint is over.
        return new GuardState(position, GuardMode.ALERT, routeIndex, target, patience, false);
    }

    public boolean hasSomewhereToSearch() {
        return lastSeenPlayer != null && alertTurnsLeft > 0;
    }
}
