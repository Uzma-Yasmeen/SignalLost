package com.signallost.tools;

import com.signallost.level.Level;
import com.signallost.level.LevelCatalog;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameMap;
import com.signallost.model.GameState;
import com.signallost.model.Position;
import com.signallost.model.TileType;
import com.signallost.pathfinding.AStarPathfinder;
import com.signallost.model.Score;
import com.signallost.progress.PlayerProfile;
import com.signallost.ui.CharacterSelectPanel;
import com.signallost.ui.LevelSelectPanel;
import com.signallost.ui.NamePanel;
import com.signallost.ui.TitlePanel;
import com.signallost.ui.Critter;
import com.signallost.ui.FooterBar;
import com.signallost.ui.GamePanel;
import com.signallost.ui.HudBar;
import com.signallost.ui.Palette;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A development tool, not part of the game: lays the real UI out off-screen and
 * writes it to PNG, so the look can be reviewed and dropped into the report
 * without replaying the level by hand.
 *
 * <p>The walks are driven by A* rather than hard-coded key presses, so the
 * screenshots keep working when the level is redrawn.
 *
 * <pre>
 * java -cp target/classes;target/test-classes com.signallost.tools.ScreenshotTool out-dir
 * </pre>
 */
public final class ScreenshotTool {

    private static final int SIGHT_RADIUS = 5;
    private static final int SCALE = 2;

    private ScreenshotTool() {
    }

    public static void main(String[] args) throws Exception {
        Path outputDir = Path.of(args.length > 0 ? args[0] : "docs/screens");
        Files.createDirectories(outputDir);

        SwingUtilities.invokeAndWait(() -> {
            try {
                // Each level's shape, revealed, so the set can be compared.
                captureLayout(outputDir.resolve("10-level2-rooms.png"), "level2", Critter.FOX);
                captureLayout(outputDir.resolve("11-level3-rings.png"), "level3", Critter.FROG);
                captureLayout(outputDir.resolve("12-level4-facility.png"), "level4", Critter.PANDA);

                captureTitle(outputDir.resolve("13-title.png"));

                // Who is playing, and what they have done so far.
                captureName(outputDir.resolve("08-name.png"));
                captureLevels(outputDir.resolve("09-level-select.png"));

                // The roster, before a run starts.
                captureSelect(outputDir.resolve("00-character-select.png"));

                // The opening position: almost everything is still dark.
                capture(outputDir.resolve("01-start.png"), Critter.CAT, false,
                        state -> state.turnsTaken() >= 1);

                // Part way to the key, with a trail of remembered tiles.
                capture(outputDir.resolve("02-explored.png"), Critter.CAT, false,
                        state -> state.turnsTaken() >= 9);

                // The first turn that ends inside a guard's line of sight.
                capture(outputDir.resolve("03-spotted.png"), Critter.FOX, false,
                        GameState::wasSpotted);

                // A clean escape, to show the rating.
                captureEscape(outputDir.resolve("07-escaped.png"), Critter.BUNNY);

                // Run straight at the hunter until it catches up.
                capture(outputDir.resolve("06-caught.png"), Critter.PANDA, false,
                        GameState::isCaught);

                // The suggested route, dodging the guards' sight.
                captureWithHint(outputDir.resolve("04-hint.png"), Critter.BUNNY,
                        state -> state.turnsTaken() >= 11);

                // Everything revealed, which is how the guard logic gets checked.
                capture(outputDir.resolve("05-dev-reveal.png"), Critter.OWL, true,
                        state -> state.turnsTaken() >= 13);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("Wrote screenshots to " + outputDir.toAbsolutePath());
    }

    /**
     * Walks the level out with the guards left at home, so the escape screen
     * can be seen. A scripted player cannot reliably beat the real thing.
     */
    private static void captureEscape(Path target, Critter critter) throws IOException {
        Level level = LevelLoader.loadFromResource("level1");
        GameState state = new GameState(level.map(), List.of(), SIGHT_RADIUS, level.turnLimit());

        // Follow the game's own suggested route, which knows there is more
        // than one key to collect before the exit will open.
        while (!state.hasEscaped() && state.turnsTaken() < level.turnLimit()) {
            List<Position> hint = state.hintRoute(1);
            if (hint.isEmpty() || !state.movePlayer(between(state.player(), hint.get(0)))) {
                break;
            }
        }

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Palette.BACKDROP);
        root.add(new HudBar(state, critter, "level1"), BorderLayout.NORTH);
        root.add(new GamePanel(state, critter), BorderLayout.CENTER);
        root.add(new FooterBar(), BorderLayout.SOUTH);
        write(target, root);
    }

    /** A whole level with the fog lifted, which is how the shapes get compared. */
    private static void captureLayout(Path target, String levelName, Critter critter)
            throws IOException {
        Level level = LevelLoader.loadFromResource(levelName);
        GameState state = new GameState(
                level.map(), level.guards(), SIGHT_RADIUS, level.turnLimit());
        state.fog().setRevealAll(true);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Palette.BACKDROP);
        root.add(new HudBar(state, critter, levelName), BorderLayout.NORTH);
        root.add(new GamePanel(state, critter), BorderLayout.CENTER);
        root.add(new FooterBar(), BorderLayout.SOUTH);
        write(target, root);
    }

    private static void captureTitle(Path target) throws IOException {
        PlayerProfile profile = new PlayerProfile("Uzma");
        profile.record("level1", new Score(2, 61, 69, 4, true));
        write(target, new TitlePanel(profile, LevelCatalog.load().size()));
    }

    private static void captureName(Path target) throws IOException {
        NamePanel panel = new NamePanel("Uzma", chosen -> {
            // Nothing to start: this render never leaves the screen.
        });
        write(target, panel);
    }

    /** The level grid, with a profile part way through, so the boxes have stars. */
    private static void captureLevels(Path target) throws IOException {
        PlayerProfile profile = new PlayerProfile("Uzma");
        profile.record("level1", new Score(2, 61, 69, 4, true));

        LevelSelectPanel panel = new LevelSelectPanel(LevelCatalog.load(), profile, entry -> {
            // Nothing to start.
        });
        write(target, panel);
    }

    private static void captureSelect(Path target) throws IOException {
        CharacterSelectPanel panel = new CharacterSelectPanel(Critter.CAT, chosen -> {
            // Nothing to start: this render never runs a level.
        });
        write(target, panel);
    }

    /**
     * Walks the level towards the key and then the exit, stopping at the first
     * turn that satisfies {@code stopWhen}, and renders that moment.
     */
    private static void captureWithHint(Path target, Critter critter,
                                        Predicate<GameState> stopWhen) throws IOException {
        capture(target, critter, false, stopWhen, true);
    }

    private static void capture(Path target, Critter critter, boolean revealAll,
                                Predicate<GameState> stopWhen) throws IOException {
        capture(target, critter, revealAll, stopWhen, false);
    }

    private static void capture(Path target, Critter critter, boolean revealAll,
                                Predicate<GameState> stopWhen, boolean showHint)
            throws IOException {
        Level level = LevelLoader.loadFromResource("level1");

        // The route has to be planned first: building the game turns the start
        // marker into ordinary floor, so findFirst(START) would come back null.
        List<Direction> route = plannedRoute(level.map());

        GameState state = new GameState(
                level.map(), level.guards(), SIGHT_RADIUS, level.turnLimit());
        for (Direction move : route) {
            if (stopWhen.test(state)) {
                break;
            }
            state.movePlayer(move);
        }
        state.fog().setRevealAll(revealAll);

        GamePanel gamePanel = new GamePanel(state, critter);
        if (showHint) {
            gamePanel.toggleHint();
        }

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Palette.BACKDROP);
        root.add(new HudBar(state, critter, "level1"), BorderLayout.NORTH);
        root.add(gamePanel, BorderLayout.CENTER);
        root.add(new FooterBar(), BorderLayout.SOUTH);

        write(target, root);
    }

