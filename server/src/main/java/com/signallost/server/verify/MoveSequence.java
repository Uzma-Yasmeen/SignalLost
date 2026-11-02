package com.signallost.server.verify;

import com.signallost.model.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * A run written down as one compact string, such as {@code "RRDDLLW"}.
 *
 * <p>The client sends what it <em>did</em> rather than what it scored, so the
 * payload has to be small enough to send on every completed level and strict
 * enough that nothing ambiguous gets through. One letter per turn does both.
 */
public final class MoveSequence {

    /** No level has a turn limit near this, so anything longer is not a run. */
    public static final int MAX_MOVES = 1_000;

    private MoveSequence() {
    }

    public static List<Direction> parse(String moves) {
        if (moves == null || moves.isBlank()) {
            throw new RunRejectedException("A run has to contain at least one move");
        }
        String trimmed = moves.trim();
        if (trimmed.length() > MAX_MOVES) {
            throw new RunRejectedException(
                    "A run of " + trimmed.length() + " moves is longer than any level allows");
        }

        List<Direction> parsed = new ArrayList<>(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            parsed.add(toDirection(trimmed.charAt(i), i));
        }
        return parsed;
    }

    private static Direction toDirection(char letter, int index) {
        return switch (Character.toUpperCase(letter)) {
            case 'U' -> Direction.UP;
            case 'D' -> Direction.DOWN;
            case 'L' -> Direction.LEFT;
            case 'R' -> Direction.RIGHT;
            case 'W' -> Direction.WAIT;
            default -> throw new RunRejectedException(
                    "'" + letter + "' at position " + index
                            + " is not a move; expected U, D, L, R or W");
        };
    }

    /** The other direction, for writing a sequence back out in a response. */
    public static String format(List<Direction> moves) {
        StringBuilder written = new StringBuilder(moves.size());
        for (Direction move : moves) {
            written.append(switch (move) {
                case UP -> 'U';
                case DOWN -> 'D';
                case LEFT -> 'L';
                case RIGHT -> 'R';
                case WAIT -> 'W';
            });
        }
        return written.toString();
    }
}
