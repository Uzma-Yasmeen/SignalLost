package com.signallost.model;

import com.signallost.guard.Guard;
import com.signallost.guard.GuardState;

import java.util.ArrayList;
import java.util.List;

/**
 * The alarm going up: every guard is told where the player was seen.
 *
 * <p>The guard that did the seeing does not chase alone. Hunters anywhere on the
 * map answer the report and start converging on the tile, which is what makes
 * walking past a patroller cost something — it cannot chase you itself, but it
 * can tell everyone who can.
 *
 * <p>Each guard hands back the state it wants rather than changing itself, so
 * this command records the old states first and undo puts them all back. Without
 * that, taking a move back would leave the facility alerted about a sighting
 * that, as far as the game is concerned, never happened.
 */
final class AlarmCommand implements Command {

    private final List<Guard> guards;
    private final Position sighting;
    private final List<GuardState> previousStates = new ArrayList<>();

    AlarmCommand(List<Guard> guards, Position sighting) {
        this.guards = guards;
        this.sighting = sighting;
    }

    @Override
    public void execute() {
        previousStates.clear();
        for (Guard guard : guards) {
            previousStates.add(guard.state());
            guard.applyState(guard.onAlarmRaised(guard.state(), sighting));
        }
    }

    @Override
    public void undo() {
        for (int i = guards.size() - 1; i >= 0; i--) {
            guards.get(i).applyState(previousStates.get(i));
        }
    }

    @Override
    public String description() {
        return "alarm at " + sighting;
    }
}
