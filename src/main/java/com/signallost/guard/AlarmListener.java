package com.signallost.guard;

import com.signallost.model.Position;

/**
 * Something that reacts when the player is seen, in the Observer pattern.
 *
 * <p>When any guard ends a turn with the player in its sight, the alarm goes up
 * and <em>every</em> listener is told where the player was. The guard that did
 * the seeing does not chase alone: the sighting is broadcast, and whoever is
 * interested answers it.
 *
 * <p>That is what gives being spotted a consequence. A patroller cannot chase
 * anybody, but it can raise the alarm, which makes the dumbest guard in the
 * facility the most dangerous thing to walk past — it is the tripwire that sets
 * the hunters moving from wherever they happen to be.
 *
 * <p>A listener returns the state it wants to adopt rather than changing itself,
 * so the caller can record the old state first and the whole alarm stays
 * undoable along with the rest of the turn.
 */
public interface AlarmListener {

    /**
     * The player has been seen.
     *
     * @param current  this guard's state right now
     * @param sighting the tile the player was seen on
     * @return the state to adopt, or {@code current} to ignore the alarm
     */
    GuardState onAlarmRaised(GuardState current, Position sighting);
}
