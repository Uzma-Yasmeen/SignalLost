package com.signallost.level;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The levels the game ships, in order, read from
 * {@code /levels/index.txt}.
 *
 * <p>A list in a file rather than in Java so that adding a level is adding a
 * text file and a line, with nothing to recompile. Each entry is the level's
 * resource name, then the stars needed to unlock it:
 *
 * <pre>
 * level1 0
 * level2 1
 * </pre>
 */
public final class LevelCatalog {

    /**
     * One level on the select screen.
     *
     * @param name          the level resource, e.g. {@code level1}
     * @param starsToUnlock stars needed across all levels before it opens
     */
    public record Entry(String name, int starsToUnlock) {

        /** What the select screen shows: "LEVEL 1" from "level1". */
        public String displayName() {
            return name.replaceAll("(?<=\\D)(?=\\d)", " ").toUpperCase();
        }

        /** The number drawn on the box, or the whole name when there is none. */
        public String shortLabel() {
            String digits = name.replaceAll("\\D+", "");
            return digits.isEmpty() ? name.toUpperCase() : digits;
        }
    }

    private LevelCatalog() {
    }

    public static List<Entry> load() {
        List<Entry> entries = new ArrayList<>();
        try (InputStream in = LevelCatalog.class.getResourceAsStream("/levels/index.txt")) {
            if (in == null) {
                throw new IllegalStateException("No level index at /levels/index.txt");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split("\\s+");
                int stars = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                entries.add(new Entry(parts[0], stars));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the level index", e);
        }
        if (entries.isEmpty()) {
            throw new IllegalStateException("The level index is empty");
        }
        return List.copyOf(entries);
    }
}
