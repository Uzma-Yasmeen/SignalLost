package com.signallost.guard;

import com.signallost.model.GameMap;
import com.signallost.model.Position;
import com.signallost.vision.VisibilityService;

import java.util.List;
import java.util.Set;

/**
 * Something that patrols the facility and can see.
 *
 * <p>Guards reuse the same {@link VisibilityService} flood fill as the player's
 * fog of war, so a guard's sight obeys exactly the rules the player has learned:
 * it travels along open tiles, bends round corners and stops at walls. Sight
 * being one shared, tested algorithm rather than two similar ones is the reason
 * a guard can never see through a wall the player is hiding behind.
 *
 * <p>A guard never changes itself. It is asked what it would do, hands back a
 * {@link GuardState}, and something else decides whether to apply it — which is
 * how a guard's turn ends up on the undo stack with the player's.
 */
public abstract class Guard implements AlarmListener {

    private final String name;
    private final int sightRadius;
    private final List<Position> route;
    private GuardState state;

    protected Guard(String name, int sightRadius, Position spawn, List<Position> route) {
        this.name = name;
        this.sightRadius = sightRadius;
        this.route = List.copyOf(route);
        this.state = GuardState.startingAt(spawn);
    }

    /**
     * Works out this guard's next state without applying it.
     *
     * <p>It takes the map and the player's tile rather than the whole game, so a
     * guard can be tested with two arguments and the guard package never has to
     * know the rest of the model.
     */
    public abstract GuardState decideTurn(GameMap map, Position player);

    /** A short word for what this guard is, shown in the HUD. */
    public abstract String type();

    /**
     * Guards ignore the alarm unless they can do something about it. A patroller
     * raises the alarm by seeing you, but it has no answer to one.
     */
    @Override
    public GuardState onAlarmRaised(GuardState current, Position sighting) {
        return current;
    }

    // ----------------------------------------------------------------- sight

    /** The tiles this guard can currently see. */
    public Set<Position> watchedTiles(GameMap map) {
        return VisibilityService.visibleFrom(map, state.position(), sightRadius);
    }

    public boolean canSee(GameMap map, Position target) {
        return watchedTiles(map).contains(target);
    }

    // ----------------------------------------------------------------- state

    public GuardState state() {
        return state;
    }

    /**
     * Swaps in a new state. Called by the turn system when a guard takes its
     * turn, and again by undo to put the old state back.
     */
    public void applyState(GuardState newState) {
        this.state = newState;
    }

    public Position position() {
        return state.position();
    }

    public GuardMode mode() {
        return state.mode();
    }

    public String name() {
        return name;
    }

    public int sightRadius() {
        return sightRadius;
    }

    /** The patrol waypoints, in order. Empty for a guard that does not patrol. */
    public List<Position> route() {
        return route;
    }
}
