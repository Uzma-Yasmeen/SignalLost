package com.signallost.tools;

import com.signallost.level.Level;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameMap;
import com.signallost.model.GameState;
import com.signallost.model.Position;
import com.signallost.model.TileType;
import com.signallost.pathfinding.AStarPathfinder;
import com.signallost.ui.CharacterSelectPanel;
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

    /** Start to key to exit, as a list of moves, found with A*. */
    private static List<Direction> plannedRoute(GameMap map) {
        Position start = map.findFirst(TileType.START);
        Position key = map.findFirst(TileType.KEY);
        Position exit = map.findFirst(TileType.EXIT);

        List<Direction> moves = new ArrayList<>();
        moves.addAll(asDirections(AStarPathfinder.findPath(map, start, key)));
        moves.addAll(asDirections(AStarPathfinder.findPath(map, key, exit)));
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
