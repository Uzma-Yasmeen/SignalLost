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

    // The end-of-turn verdict, kept so undo can take it back too.
    private int previousSightings;
    private boolean previousSpotted;
    private boolean previousFailed;
    private boolean previousCaught;

    /** The alarm raised this turn, if the player was seen. */
    private Command alarmAction;

    TurnCommand(GameState state, Command playerAction) {
        this.state = state;
        this.playerAction = playerAction;
    }

    @Override
    public void execute() {
        previousSightings = state.sightings;
        previousSpotted = state.spotted;
        previousFailed = state.failed;
        previousCaught = state.caught;

        playerAction.execute();

        guardActions.clear();
        for (Guard guard : state.guards()) {
            Command guardAction = new GuardMoveCommand(guard, state.map(), state.player());
            guardAction.execute();
            guardActions.add(guardAction);
        }

        settleEndOfTurn();
    }

    /**
     * Decides whether the player was seen and whether the clock ran out.
     *
     * <p>This happens <em>after</em> the guards have moved, which is the whole
     * tension of the game: stepping somewhere safe is not enough, because a
     * guard may turn a corner and be looking at you by the time the turn ends.
     *
     * <p>Getting away wins outright. A player who reaches the exit on the very
     * last turn, or while standing in a guard's sight, has still escaped.
     */
    private void settleEndOfTurn() {
        alarmAction = null;
        if (state.escaped) {
            state.spotted = false;
            return;
        }
        // A guard standing on the player has caught them. Contact is contact:
        // a patroller that blunders into you is as final as a hunter that ran
        // you down, and a rule with no exceptions is one a player can learn.
        state.caught = state.guards().stream()
                .anyMatch(guard -> guard.position().equals(state.player));

        state.spotted = state.caught || state.isPlayerWatched();
        if (state.spotted) {
            state.sightings++;
        }
        // Raising the alarm once you are already caught would be shouting for
        // help that has arrived.
        if (state.spotted && !state.caught) {
            alarmAction = new AlarmCommand(state.guards(), state.player());
            alarmAction.execute();
        }
        state.failed = state.hasTurnLimit() && state.turnsTaken >= state.turnLimit();
    }

    @Override
    public void undo() {
        // Everything unwinds in the order it was applied, backwards.
        if (alarmAction != null) {
            alarmAction.undo();
        }
        for (int i = guardActions.size() - 1; i >= 0; i--) {
            guardActions.get(i).undo();
        }
        playerAction.undo();

        state.sightings = previousSightings;
        state.spotted = previousSpotted;
        state.failed = previousFailed;
        state.caught = previousCaught;
    }

    /** The HUD shows the player's half of the turn; the guards are implied. */
    @Override
    public String description() {
        return playerAction.description();
    }
}
