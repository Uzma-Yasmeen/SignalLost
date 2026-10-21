package com.signallost.vision;

import com.signallost.model.GameMap;
import com.signallost.model.Position;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The player's knowledge of the map: what is in sight this turn, and what has
 * ever been in sight.
 *
 * <p>{@link VisibilityService} is the pure algorithm; this class is the memory
 * that accumulates around it as the player walks. A developer toggle reveals the
 * whole map, which is how the fog logic gets checked by eye during development.
 */
public final class FogOfWar {

    private final GameMap map;
    private final int sightRadius;
    private final Set<Position> explored = new HashSet<>();
    private Map<Position, Integer> sightDistances = Collections.emptyMap();
    private boolean revealAll;

    public FogOfWar(GameMap map, int sightRadius) {
        this.map = map;
        this.sightRadius = sightRadius;
    }

    /** Recomputes sight from the player's new tile and adds it to memory. */
    public void update(Position viewer) {
        sightDistances = VisibilityService.sightDistancesFrom(map, viewer, sightRadius);
        explored.addAll(sightDistances.keySet());
    }

    public TileVisibility visibilityOf(Position position) {
        if (revealAll) {
            return TileVisibility.VISIBLE;
        }
        if (sightDistances.containsKey(position)) {
            return TileVisibility.VISIBLE;
        }
        if (explored.contains(position)) {
            return TileVisibility.REMEMBERED;
        }
        return TileVisibility.UNEXPLORED;
    }

    public boolean isVisible(Position position) {
        return visibilityOf(position) == TileVisibility.VISIBLE;
    }

    /**
     * How many steps of light it took to reach this tile, or -1 when the tile is
     * not in sight. The renderer uses it to dim distant tiles.
     */
    public int sightDistance(Position position) {
        Integer distance = sightDistances.get(position);
        return distance == null ? -1 : distance;
    }

    public Set<Position> visibleTiles() {
        return Collections.unmodifiableSet(sightDistances.keySet());
    }

    public Set<Position> exploredTiles() {
        return Collections.unmodifiableSet(explored);
    }

    public int sightRadius() {
        return sightRadius;
    }

    /** The developer toggle that draws the whole map regardless of fog. */
    public boolean isRevealAll() {
        return revealAll;
    }

    public void setRevealAll(boolean revealAll) {
        this.revealAll = revealAll;
    }

    public void toggleRevealAll() {
        revealAll = !revealAll;
    }
}
