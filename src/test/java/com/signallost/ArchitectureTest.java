package com.signallost;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps the game logic free of any user interface.
 *
 * <p>Every rule in the game — the fog, the pathfinding, the guards, the undo
 * stack, the scoring — is written without a single reference to Swing or AWT.
 * That is why the whole test suite runs without opening a window, and it is
 * also why this game could be put on a phone by writing a new front end rather
 * than a new game: Android has no Swing, but it has all of {@code java.util}.
 *
 * <p>That property is worth one line of {@code import} away from being lost
 * forever, and nothing else would notice. A model class that reaches for
 * {@code java.awt.Color} because it is a convenient way to hold a colour
 * compiles perfectly, passes every other test, and quietly welds the rules to
 * the desktop. This test is what stops that.
 */
class ArchitectureTest {

    /** Packages that hold rules, and must stay independent of how they are drawn. */
    private static final String[] LOGIC_PACKAGES = {
        "model", "vision", "pathfinding", "guard", "level", "progress"
    };

    private static final String[] FORBIDDEN = {"java.awt", "javax.swing", "java.applet"};

    @ParameterizedTest(name = "{0} stays free of the user interface")
    @ValueSource(strings = {"model", "vision", "pathfinding", "guard", "level", "progress"})
    void logicDoesNotDependOnSwing(String packageName) {
        Path folder = Path.of("src", "main", "java", "com", "signallost", packageName);
        assertTrue(Files.isDirectory(folder), "cannot find " + folder);

        List<String> offences = new ArrayList<>();
        for (Path source : javaFilesIn(folder)) {
            for (String line : readLines(source)) {
                String trimmed = line.strip();
                if (!trimmed.startsWith("import ")) {
                    continue;
                }
                for (String banned : FORBIDDEN) {
                    if (trimmed.contains(banned)) {
                        offences.add(source.getFileName() + ": " + trimmed);
                    }
                }
            }
        }

        assertTrue(offences.isEmpty(),
                "The rules must not know how they are drawn, or the game cannot be "
                        + "put on anything that is not a desktop. Found:\n  "
                        + String.join("\n  ", offences));
    }

    /**
     * The only part of the logic that touches the outside world is the save
     * file, and it has to stay injectable: a phone has no home directory to
     * speak of, so the path comes from the caller.
     */
    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("Progress can be told where to save, not just assume")
    void progressStoreTakesItsPath() {
        boolean hasPathConstructor = Stream.of(
                        com.signallost.progress.ProgressStore.class.getConstructors())
                .anyMatch(constructor -> constructor.getParameterCount() == 1
                        && constructor.getParameterTypes()[0] == Path.class);

        assertTrue(hasPathConstructor,
                "ProgressStore must accept a path so a platform without a home "
                        + "directory can hand it one");
    }

    private static List<Path> javaFilesIn(Path folder) {
        try (Stream<Path> files = Files.walk(folder)) {
            return files.filter(path -> path.toString().endsWith(".java")).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + folder, e);
        }
    }

    private static List<String> readLines(Path source) {
        try {
            return Files.readAllLines(source);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + source, e);
        }
    }
}
