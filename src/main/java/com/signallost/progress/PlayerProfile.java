package com.signallost.progress;

import com.signallost.model.Score;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Who is playing, and the best they have done on each level.
 *
 * <p>Only an improvement is kept, so a bad run never costs a player the record
 * of a good one. That matters more here than in most games: the stealth star
 * rewards a slow, careful route, and without a best-of rule a player chasing it
 * would watch their fast two-star time get overwritten by every failed attempt.
 */
public final class PlayerProfile {

    private String name;
    private final Map<String, LevelResult> best = new LinkedHashMap<>();

    public PlayerProfile(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public void rename(String newName) {
        this.name = newName;
    }

    /**
     * Files a finished run.
     *
     * @return true when it was an improvement and has been kept
     */
    public boolean record(String levelName, Score score) {
        if (!score.escaped()) {
            return false;
        }
        LevelResult result = LevelResult.from(score);
        if (!result.beats(best.get(levelName))) {
            return false;
        }
        best.put(levelName, result);
        return true;
    }

    /** The best run on a level, or null when it has never been finished. */
    public LevelResult bestOn(String levelName) {
        return best.get(levelName);
    }

    public int starsOn(String levelName) {
        LevelResult result = best.get(levelName);
        return result == null ? 0 : result.stars();
    }

    public boolean hasCompleted(String levelName) {
        return best.containsKey(levelName);
    }

    /** Every star earned across every level, which is what unlocks later ones. */
    public int totalStars() {
        return best.values().stream().mapToInt(LevelResult::stars).sum();
    }

    public Map<String, LevelResult> allResults() {
        return Collections.unmodifiableMap(best);
    }

    /** Used by the store when loading; replaces whatever was there. */
    void put(String levelName, LevelResult result) {
        best.put(levelName, result);
    }
}
