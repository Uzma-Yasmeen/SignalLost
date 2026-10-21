package com.signallost.model;

import com.signallost.guard.Guard;

import java.util.ArrayList;
import java.util.List;

/**
 * A whole turn: the player's action, then every guard's.
 *
 * <p>This is the payoff for having built undo before the guards. A turn is a
 * command made of commands, so pressing undo once rewinds the player's step
 * <em>and</em> every guard that moved in response — in reverse order, the way
 * you unstack anything. The turn system did not have to be rewritten to make
 * room for enemies; enemies just became more commands inside the turn.
 *
 * <p>The guards' commands are built during {@link #execute()} rather than up
 * front, because a guard decides what to do based on where the player has just
 * moved to, not where they were.
 */
final class TurnCommand implements Command {

    private final GameState state;
    private final Command playerAction;
    private final List<Command> guardActions = new ArrayList<>();

    TurnCommand(GameState state, Command playerAction) {
        this.state = state;
        this.playerAction = playerAction;
    }

    @Override
    public void execute() {
        playerAction.execute();

        guardActions.clear();
        for (Guard guard : state.guards()) {
            Command guardAction = new GuardMoveCommand(guard, state.map(), state.player());
            guardAction.execute();
            guardActions.add(guardAction);
        }
    }

    @Override
    public void undo() {
        // Reverse order, so the game passes back through the exact states it came through.
        for (int i = guardActions.size() - 1; i >= 0; i--) {
            guardActions.get(i).undo();
        }
        playerAction.undo();
    }

    /** The HUD shows the player's half of the turn; the guards are implied. */
    @Override
    public String description() {
        return playerAction.description();
    }
}
