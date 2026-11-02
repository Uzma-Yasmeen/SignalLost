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

    public Sentinel(String name, Position post, Direction initialFacing) {
        super(name, DEFAULT_SIGHT, post, List.of(post));
        applyState(state().turnedTo(initialFacing));
    }

    /**
     * Sweeps, and keeps sweeping, whatever else is happening.
     *
     * <p>It used to break off and stare at wherever the player had been seen,
     * which read well and played terribly. The alarm is raised again on every
     * turn the player is still in somebody's sight, and each one restarted the
     * stare, so a sentinel that caught sight of you once stopped turning for
     * good and became a spotlight bolted to your back.
     *
     * <p>The fix is not a longer fuse but no fuse. A camera whose sweep never
     * varies can be watched for four turns and then timed, which is the only
     * thing that makes it a puzzle rather than one more thing chasing you.
     */
    @Override
    public GuardState decideTurn(GameMap map, Position player) {
        return SentinelBehaviour.INSTANCE.takeTurn(this, state(), map, player);
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
