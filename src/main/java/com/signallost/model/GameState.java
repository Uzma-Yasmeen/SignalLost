package com.signallost.model;

import com.signallost.guard.Guard;
import com.signallost.guard.GuardMode;
import com.signallost.pathfinding.AStarPathfinder;
import com.signallost.vision.FogOfWar;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Everything that changes as the game is played: where the player is, how many
 * turns have passed, whether the key is held and whether the exit was reached.
 *
 * <p>Nothing outside this package writes to it. The player's actions arrive as
 * {@link Command} objects, which are executed and then kept on a stack, so undo
 * is a pop. The mutable fields are package-private rather than private for
 * exactly that reason: a command needs to reach them, and nothing else can.
 *
 * <p>Guards and scoring arrive in later steps and will hook into the same turn
 * flow, each taking their turn after the player's command has been executed.
 */
public final class GameState {

    /**
     * How many extra steps a hint will happily walk to stay out of a guard's
     * sight. High enough to go the long way round a patrolled corridor, low
     * enough that it will still cut through when there is genuinely no way past.
     */
    private static final int GUARD_AVOIDANCE_COST = 8;

    private final GameMap map;
    private final FogOfWar fog;
    private final Position exit;
    private final List<Guard> guards;
    private final Deque<Command> history = new ArrayDeque<>();

    // Written only by commands in this package; read by everyone through getters.
    Position player;
    int turnsTaken;
    boolean hasKey;
    boolean escaped;

    /** A level with no guards, which is how most rule tests are set up. */
    public GameState(GameMap map, int sightRadius) {
        this(map, List.of(), sightRadius);
    }

    public GameState(GameMap map, List<Guard> guards, int sightRadius) {
        this.map = map;
        this.guards = List.copyOf(guards);
        Position start = map.findFirst(TileType.START);
        if (start == null) {
            throw new IllegalArgumentException("Level has no start tile '@'");
        }
        this.exit = map.findFirst(TileType.EXIT);
        if (exit == null) {
            throw new IllegalArgumentException("Level has no exit tile 'E'");
        }
        // The start marker is only a spawn point; underneath it is ordinary floor.
        map.setTileAt(start, TileType.FLOOR);
        this.player = start;
        this.fog = new FogOfWar(map, sightRadius);
        this.fog.update(player);
    }

    /**
     * Takes the player's turn: builds a {@link MoveCommand}, runs it, and keeps
     * it for undo.
     *
     * @return true when the move happened, false when a wall, the map edge or an
     *         already-finished level refused it
     */
    public boolean movePlayer(Direction direction) {
        if (escaped) {
            return false;
        }
        Position target = direction.applyTo(player);
        if (direction != Direction.WAIT && !map.isWalkable(target)) {
            return false;
        }
        // The player's action and every guard's reply are one undoable turn.
        Command turn = new TurnCommand(this, new MoveCommand(this, direction, target));
        turn.execute();
        history.push(turn);
        return true;
    }

    /**
     * Takes back the last action, including the turn it cost and any key picked
     * up on the way.
     *
     * @return true when something was undone, false when the stack was empty
     */
    public boolean undoLastMove() {
        if (history.isEmpty()) {
            return false;
        }
        history.pop().undo();
        return true;
    }

    public boolean canUndo() {
        return !history.isEmpty();
    }

    /** How many actions are on the undo stack. */
    public int undoDepth() {
        return history.size();
    }

    /** What undo would take back, or null when there is nothing to take back. */
    public String nextUndoDescription() {
        Command top = history.peek();
        return top == null ? null : top.description();
    }

    /**
     * The next few tiles towards whatever the player needs next — the key, or
     * the exit once they hold it.
     *
     * <p>The route deliberately prefers tiles no guard is watching, even when
     * that makes it longer, so a hint suggests a way through rather than the
     * shortest way into trouble. It is also deliberately omniscient: it accounts
     * for guards the player cannot see. That is the point of a hint, and the
     * reason it is something you choose to turn on.
     *
     * <p>Only the first few steps are returned, so it points rather than solves.
     *
     * @return the tiles to walk, nearest first, or empty when there is nothing
     *         useful to suggest
     */
    public List<Position> hintRoute(int maxSteps) {
        if (escaped || maxSteps <= 0) {
            return List.of();
        }
        Position goal = hasKey ? exit : map.findFirst(TileType.KEY);
        if (goal == null) {
            return List.of();
        }

        Set<Position> watched = new HashSet<>();
        for (Guard guard : guards) {
            watched.addAll(guard.watchedTiles(map));
        }

        List<Position> path = AStarPathfinder.findPath(map, player, goal,
                tile -> watched.contains(tile) ? GUARD_AVOIDANCE_COST : 0);

        // The first entry is the tile the player already stands on.
        return path.size() <= 1
                ? List.of()
                : List.copyOf(path.subList(1, Math.min(path.size(), maxSteps + 1)));
    }

    public List<Guard> guards() {
        return guards;
    }

    /** True when any guard can see the player right now. */
    public boolean isPlayerWatched() {
        return guards.stream().anyMatch(guard -> guard.canSee(map, player));
    }

    /** The most urgent mode any guard is in, or null when there are no guards. */
    public GuardMode highestAlertLevel() {
        GuardMode highest = null;
        for (Guard guard : guards) {
            if (highest == null || guard.mode().ordinal() > highest.ordinal()) {
                highest = guard.mode();
            }
        }
        return highest;
    }

    public GameMap map() {
        return map;
    }

    public FogOfWar fog() {
        return fog;
    }

    public Position player() {
        return player;
    }

    public Position exit() {
        return exit;
    }

    public int turnsTaken() {
        return turnsTaken;
    }

    public boolean hasKey() {
        return hasKey;
    }

    public boolean hasEscaped() {
        return escaped;
    }
}
