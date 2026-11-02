package com.signallost.guard;

import com.signallost.model.Direction;
import com.signallost.model.GameMap;
import com.signallost.model.Position;

/**
 * Stand still and keep turning.
 *
 * <p>A sentinel's threat is timing rather than pursuit. It never moves, so the
 * danger it poses is entirely a question of which quarter of the room it
 * happens to be looking at on the turn you cross it. Because the rotation is a
 * fixed quarter-turn each turn, a player who watches it for four turns knows
 * the whole cycle — which is the point. A hazard you can learn is a puzzle; one
 * you cannot is a coin toss.
 */
public final class SentinelBehaviour implements GuardBehaviour {

    public static final SentinelBehaviour INSTANCE = new SentinelBehaviour();

    /** The order a sentinel sweeps through, one step per turn. */
    private static final Direction[] SWEEP = {
        Direction.UP, Direction.RIGHT, Direction.DOWN, Direction.LEFT
    };

    private SentinelBehaviour() {
    }

    /**
     * Sweeping counts as patrolling.
     *
     * <p>It is the second behaviour to claim {@code PATROL}, which the one
     * behaviour per mode arrangement did not anticipate. Nothing looks a
     * behaviour up by its mode — each guard names the ones it uses — so the
     * overlap costs nothing, and the alternative, a mode meaning "patrolling
     * but standing still", would say nothing the guard's own type does not.
     */
    @Override
    public GuardMode mode() {
        return GuardMode.PATROL;
    }

    @Override
    public GuardState takeTurn(Guard guard, GuardState current, GameMap map, Position player) {
        return current.turnedTo(nextAfter(current.facing()));
    }

    /** One quarter-turn clockwise. */
    static Direction nextAfter(Direction facing) {
        for (int i = 0; i < SWEEP.length; i++) {
            if (SWEEP[i] == facing) {
                return SWEEP[(i + 1) % SWEEP.length];
            }
        }
        return SWEEP[0];
    }

    /**
     * The quarter a sentinel would have to be looking at to see this tile.
     *
     * <p>Used when an alarm goes up: the sentinel stops sweeping and stares at
     * wherever the player was seen.
     */
    static Direction towards(Position from, Position target) {
        int downwards = target.row() - from.row();
        int rightwards = target.col() - from.col();
        if (Math.abs(downwards) >= Math.abs(rightwards)) {
            return downwards < 0 ? Direction.UP : Direction.DOWN;
        }
        return rightwards < 0 ? Direction.LEFT : Direction.RIGHT;
    }
}
