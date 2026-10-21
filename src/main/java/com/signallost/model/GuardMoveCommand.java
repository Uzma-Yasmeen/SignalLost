package com.signallost.model;

import com.signallost.guard.Guard;
import com.signallost.guard.GuardState;

/**
 * One guard taking its turn.
 *
 * <p>The guard works out what it wants to do, this command keeps the state it
 * had beforehand, and undo swaps the old one back. Because a guard's whole turn
 * is one immutable {@link GuardState}, that swap is exact: position, mode, patrol
 * progress and the memory of where the player was last seen all rewind together.
 */
final class GuardMoveCommand implements Command {

    private final Guard guard;
    private final GameMap map;
    private final Position playerPosition;

    private GuardState previousState;

    GuardMoveCommand(Guard guard, GameMap map, Position playerPosition) {
        this.guard = guard;
        this.map = map;
        this.playerPosition = playerPosition;
    }

    @Override
    public void execute() {
        previousState = guard.state();
        guard.applyState(guard.decideTurn(map, playerPosition));
    }

    @Override
    public void undo() {
        if (previousState != null) {
            guard.applyState(previousState);
        }
    }

    @Override
    public String description() {
        return guard.name() + " " + guard.mode().label().toLowerCase();
    }
}
