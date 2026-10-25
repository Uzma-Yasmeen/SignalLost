package com.signallost.model;

import com.signallost.guard.Guard;
import com.signallost.guard.GuardMode;
import com.signallost.guard.GuardState;
import com.signallost.pathfinding.AStarPathfinder;
import com.signallost.pathfinding.RouteMetrics;
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

    /**
     * How far around itself a guard makes the ground expensive, and how much.
     *
     * <p>Avoiding only the tiles a guard can see right now is not enough advice
     * to survive on. A hunter covers ground faster than the player, so by the
     * time you reach a tile it was merely near, it is looking at you. The hint
     * therefore gives guards a berth that widens when they are hunting.
     */
    private static final int NEAR_GUARD_REACH = 3;
    private static final int NEAR_HUNTER_REACH = 5;
    private static final int NEAR_GUARD_COST = 6;
    private static final int NEAR_HUNTER_COST = 14;

    /** A turn limit of this value means the level is untimed. */
    public static final int NO_TURN_LIMIT = 0;

    /** Attempts at a level before the run is over. */
    public static final int STARTING_LIVES = 3;

    /**
     * How much longer than the straight-line route still counts as brisk.
     *
     * <p>Par is worked out from the level's own geometry rather than typed into
     * the level file, so it cannot drift out of date when the walls move.
     *
     * <p>The slack has to be generous, and the reason is a trap worth spelling
     * out. The clear route ignores guards; a player avoiding them walks further.
     * At 1.4 the two bonus stars on level 1 became mutually exclusive: the
     * fastest escape took 38 turns but the fastest <em>unseen</em> escape took
     * 56, against a par of 51. Three stars was unreachable no matter how well
     * anybody played, which is not difficulty, it is a broken promise.
     * {@code StarRatingAchievableTest} now holds the line.
     */
    private static final double PAR_SLACK = 1.9;

    private final GameMap map;
    private final FogOfWar fog;
    private final Position exit;
    private final List<Guard> guards;
    private final int turnLimit;
    private final int keysRequired;
    private final Position startTile;
    private final List<Position> keySpawns;
    private final List<GuardState> guardSpawns;
    private final int parTurns;
    private final Deque<Command> history = new ArrayDeque<>();

    // Written only by commands in this package; read by everyone through getters.
    Position player;
    int turnsTaken;
    int keysHeld;
    boolean escaped;
    int sightings;
    boolean spotted;
    boolean failed;
    boolean caught;
    int livesLeft = STARTING_LIVES;
    boolean runOver;

    /** A level with no guards, which is how most rule tests are set up. */
    public GameState(GameMap map, int sightRadius) {
        this(map, List.of(), sightRadius, NO_TURN_LIMIT);
    }

    public GameState(GameMap map, List<Guard> guards, int sightRadius) {
        this(map, guards, sightRadius, NO_TURN_LIMIT);
    }

    public GameState(GameMap map, List<Guard> guards, int sightRadius, int turnLimit) {
        this.map = map;
        this.guards = List.copyOf(guards);
        this.turnLimit = Math.max(NO_TURN_LIMIT, turnLimit);
        this.keysRequired = map.count(TileType.KEY);
        this.keySpawns = map.findAll(TileType.KEY);
        // Worked out before the start marker is paved over, since the route
        // begins there.
        int clearRoute = RouteMetrics.shortestClearRoute(map);
        this.parTurns = clearRoute == RouteMetrics.UNREACHABLE
                ? 0
                : (int) Math.ceil(clearRoute * PAR_SLACK);
        this.guardSpawns = this.guards.stream().map(Guard::state).toList();
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
        this.startTile = start;
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
        if (isFinished() || isAttemptOver()) {
            return false;
        }
        Position target = direction.applyTo(player);
        if (direction != Direction.WAIT && !map.isWalkable(target)) {
            return false;
        }
        // Guards block like walls. Walking into one is not a way to die, it is
        // simply not a move.
        if (direction != Direction.WAIT && isGuardOn(target)) {
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
        // Being caught, or running the clock out, is final. Undo is for
        // mistakes you notice, not for the ones that cost you a life.
        if (isAttemptOver() || history.isEmpty()) {
            return false;
        }
        history.pop().undo();
        return true;
    }

    /**
     * Whether undo would do anything.
     *
     * <p>It has to agree with {@link #undoLastMove()}, including the refusal
     * once an attempt is over: a caller that loops while this is true and undo
     * keeps refusing would spin forever.
     */
    public boolean canUndo() {
        return !isAttemptOver() && !history.isEmpty();
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
        if (escaped || isAttemptOver() || maxSteps <= 0) {
            return List.of();
        }

        Set<Position> watched = new HashSet<>();
        for (Guard guard : guards) {
            watched.addAll(guard.watchedTiles(map));
        }

        // Standing in somebody's sight, the useful advice is not "the key is
        // that way", it is "get out of view". Progress is no good to a player
        // who is about to be caught making it.
        Position goal = hintSeeksCover()
                ? nearestCover(watched)
                : (hasAllKeys() ? exit : nearestKey());
        if (goal == null) {
            goal = hasAllKeys() ? exit : nearestKey();
        }
        if (goal == null) {
            return List.of();
        }

        List<Position> path = AStarPathfinder.findPath(map, player, goal,
                tile -> hintCost(tile, watched));

        // The first entry is the tile the player already stands on.
        return path.size() <= 1
                ? List.of()
                : List.copyOf(path.subList(1, Math.min(path.size(), maxSteps + 1)));
    }

    /**
     * Whether the hint is currently pointing at cover rather than at a key.
     *
     * <p>The UI colours the arrow differently for the two, because "go this way
     * to make progress" and "go this way or be caught" deserve to look
     * different at a glance.
     */
    public boolean hintSeeksCover() {
        return isPlayerWatched();
    }

    /**
     * The uncollected key closest to the player, by straight-line step count.
     *
     * <p>Nearest rather than first-in-the-file, so a hint sends the player to
     * the key in front of them rather than back across the level.
     */
    private Position nearestKey() {
        Position nearest = null;
        int best = Integer.MAX_VALUE;
        for (Position tile : map.findAll(TileType.KEY)) {
            int distance = tile.manhattanDistanceTo(player);
            if (distance < best) {
                best = distance;
                nearest = tile;
            }
        }
        return nearest;
    }

    /**
     * The closest tile no guard can see and none is standing on.
     *
     * <p>Breadth-first, so it finds the quickest way out of view rather than
     * the safest-looking one further off: when a hunter is closing, the number
     * of turns it takes to disappear is the thing that matters.
     */
    private Position nearestCover(Set<Position> watched) {
        Deque<Position> frontier = new ArrayDeque<>();
        Set<Position> seen = new HashSet<>();
        frontier.add(player);
        seen.add(player);

        while (!frontier.isEmpty()) {
            Position current = frontier.remove();
            if (!watched.contains(current) && !isGuardOn(current)) {
                return current;
            }
            for (Position neighbour : map.walkableNeighbours(current)) {
                if (!isGuardOn(neighbour) && seen.add(neighbour)) {
                    frontier.add(neighbour);
                }
            }
        }
        return null;
    }

    /** True when some guard is standing on this tile. */
    public boolean isGuardOn(Position tile) {
        return guards.stream().anyMatch(guard -> guard.position().equals(tile));
    }

    /**
     * What the hint pays to step on a tile, on top of the step itself.
     *
     * <p>Being watched costs; being close to a guard costs more the closer it
     * is, and far more when that guard is hunting. The result is a route that
     * gives trouble a wide berth rather than one that technically threads it.
     */
    private int hintCost(Position tile, Set<Position> watched) {
        int cost = watched.contains(tile) ? GUARD_AVOIDANCE_COST : 0;
        for (Guard guard : guards) {
            boolean hunting = guard.mode() == GuardMode.HUNT;
            int reach = hunting ? NEAR_HUNTER_REACH : NEAR_GUARD_REACH;
            int distance = guard.position().manhattanDistanceTo(tile);
            if (distance <= reach) {
                cost += (reach - distance + 1) * (hunting ? NEAR_HUNTER_COST : NEAR_GUARD_COST);
            }
        }
        return cost;
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

    /** How many keys the player is carrying. */
    public int keysHeld() {
        return keysHeld;
    }

    /** How many the level started with, and therefore how many the exit wants. */
    public int keysRequired() {
        return keysRequired;
    }

    /** The exit only opens once every key on the level has been collected. */
    public boolean hasAllKeys() {
        return keysHeld >= keysRequired;
    }

    public boolean hasEscaped() {
        return escaped;
    }

    // ------------------------------------------------- spotting and the clock

    /**
     * True when the turn just taken ended with the player inside a guard's
     * sight.
     *
     * <p>Being seen is not the end of the run. It is counted, and the player can
     * take the move back, which is what makes the game a puzzle you work out
     * rather than one you lose by surprise.
     */
    public boolean wasSpotted() {
        return spotted;
    }

    /** How many turns have ended with the player in sight, across the run. */
    public int sightings() {
        return sightings;
    }

    /** The turn limit, or {@link #NO_TURN_LIMIT} when the level is untimed. */
    public int turnLimit() {
        return turnLimit;
    }

    public boolean hasTurnLimit() {
        return turnLimit != NO_TURN_LIMIT;
    }

    /** Turns left before the run fails, or -1 when the level is untimed. */
    public int turnsRemaining() {
        return hasTurnLimit() ? Math.max(0, turnLimit - turnsTaken) : -1;
    }

    /** True when the turn limit ran out before the player got away. */
    public boolean hasFailed() {
        return failed;
    }

    /**
     * True when a guard ended the turn standing on the player.
     *
     * <p>Being seen starts a chase; being reached ends the run. Without this,
     * a hunter could walk onto the player and simply stay there, which made the
     * whole stealth layer decorative: there was no reason not to be seen.
     */
    public boolean isCaught() {
        return caught;
    }

    /** Escaped or out of time: either way, no more moves are accepted. */
    /** This attempt is over, but the run may not be: a life is waiting to be spent. */
    public boolean isAttemptOver() {
        return !escaped && (caught || failed);
    }

    /** The whole run is over: either out, or out of lives. */
    public boolean isFinished() {
        return escaped || runOver;
    }

    /** The turn count this level considers a clean route; 0 when unknowable. */
    public int parTurns() {
        return parTurns;
    }

    /**
     * How the run is rated. Meaningful once the player has escaped; before
     * that it reports what they would get if they walked out this instant.
     */
    public Score score() {
        return Score.of(escaped, turnsTaken, parTurns, sightings);
    }

    public int livesLeft() {
        return livesLeft;
    }

    public boolean isRunOver() {
        return runOver;
    }

    /**
     * Spends a life and sets the level back up for another attempt.
     *
     * <p>The player goes back to the start, the keys go back on the floor, the
     * guards go back to their posts and the clock goes back to zero: a life is
     * a clean attempt, not a wounded continuation. Sightings are the exception,
     * because they are the run's score rather than the attempt's.
     *
     * @return true when another attempt began, false when that was the last life
     */
    public boolean beginNextLife() {
        if (!isAttemptOver()) {
            return false;
        }
        livesLeft--;
        if (livesLeft <= 0) {
            livesLeft = 0;
            runOver = true;
            return false;
        }
        respawn();
        return true;
    }

    private void respawn() {
        for (Position keyTile : keySpawns) {
            map.setTileAt(keyTile, TileType.KEY);
        }
        for (int i = 0; i < guards.size(); i++) {
            guards.get(i).applyState(guardSpawns.get(i));
        }
        player = startTile;
        keysHeld = 0;
        turnsTaken = 0;
        spotted = false;
        caught = false;
        failed = false;
        history.clear();
        // The fog keeps what it has explored: a new attempt, not amnesia.
        fog.update(player);
    }
}
