package com.signallost.ui;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Draws a character, preferring artwork on the classpath and falling back to the
 * shapes in {@link CritterRenderer} when there is none.
 *
 * <p>This is the seam for adding your own sprites. Drop PNG files into
 * {@code src/main/resources/sprites} and they are picked up at startup; delete
 * them and the drawn-in-code characters come back. No Java changes either way,
 * so artwork can be swapped, compared and reverted without touching the game.
 *
 * <p><b>Naming.</b> One still image per character:
 * <pre>
 *   sprites/cat.png
 * </pre>
 * or numbered frames for an animation, starting at 1 and counting up with no
 * gaps:
 * <pre>
 *   sprites/cat-1.png
 *   sprites/cat-2.png
 *   sprites/cat-3.png
 * </pre>
 * The names are the {@link Critter} constants in lower case: {@code cat},
 * {@code fox}, {@code panda}, {@code frog}, {@code owl}, {@code bunny}. Numbered
 * frames win when both exist. A character with no file keeps its drawn version,
 * so artwork can arrive one animal at a time.
 *
 * <p>Images are scaled with nearest-neighbour rather than smooth interpolation,
 * because pixel art that gets blurred on the way to the screen stops being pixel
 * art. That costs nothing for a smooth sprite and saves a pixelled one.
 */
public final class CritterArt {

    /** Frames are looked for from 1 up to here before giving up. */
    private static final int MAX_FRAMES = 16;

    private static final Map<Critter, List<BufferedImage>> FRAMES = new EnumMap<>(Critter.class);

    static {
        for (Critter critter : Critter.values()) {
            FRAMES.put(critter, loadFrames(critter));
        }
    }

    private CritterArt() {
    }

    /** How many animation frames this character has; 0 when it is drawn in code. */
    public static int frameCount(Critter critter) {
        return FRAMES.get(critter).size();
    }

    /** True when any character has artwork, so the game knows to animate at all. */
    public static boolean anyArtworkPresent() {
        return FRAMES.values().stream().anyMatch(frames -> !frames.isEmpty());
    }

    /**
     * Paints the character centred on {@code (cx, cy)} at the given head radius.
     *
     * @param frame a counter that rises over time; it is wrapped to the number of
     *              frames available, so callers never have to know the count
     */
    public static void paint(Graphics2D g, Critter critter, double cx, double cy,
                             double r, int frame) {
        List<BufferedImage> frames = FRAMES.get(critter);
        if (frames.isEmpty()) {
            CritterRenderer.paint(g, critter, cx, cy, r);
            return;
        }
        BufferedImage image = frames.get(Math.floorMod(frame, frames.size()));
        drawFitted(g, image, cx, cy, r);
    }

    /** The still version, for places that do not animate. */
    public static void paint(Graphics2D g, Critter critter, double cx, double cy, double r) {
        paint(g, critter, cx, cy, r, 0);
    }

    /**
     * Scales the image to fit a box around the head radius without distorting it,
     * so sprites of different proportions all sit sensibly on a tile.
     */
    private static void drawFitted(Graphics2D g, BufferedImage image,
                                   double cx, double cy, double r) {
        double box = r * 2.3;
        double scale = Math.min(box / image.getWidth(), box / image.getHeight());
        double width = image.getWidth() * scale;
        double height = image.getHeight() * scale;

        Object previous = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image,
                (int) Math.round(cx - width / 2), (int) Math.round(cy - height / 2),
                (int) Math.round(width), (int) Math.round(height), null);
        if (previous != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, previous);
        }
    }

    private static List<BufferedImage> loadFrames(Critter critter) {
        String base = "/sprites/" + critter.name().toLowerCase();

        List<BufferedImage> numbered = new ArrayList<>();
        for (int i = 1; i <= MAX_FRAMES; i++) {
            BufferedImage frame = read(base + "-" + i + ".png");
            if (frame == null) {
                break;   // frames must be consecutive, so the first gap ends the run
            }
            numbered.add(frame);
        }
        if (!numbered.isEmpty()) {
            return List.copyOf(numbered);
        }

        BufferedImage single = read(base + ".png");
        return single == null ? List.of() : List.of(single);
    }

    /** Reads a classpath image, returning null when it is simply not there. */
    private static BufferedImage read(String resourcePath) {
        try (InputStream in = CritterArt.class.getResourceAsStream(resourcePath)) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException e) {
            System.err.println("Could not read sprite " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }
}
