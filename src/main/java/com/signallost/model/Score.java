package com.signallost.model;

/**
 * How well a run went: one star for getting out, and one each for the two ways
 * of getting out well.
 *
 * <p>The counter for sightings used to be a number that rose and never paid
 * off, which teaches a player to ignore it. This is what it was always for.
 * Having two separate bars rather than one blended score also means the second
 * playthrough has a goal the first did not: you beat the level, then you go
 * back and beat it <em>unseen</em>, which is the loop every stealth game runs
 * on.
 *
 * @param stars      0 to 3; zero only when the player never got out
 * @param turns      turns the run took
 * @param par        the turn count the level considers a clean route
 * @param sightings  turns that ended inside a guard's sight
 * @param escaped    whether the player got out at all
 */
public record Score(int stars, int turns, int par, int sightings, boolean escaped) {

    public static final int MAX_STARS = 3;

    /** Works out the rating for a finished run. */
    public static Score of(boolean escaped, int turns, int par, int sightings) {
        if (!escaped) {
            return new Score(0, turns, par, sightings, false);
        }
        int stars = 1;                       // getting out at all
        if (par > 0 && turns <= par) {
            stars++;                         // and doing it briskly
        }
        if (sightings == 0) {
            stars++;                         // and doing it unseen
        }
        return new Score(stars, turns, par, sightings, true);
    }

    public boolean withinPar() {
        return escaped && par > 0 && turns <= par;
    }

    public boolean unseen() {
        return escaped && sightings == 0;
    }

    /** A short line naming what was and was not earned. */
    public String summary() {
        if (!escaped) {
            return "no escape";
        }
        String pace = withinPar() ? "under par" : turns + " turns, par " + par;
        String stealth = unseen()
                ? "never seen"
                : (sightings == 1 ? "seen once" : "seen " + sightings + " times");
        return pace + "  ·  " + stealth;
    }
}
