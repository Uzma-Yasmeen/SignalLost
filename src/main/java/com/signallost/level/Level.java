package com.signallost.level;

import com.signallost.guard.Guard;
import com.signallost.model.GameMap;

import java.util.List;

/**
 * A loaded level: its walls, and the guards posted in it.
 *
 * @param name   the resource the level came from, shown in the HUD
 * @param map    the grid
 * @param guards the guards, in the order the file listed them
 */
public record Level(String name, GameMap map, List<Guard> guards) {

    public Level {
        guards = List.copyOf(guards);
    }
}
