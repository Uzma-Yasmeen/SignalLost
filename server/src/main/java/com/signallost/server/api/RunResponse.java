package com.signallost.server.api;

import com.signallost.server.store.Run;

import java.time.Instant;

/**
 * A run as the server scored it.
 *
 * @param id         the stored run
 * @param playerName whose it is
 * @param levelName  which level
 * @param stars      0 to 3, computed here
 * @param turns      turns taken
 * @param par        the turn count this level considers a clean route
 * @param sightings  turns that ended inside a guard's sight
 * @param submittedAt when it arrived
 */
public record RunResponse(
        Long id,
        String playerName,
        String levelName,
        int stars,
        int turns,
        int par,
        int sightings,
        Instant submittedAt) {

    public static RunResponse of(Run run) {
        return new RunResponse(
                run.id(),
                run.player().displayName(),
                run.levelName(),
                run.stars(),
                run.turns(),
                run.par(),
                run.sightings(),
                run.submittedAt());
    }
}
