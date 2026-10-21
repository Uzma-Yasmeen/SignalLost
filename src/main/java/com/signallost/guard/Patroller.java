package com.signallost.guard;

import com.signallost.model.GameMap;
import com.signallost.model.Position;

import java.util.List;

/**
 * A guard that walks its route and nothing else.
 *
 * <p>It can see, and the player can be standing in its sight, but it never
 * reacts — it is the predictable hazard you learn to time. Keeping one guard
 * type dumb is what makes the {@link Hunter} frightening by contrast.
 */
public final class Patroller extends Guard {

    public static final int DEFAULT_SIGHT = 3;

    public Patroller(String name, Position spawn, List<Position> route) {
        super(name, DEFAULT_SIGHT, spawn, route);
    }

    @Override
    public GuardState decideTurn(GameMap map, Position player) {
        return PatrolBehaviour.INSTANCE.takeTurn(this, state(), map, player);
    }

    @Override
    public String type() {
        return "Patroller";
    }
}
