package com.signallost.guard;

import com.signallost.model.GameMap;
import com.signallost.model.Position;

import java.util.List;

/**
 * A guard that chases.
 *
 * <p>It is a small state machine over {@link GuardMode}, and the transitions are
 * the whole character of the enemy:
 *
 * <pre>
 *   PATROL  --sees the player-->  HUNT
 *   HUNT    --loses sight------>  ALERT   (remembering where they were)
 *   ALERT   --sees the player-->  HUNT
 *   ALERT   --patience runs out or reaches the spot-->  PATROL
 * </pre>
 *
 * <p>Each mode delegates the actual move to a {@link GuardBehaviour}, so this
 * class holds only the decision about <em>which</em> behaviour runs, never the
 * pathfinding itself. Adding a fourth mode means adding a behaviour and one
 * branch here.
 */
public final class Hunter extends Guard {

    public static final int DEFAULT_SIGHT = 4;

    /** How many turns it keeps searching before going back to its route. */
    public static final int ALERT_PATIENCE = 6;

    public Hunter(String name, Position spawn, List<Position> route) {
        super(name, DEFAULT_SIGHT, spawn, route);
    }

    @Override
    public GuardState decideTurn(GameMap map, Position player) {
        GuardState current = state();

        if (canSee(map, player)) {
            return HuntBehaviour.INSTANCE.takeTurn(this, current, map, player);
        }

        if (current.mode() == GuardMode.HUNT) {
            // Sight broke this turn. Start searching from the last known tile.
            GuardState searching = current.searching(current.lastSeenPlayer(), ALERT_PATIENCE);
            return AlertBehaviour.INSTANCE.takeTurn(this, searching, map, player);
        }

        if (current.mode() == GuardMode.ALERT && stillSearching(current)) {
            return AlertBehaviour.INSTANCE.takeTurn(this, current, map, player);
        }

        return PatrolBehaviour.INSTANCE.takeTurn(this, current, map, player);
    }

    /**
     * Answers the alarm by heading for wherever the player was seen, from
     * anywhere on the map.
     *
     * <p>A hunter already in sight of the player ignores it: it has better
     * information than the report does.
     */
    @Override
    public GuardState onAlarmRaised(GuardState current, Position sighting) {
        if (current.mode() == GuardMode.HUNT || sighting == null) {
            return current;
        }
        return current.searching(sighting, ALERT_PATIENCE);
    }

    /** Searching continues until patience runs out or the spot is reached. */
    private boolean stillSearching(GuardState current) {
        return current.hasSomewhereToSearch()
                && !current.position().equals(current.lastSeenPlayer());
    }

    @Override
    public String type() {
        return "Hunter";
    }
}
