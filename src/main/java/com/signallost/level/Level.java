package com.signallost.level;

import com.signallost.guard.Guard;
import com.signallost.model.GameMap;

import java.util.List;

/**
 * A loaded level: its walls, and the guards posted in it.
 *
 * @param name      the resource the level came from, shown in the HUD
 * @param map       the grid
 * @param guards    the guards, in the order the file listed them
 * @param turnLimit turns allowed before the run fails, or
 *                  {@link com.signallost.model.GameState#NO_TURN_LIMIT} for none
 */
public record Level(String name, GameMap map, List<Guard> guards, int turnLimit) {

    public Level {
        guards = List.copyOf(guards);
    }
}
