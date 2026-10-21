package com.signallost.level;

import com.signallost.guard.Guard;
import com.signallost.guard.GuardFactory;
import com.signallost.model.GameMap;
import com.signallost.model.TileType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a {@link Level} from the plain-text files on the classpath.
 *
 * <p>A file is the grid, then a line of three dashes, then one guard per line:
 *
 * <pre>
 * #######
 * #@...E#
 * #######
 * ---
 * patroller 1,1 1,5
 * hunter 1,3
 * </pre>
 *
 * <p>The guard section is optional, so the grid alone is still a valid level.
 * Keeping levels as text means one can be edited in any editor and reviewed in a
 * diff, and it keeps level data out of the Java source.
 */
public final class LevelLoader {

    /** Separates the grid from the guard list. */
    private static final String SECTION_BREAK = "---";

    private LevelLoader() {
    }

    /** Loads {@code /levels/<name>.txt} from the classpath. */
    public static Level loadFromResource(String name) {
        String path = "/levels/" + name + ".txt";
        try (InputStream in = LevelLoader.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("No level resource at " + path);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return parseLevel(name, lines);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read level " + name, e);
        }
    }

    /** Splits a file into its grid and its guards. */
    public static Level parseLevel(String name, List<String> lines) {
        List<String> gridLines = new ArrayList<>();
        List<String> guardLines = new ArrayList<>();
        boolean inGuardSection = false;

        for (String line : lines) {
            if (line.trim().equals(SECTION_BREAK)) {
                inGuardSection = true;
                continue;
            }
            if (line.isBlank() || line.trim().startsWith("#!")) {
                continue;   // blank lines and "#!" comments are ignored
            }
            if (inGuardSection) {
                guardLines.add(line);
            } else {
                gridLines.add(line);
            }
        }

        GameMap map = parse(gridLines);
        List<Guard> guards = new ArrayList<>();
        for (int i = 0; i < guardLines.size(); i++) {
            Guard guard = GuardFactory.fromSpecification(guardLines.get(i), i + 1);
            requirePostable(map, guard, guardLines.get(i));
            guards.add(guard);
        }
        return new Level(name, map, guards);
    }

    /** Parses lines of level symbols into a map. */
    public static GameMap parse(List<String> lines) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("A level needs at least one row");
        }
        int cols = lines.get(0).length();
        TileType[][] tiles = new TileType[lines.size()][cols];
        for (int row = 0; row < lines.size(); row++) {
            String text = lines.get(row);
            if (text.length() != cols) {
                throw new IllegalArgumentException(
                        "Level row " + row + " is " + text.length() + " wide, expected " + cols);
            }
            for (int col = 0; col < cols; col++) {
                tiles[row][col] = TileType.fromSymbol(text.charAt(col));
            }
        }
        return new GameMap(tiles);
    }

    /**
     * A guard standing in a wall would never move, because A* would find no
     * route out, so a bad waypoint is caught at load time rather than becoming a
     * mysteriously motionless guard.
     */
    private static void requirePostable(GameMap map, Guard guard, String specification) {
        guard.route().forEach(tile -> {
            if (!map.isWalkable(tile)) {
                throw new IllegalArgumentException(
                        "Guard waypoint " + tile + " is not walkable, in: " + specification);
            }
        });
    }
}
