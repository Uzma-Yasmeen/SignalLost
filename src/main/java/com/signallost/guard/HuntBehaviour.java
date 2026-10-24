package com.signallost.guard;

import com.signallost.model.GameMap;
import com.signallost.model.Position;
import com.signallost.pathfinding.AStarPathfinder;

import java.util.Optional;

/**
 * Closes in on the player by the shortest route, two tiles at a time.
 *
 * <p>The path is recomputed every single turn rather than walked once, which is
 * what makes a chase feel alive: doubling back does not shake the guard off,
 * because next turn it simply solves the new, shorter problem.
 *
 * <p><b>A hunting guard surges: two tiles, then one, then two again.</b> At a
 * flat one tile a turn it was exactly the player's speed, so once it fell in
 * behind you the gap froze — it could never close and you could never shake it,
 * so it escorted you to the exit instead of chasing you. At a flat two it was
 * the opposite problem: it gained a whole tile every turn, which is faster than
 * the levels give the player any chance to reach cover.
 *
 * <p>Alternating gains half a tile a turn. Running is not an answer, because the
 * gap does close — but spotted at the edge of its sight you have roughly eight
 * turns to get out of view, which the level geometry can actually honour.
 *
 * <p>So the way out is breaking line of sight, not speed, and that turns the fog
 * from scenery into the thing that saves you.
 */
public final class HuntBehaviour implements GuardBehaviour {

    public static final HuntBehaviour INSTANCE = new HuntBehaviour();

    private HuntBehaviour() {
    }

    @Override
    public GuardMode mode() {
        return GuardMode.HUNT;
    }

    /** Tiles covered on a surging turn. On the turn after, it covers one. */
    public static final int SURGE_STEPS = 2;

    @Override
    public GuardState takeTurn(Guard guard, GuardState current, GameMap map, Position player) {
        boolean surging = !current.surgedLastTurn();
        int steps = surging ? SURGE_STEPS : 1;

        Position at = current.position();
        for (int step = 0; step < steps; step++) {
            if (at.equals(player)) {
                break;   // reached them; no point walking past
            }
            Optional<Position> next = AStarPathfinder.nextStepTowards(map, at, player);
            if (next.isEmpty()) {
                break;   // no route, or already standing on them
            }
            at = next.get();
        }

        // The player's tile is recorded every turn, so the moment sight breaks
        // the guard already knows the freshest place to start searching.
        return new GuardState(at, GuardMode.HUNT, current.routeIndex(), player, 0, surging);
    }
}
