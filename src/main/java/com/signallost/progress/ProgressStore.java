package com.signallost.progress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Map;

/**
 * Keeps a profile on disk between sessions.
 *
 * <p>A {@code .properties} file rather than JSON, because it needs no library,
 * it is plain text a person can open and read, and a corrupt or half-written
 * one can be deleted without ceremony. The format is deliberately boring:
 *
 * <pre>
 * player.name=Sam
 * level1.stars=3
 * level1.turns=58
 * level1.sightings=0
 * </pre>
 *
 * <p>Nothing here throws at the caller. A profile that cannot be read is a
 * fresh profile, and a profile that cannot be written is a lost high score —
 * neither is worth taking the game down for.
 */
public final class ProgressStore {

    private static final String NAME_KEY = "player.name";

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

    /** Loads the saved profile, or null when there is not one yet. */
    public PlayerProfile load() {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (IOException e) {
            System.err.println("Could not read " + file + ": " + e.getMessage());
            return null;
        }

        String name = properties.getProperty(NAME_KEY);
        if (name == null || name.isBlank()) {
            return null;
        }

        PlayerProfile profile = new PlayerProfile(name);
        for (String key : properties.stringPropertyNames()) {
            if (!key.endsWith(".stars")) {
                continue;
            }
            String level = key.substring(0, key.length() - ".stars".length());
            profile.put(level, new LevelResult(
                    readInt(properties, level + ".stars"),
                    readInt(properties, level + ".turns"),
                    readInt(properties, level + ".sightings")));
        }
        return profile;
    }

    /** Writes the profile out, quietly giving up if the disk will not have it. */
    public void save(PlayerProfile profile) {
        Properties properties = new Properties();
        properties.setProperty(NAME_KEY, profile.name());
        for (Map.Entry<String, LevelResult> entry : profile.allResults().entrySet()) {
            String level = entry.getKey();
            LevelResult result = entry.getValue();
            properties.setProperty(level + ".stars", String.valueOf(result.stars()));
            properties.setProperty(level + ".turns", String.valueOf(result.turns()));
            properties.setProperty(level + ".sightings", String.valueOf(result.sightings()));
        }

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
