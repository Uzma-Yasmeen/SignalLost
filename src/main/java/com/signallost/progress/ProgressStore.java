package com.signallost.progress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Keeps players' progress on disk between sessions.
 *
 * <p>It lives in the player's own home folder, not beside the game, so nothing
 * is carried along when the game is copied or shared: somebody opening it on
 * their own machine finds no file and starts at zero.
 *
 * <p><b>Each name gets its own progress.</b> One shared profile meant a friend
 * who sat down at your laptop and typed their name inherited your stars, and
 * saving overwrote yours with theirs. Keyed by name, both of you keep what you
 * earned and swapping back restores it.
 *
 * <p>A {@code .properties} file rather than JSON, because it needs no library,
 * it is plain text a person can open and read, and a corrupt one can be deleted
 * without ceremony:
 *
 * <pre>
 * player.last=sam
 * player.sam.name=Sam
 * player.sam.level1.stars=3
 * player.sam.level1.turns=58
 * player.sam.level1.sightings=0
 * </pre>
 *
 * <p>Nothing here throws at the caller. A profile that cannot be read is a
 * fresh profile, and one that cannot be written is a lost high score — neither
 * is worth taking the game down for.
 */
public final class ProgressStore {

    private static final String LAST_PLAYER_KEY = "player.last";
    private static final String PREFIX = "player.";
    private static final String NAME_SUFFIX = ".name";

    private final Path file;

    public ProgressStore(Path file) {
        this.file = file;
    }

    /** The usual place: a dot-directory in the player's home folder. */
    public static ProgressStore inUserHome() {
        return new ProgressStore(
                Path.of(System.getProperty("user.home"), ".signal-lost", "progress.properties"));
    }

    public Path file() {
        return file;
    }

    /** Whoever played last, so the title screen can greet them; null if nobody has. */
    public PlayerProfile load() {
        Properties saved = read();
        String lastKey = saved.getProperty(LAST_PLAYER_KEY);
        if (lastKey == null || lastKey.isBlank()) {
            return null;
        }
        return profileFrom(saved, lastKey);
    }

    /**
     * That player's saved progress, or a fresh profile when the name is new.
     *
     * <p>This is what keeps one person's stars off another person's screen: a
     * name nobody has used before starts at zero, and an existing one picks up
     * where it left off.
     */
    public PlayerProfile loadFor(String name) {
        PlayerProfile existing = profileFrom(read(), keyFor(name));
        return existing != null ? existing : new PlayerProfile(name);
    }

    /** Writes this player's progress, leaving everybody else's alone. */
    public void save(PlayerProfile profile) {
        Properties saved = read();
        String key = keyFor(profile.name());

        saved.setProperty(LAST_PLAYER_KEY, key);
        saved.setProperty(PREFIX + key + NAME_SUFFIX, profile.name());
        for (Map.Entry<String, LevelResult> entry : profile.allResults().entrySet()) {
            String base = PREFIX + key + "." + entry.getKey();
            LevelResult result = entry.getValue();
            saved.setProperty(base + ".stars", String.valueOf(result.stars()));
            saved.setProperty(base + ".turns", String.valueOf(result.turns()));
            saved.setProperty(base + ".sightings", String.valueOf(result.sightings()));
        }
        write(saved);
    }

    // ------------------------------------------------------------- internals

    private PlayerProfile profileFrom(Properties saved, String key) {
        String name = saved.getProperty(PREFIX + key + NAME_SUFFIX);
        if (name == null || name.isBlank()) {
            return null;
        }

        PlayerProfile profile = new PlayerProfile(name);
        String base = PREFIX + key + ".";
        for (String property : saved.stringPropertyNames()) {
            if (!property.startsWith(base) || !property.endsWith(".stars")) {
                continue;
            }
            String level = property.substring(base.length(), property.length() - ".stars".length());
            profile.put(level, new LevelResult(
                    readInt(saved, base + level + ".stars"),
                    readInt(saved, base + level + ".turns"),
                    readInt(saved, base + level + ".sightings")));
        }
        return profile;
    }

    /**
     * A file-safe key for a name.
     *
     * <p>Lower case and letters and digits only, so "Sam" and "sam " are the
     * same player and nothing in the name can break the file format.
     */
    private static String keyFor(String name) {
        String key = name.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
        return key.isEmpty() ? "player" : key;
    }

    private Properties read() {
        Properties properties = new Properties();
        if (!Files.isRegularFile(file)) {
            return properties;
        }
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (IOException e) {
            System.err.println("Could not read " + file + ": " + e.getMessage());
        }
        return properties;
    }

    private void write(Properties properties) {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream out = Files.newOutputStream(file)) {
                properties.store(out, "Signal Lost progress");
            }
        } catch (IOException e) {
            System.err.println("Could not save progress to " + file + ": " + e.getMessage());
        }
    }

    private static int readInt(Properties properties, String key) {
        try {
            return Integer.parseInt(properties.getProperty(key, "0").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
