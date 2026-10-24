package com.signallost;

import com.signallost.level.LevelCatalog;
import com.signallost.model.Score;
import com.signallost.progress.LevelResult;
import com.signallost.progress.PlayerProfile;
import com.signallost.progress.ProgressStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the player's name and their best run on each level. */
class ProgressTest {

    private static Score escape(int stars, int turns, int sightings) {
        // Par chosen so the star count comes out as asked.
        int par = stars >= 2 ? turns : turns - 1;
        return new Score(stars, turns, par, sightings, true);
    }

    // ----------------------------------------------------------- best-of rule

    @Test
    @DisplayName("A first escape is always recorded")
    void firstEscapeIsKept() {
        PlayerProfile profile = new PlayerProfile("Sam");

        assertTrue(profile.record("level1", escape(1, 90, 6)));
        assertEquals(1, profile.starsOn("level1"));
        assertTrue(profile.hasCompleted("level1"));
    }

    @Test
    @DisplayName("A run that did not escape is not recorded at all")
    void failedRunsAreNotRecorded() {
        PlayerProfile profile = new PlayerProfile("Sam");

        assertFalse(profile.record("level1", new Score(0, 40, 60, 2, false)));
        assertNull(profile.bestOn("level1"));
        assertEquals(0, profile.starsOn("level1"));
    }

    @Test
    @DisplayName("More stars always wins")
    void moreStarsWins() {
        PlayerProfile profile = new PlayerProfile("Sam");
        profile.record("level1", escape(1, 40, 5));

        assertTrue(profile.record("level1", escape(3, 95, 0)),
                "three stars beats one even though it took longer");
        assertEquals(3, profile.starsOn("level1"));
        assertEquals(95, profile.bestOn("level1").turns());
    }

    @Test
    @DisplayName("A worse run never overwrites a better one")
    void worseRunsAreIgnored() {
        PlayerProfile profile = new PlayerProfile("Sam");
        profile.record("level1", escape(3, 60, 0));

        assertFalse(profile.record("level1", escape(1, 120, 9)));
        assertEquals(3, profile.starsOn("level1"));
        assertEquals(60, profile.bestOn("level1").turns());
    }

    @Test
    @DisplayName("With equal stars, fewer turns wins")
    void fewerTurnsBreaksTheTie() {
        PlayerProfile profile = new PlayerProfile("Sam");
        profile.record("level1", escape(2, 80, 3));

        assertTrue(profile.record("level1", escape(2, 70, 3)));
        assertEquals(70, profile.bestOn("level1").turns());
    }

    @Test
    @DisplayName("With equal stars and turns, fewer sightings wins")
    void fewerSightingsBreaksTheRemainingTie() {
        PlayerProfile profile = new PlayerProfile("Sam");
        profile.record("level1", escape(2, 70, 4));

        assertTrue(profile.record("level1", escape(2, 70, 1)));
        assertEquals(1, profile.bestOn("level1").sightings());
    }

    @Test
    @DisplayName("Stars add up across levels, which is what unlocks later ones")
    void starsAccumulate() {
        PlayerProfile profile = new PlayerProfile("Sam");
        profile.record("level1", escape(3, 60, 0));
        profile.record("level2", escape(2, 120, 2));

        assertEquals(5, profile.totalStars());
    }

    // ------------------------------------------------------------ persistence

    @Test
    @DisplayName("A profile survives being written out and read back")
    void profileRoundTrips(@TempDir Path folder) {
        ProgressStore store = new ProgressStore(folder.resolve("progress.properties"));
        PlayerProfile saved = new PlayerProfile("Uzma");
        saved.record("level1", escape(3, 58, 0));
        saved.record("level2", escape(1, 210, 7));
        store.save(saved);

        PlayerProfile loaded = store.load();

        assertNotNull(loaded);
        assertEquals("Uzma", loaded.name());
        assertEquals(4, loaded.totalStars());
        LevelResult first = loaded.bestOn("level1");
        assertEquals(3, first.stars());
        assertEquals(58, first.turns());
        assertEquals(0, first.sightings());
    }

    @Test
    @DisplayName("No saved file means no profile, not a crash")
    void missingFileLoadsNothing() {
        ProgressStore store = new ProgressStore(Path.of("does", "not", "exist.properties"));

        assertNull(store.load());
    }

    @Test
    @DisplayName("A corrupt file is treated as no profile rather than taken as gospel")
    void corruptFileLoadsNothing(@TempDir Path folder) throws Exception {
        Path file = folder.resolve("progress.properties");
        java.nio.file.Files.writeString(file, "this is not a profile\n");
        ProgressStore store = new ProgressStore(file);

        assertNull(store.load(), "no name means nothing worth loading");
    }

    @Test
    @DisplayName("Saving creates the folder it needs")
    void saveCreatesItsFolder(@TempDir Path folder) {
        Path nested = folder.resolve("deeper").resolve("progress.properties");
        ProgressStore store = new ProgressStore(nested);

        store.save(new PlayerProfile("Sam"));

        assertTrue(java.nio.file.Files.isRegularFile(nested));
    }

    // ---------------------------------------------------------- level catalog

    @Test
    @DisplayName("The level index lists the shipped levels in order")
    void catalogListsLevels() {
        List<LevelCatalog.Entry> levels = LevelCatalog.load();

        assertEquals("level1", levels.get(0).name());
        assertEquals(0, levels.get(0).starsToUnlock(), "the first level is always open");
        assertTrue(levels.size() >= 2);
        assertEquals("LEVEL 1", levels.get(0).displayName());
        assertEquals("level2", levels.get(1).name(), "the second box is the second level");
    }

    @Test
    @DisplayName("Every level in the index can actually be loaded")
    void everyListedLevelExists() {
        for (LevelCatalog.Entry entry : LevelCatalog.load()) {
            assertNotNull(com.signallost.level.LevelLoader.loadFromResource(entry.name()),
                    entry.name() + " is listed but missing");
        }
    }
}
