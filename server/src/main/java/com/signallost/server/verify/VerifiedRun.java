package com.signallost.server.verify;

/**
 * What a submitted run turned out to be worth, once the server had played it.
 *
 * <p>Every number here was computed on this side. Nothing the client sent about
 * its own score is carried through, because nothing about its score was asked
 * for: the client sends moves, and this is the result of making them.
 *
 * @param levelName the level played
 * @param stars     0 to 3
 * @param turns     turns the run took
 * @param par       the turn count the level considers a clean route
 * @param sightings turns that ended inside a guard's sight
 * @param moveCount how many moves were submitted, which equals turns for a
 *                  run with no refused moves
 */
public record VerifiedRun(
        String levelName,
        int stars,
        int turns,
        int par,
        int sightings,
        int moveCount) {
}
