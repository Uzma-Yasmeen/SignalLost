package com.signallost.server.verify;

/**
 * Thrown when a submitted run is not a run: a move that was not legal, a level
 * that does not exist, or a sequence that never reaches the exit.
 *
 * <p>It is deliberately not an "invalid input" exception. A rejected run may be
 * a malformed request, or it may be somebody claiming a score they did not
 * earn, and from the server's point of view those are the same event: the moves
 * do not produce the result claimed, so nothing is recorded.
 */
public class RunRejectedException extends RuntimeException {

    public RunRejectedException(String message) {
        super(message);
    }
}
