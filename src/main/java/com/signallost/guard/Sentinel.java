package com.signallost.guard;

import com.signallost.model.Direction;
import com.signallost.model.GameMap;
import com.signallost.model.Position;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A camera on a post: it never moves, and it sees only where it is pointed.
 *
 * <p>The other two guards see in every direction at once, which makes them
 * hazards you keep away from. A sentinel is the opposite — you can walk right
 * past one, provided you do it while it is looking the other way. It turns the
 * level into a timing problem rather than a distance problem.
 *
 * <p>It also cannot catch anybody. It never enters {@link GuardMode#HUNT}, and
 * only a hunting guard captures, so the worst a sentinel does is see you and
 * shout. That is enough: what it shouts for is the hunter.
 */
public final class Sentinel extends Guard {

    /** Sentinels see further than a patroller, because they only see one way. */
    public static final int DEFAULT_SIGHT = 6;

    /** Turns a sentinel stares at a sighting before resuming its sweep. */
    public static final int STARE_TURNS = 3;

    public Sentinel(String name, Position post, Direction initialFacing) {
        super(name, DEFAULT_SIGHT, post, List.of(post));
        applyState(state().turnedTo(initialFacing));
    }

    @Override
    public GuardState decideTurn(GameMap map, Position player) {
        GuardState current = state();

        // Staring at a sighting: hold still, burning one turn of patience.
        if (current.mode() == GuardMode.ALERT && current.hasSomewhereToSearch()) {
            GuardState waited = current.oneTurnLessPatient();
            return waited.hasSomewhereToSearch() ? waited : waited.withMode(GuardMode.PATROL);
        }

        return SentinelBehaviour.INSTANCE.takeTurn(this, current, map, player);
    }

    /**
     * Turns to face where the player was seen and holds there.
     *
     * <p>A sentinel cannot answer an alarm by chasing, but it can stop sweeping
     * and watch the place the player was last seen, which is the most useful
     * thing a fixed camera can do.
     */
    @Override
    public GuardState onAlarmRaised(GuardState current, Position sighting) {
        return current
                .searching(sighting, STARE_TURNS)
                .turnedTo(SentinelBehaviour.towards(current.position(), sighting));
    }

    /**
     * Only the quarter it is facing, and only as far as the walls allow.
     *
     * <p>The flood fill is the same one the player's fog uses, so a sentinel
     * still cannot see through a wall. The cone is applied on top of it, which
     * means the two rules compose rather than compete: a tile has to be both in
     * front of the sentinel and actually reachable by light.
     */
    @Override
    public Set<Position> watchedTiles(GameMap map) {
        Position eye = position();
        Direction facing = state().facing();
        Set<Position> cone = new LinkedHashSet<>();
        for (Position lit : super.watchedTiles(map)) {
            if (withinCone(eye, facing, lit)) {
                cone.add(lit);
            }
        }
        return cone;
    }

    /**
     * A quarter-circle wedge opening away from the sentinel.
     *
     * <p>The test is that the sideways offset never exceeds the forward one, so
     * the wedge widens by one tile for every tile of distance. That gives a
     * ninety-degree cone with no trigonometry and no rounding to argue about.
     */
    static boolean withinCone(Position eye, Direction facing, Position tile) {
        int downwards = tile.row() - eye.row();
        int rightwards = tile.col() - eye.col();
        if (downwards == 0 && rightwards == 0) {
            return true;                                  // its own tile
        }
        return switch (facing) {
            case UP -> downwards < 0 && Math.abs(rightwards) <= -downwards;
            case DOWN -> downwards > 0 && Math.abs(rightwards) <= downwards;
            case LEFT -> rightwards < 0 && Math.abs(downwards) <= -rightwards;
            case RIGHT -> rightwards > 0 && Math.abs(downwards) <= rightwards;
            case WAIT -> true;                            // not a facing a sentinel takes
        };
    }

    @Override
    public String type() {
        return "Sentinel";
    }
}
