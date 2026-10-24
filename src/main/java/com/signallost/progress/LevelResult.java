package com.signallost.progress;

import com.signallost.model.Score;

/**
 * The best run a player has managed on one level.
 *
 * @param stars     how many of the three were earned
 * @param turns     the turn count of that run
 * @param sightings how many turns ended inside a guard's sight
 */
public record LevelResult(int stars, int turns, int sightings) {

    public static LevelResult from(Score score) {
        return new LevelResult(score.stars(), score.turns(), score.sightings());
    }

    /**
     * Whether this run beats {@code previous}.
     *
     * <p>More stars always wins. Between runs with the same stars, fewer turns
     * wins, and sightings break any remaining tie — so a player chasing the
     * stealth star still sees their best sighting count improve even on the
     * runs where the star count does not move.
     */
    public boolean beats(LevelResult previous) {
        if (previous == null) {
            return true;
        }
        if (stars != previous.stars) {
            return stars > previous.stars;
        }
        if (turns != previous.turns) {
            return turns < previous.turns;
        }
        return sightings < previous.sightings;
    }
}
