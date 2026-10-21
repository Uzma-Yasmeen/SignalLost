package com.signallost.model;

/**
 * A single player action: a step in one direction, or a deliberate wait.
 *
 * <p>Before changing anything, the command copies the parts of the game it is
 * about to touch, which is what makes undo exact rather than approximate. One
 * thing is deliberately <em>not</em> reversed: the fog's memory of tiles already
 * explored. Taking a move back un-walks the step, but the player has still seen
 * what they saw, so the remembered map stays.
 */
final class MoveCommand implements Command {

    private final GameState state;
    private final Direction direction;
    private final Position destination;

    // The snapshot taken at execute() time.
    private Position previousPosition;
    private int previousTurns;
    private boolean previousHasKey;
    private boolean previousEscaped;

    /** The tile the move consumed, so it can be put back; null when none was. */
    private TileType consumedTile;

    MoveCommand(GameState state, Direction direction, Position destination) {
        this.state = state;
        this.direction = direction;
        this.destination = destination;
    }

    @Override
    public void execute() {
        previousPosition = state.player;
        previousTurns = state.turnsTaken;
        previousHasKey = state.hasKey;
        previousEscaped = state.escaped;
        consumedTile = null;

        state.player = destination;
        state.turnsTaken++;

        if (state.map().tileAt(destination) == TileType.KEY) {
            consumedTile = TileType.KEY;
            state.hasKey = true;
            state.map().setTileAt(destination, TileType.FLOOR);
        }
        if (state.hasKey && destination.equals(state.exit())) {
            state.escaped = true;
        }
        state.fog().update(destination);
    }

    @Override
    public void undo() {
        if (consumedTile != null) {
            state.map().setTileAt(destination, consumedTile);
        }
        state.player = previousPosition;
        state.turnsTaken = previousTurns;
        state.hasKey = previousHasKey;
        state.escaped = previousEscaped;

        // Recompute sight from where the player now stands. The explored set
        // inside FogOfWar only ever grows, so memory survives the undo.
        state.fog().update(previousPosition);
    }

    @Override
    public String description() {
        return direction == Direction.WAIT ? "wait" : "move " + direction.name().toLowerCase();
    }
}