    /** Start, then every key nearest-first, then the exit, as a list of moves. */
    private static List<Direction> plannedRoute(GameMap map) {
        Position at = map.findFirst(TileType.START);
        List<Position> remaining = new ArrayList<>(map.findAll(TileType.KEY));
        List<Direction> moves = new ArrayList<>();

        while (!remaining.isEmpty()) {
            Position nearest = null;
            int best = Integer.MAX_VALUE;
            for (Position key : remaining) {
                int steps = AStarPathfinder.findPath(map, at, key).size();
                if (steps > 0 && steps < best) {
                    best = steps;
                    nearest = key;
                }
            }
            if (nearest == null) {
                break;
            }
            moves.addAll(asDirections(AStarPathfinder.findPath(map, at, nearest)));
            at = nearest;
            remaining.remove(nearest);
        }
        moves.addAll(asDirections(AStarPathfinder.findPath(map, at, map.findFirst(TileType.EXIT))));
        return moves;
    }

    private static List<Direction> asDirections(List<Position> path) {
        List<Direction> moves = new ArrayList<>();
        for (int i = 1; i < path.size(); i++) {
            moves.add(between(path.get(i - 1), path.get(i)));
        }
        return moves;
    }

    private static Direction between(Position from, Position to) {
        if (to.row() < from.row()) {
            return Direction.UP;
        }
        if (to.row() > from.row()) {
            return Direction.DOWN;
        }
        return to.col() < from.col() ? Direction.LEFT : Direction.RIGHT;
    }

    private static void write(Path target, JPanel root) throws IOException {
        // Packing a frame is what gives the layout real sizes; we never show it.
        JFrame frame = new JFrame();
        frame.setContentPane(root);
        frame.pack();

        BufferedImage image = new BufferedImage(
                root.getWidth() * SCALE, root.getHeight() * SCALE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(SCALE, SCALE);
        root.printAll(g);
        g.dispose();
        frame.dispose();

        File file = target.toFile();
        ImageIO.write(image, "png", file);
        System.out.println("  " + file.getName() + "  " + image.getWidth() + "x" + image.getHeight());
    }
}
