package com.signallost.guard;

/**
 * What a guard is currently doing. A guard's mode decides which
 * {@link GuardBehaviour} runs on its turn, and the HUD and the map colour the
 * guard by it, so the player can read a guard's intent at a glance.
 */
public enum GuardMode {

    /** Walking a fixed route, unaware of the player. */
    PATROL("Patrolling"),

    /** Lost sight of the player and heading for where they were last seen. */
    ALERT("Searching"),

    /** Can see the player right now and is closing in. */
    HUNT("Hunting");

    private final String label;

    GuardMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
