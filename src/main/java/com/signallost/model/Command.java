package com.signallost.model;

/**
 * One reversible action, in the Command pattern.
 *
 * <p>Every change the player makes to the game goes through an object like this
 * rather than being written straight into {@link GameState}. The game keeps the
 * executed commands on a stack, so undo is a pop followed by {@link #undo()}.
 *
 * <p>Commands live in the same package as {@code GameState} on purpose: it lets
 * them touch its fields directly while nothing outside the package can, so the
 * only way for the rest of the program to change the game is through a command.
 */
public interface Command {

    /** Applies the action to the game. */
    void execute();

    /** Puts the game back exactly as it was before {@link #execute()}. */
    void undo();

    /** A short label, used by the HUD and useful when debugging the stack. */
    String description();
}
