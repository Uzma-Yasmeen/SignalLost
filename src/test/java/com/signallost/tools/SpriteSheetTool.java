package com.signallost.tools;

import com.signallost.art.Critter;
import com.signallost.art.CritterPixels;
import com.signallost.art.GuardPixels;
import com.signallost.art.ObjectPixels;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Draws the game's artwork as a sheet, for the report.
 *
 * <p>Rendered from {@code CritterPixels}, {@code GuardPixels} and
 * {@code ObjectPixels} rather than from saved images, so the figure in the
 * document is the same data the game draws from and cannot fall out of step
 * with it. Every pixel is a filled square at a whole-number scale, which is how
 * the game draws them too: pixel art resampled smoothly stops being pixel art.
 */
public final class SpriteSheetTool {

    private static final int SCALE = 7;         // one art pixel to seven screen
    private static final int CELL = 16 * SCALE;
    private static final int GAP = 20;
    private static final int LABEL = 26;
    private static final Color BACKDROP = new Color(0x10141E);
    private static final Color TEXT = new Color(0xE6EDF7);
    private static final Color DIM = new Color(0x75849E);

    private SpriteSheetTool() {
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "docs/screens");
        Files.createDirectories(out);

        Critter[] cast = Critter.values();
        String[] modes = {"PATROL", "ALERT", "HUNT"};
        int columns = Math.max(cast.length, modes.length + 3);

        int width = GAP + columns * (CELL + GAP);
        int height = GAP + LABEL + CELL + GAP + LABEL + CELL + GAP * 2 + LABEL;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(BACKDROP);
        g.fillRect(0, 0, width, height);
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 15));

        int y = GAP + LABEL;
        caption(g, "The six playable characters", GAP, y - 8, TEXT);
        for (int i = 0; i < cast.length; i++) {
            int x = GAP + i * (CELL + GAP);
            Critter who = cast[i];
            paint(g, CritterPixels.shapeOf(who), ch -> {
                int packed = CritterPixels.colourOf(who, ch);
                return packed == CritterPixels.TRANSPARENT ? null : new Color(packed);
            }, x, y);
            caption(g, who.displayName(), x, y + CELL + 17, DIM);
        }

        y += CELL + GAP + LABEL + 12;
        caption(g, "One guard, coloured by what it is doing, and the objects", GAP, y - 8, TEXT);
        for (int i = 0; i < modes.length; i++) {
            int x = GAP + i * (CELL + GAP);
            Map<Character, Integer> palette = GuardPixels.paletteFor(modes[i]);
            paint(g, GuardPixels.shape(),
                    ch -> palette.get(ch) == null ? null : new Color(palette.get(ch)), x, y);
            caption(g, modes[i], x, y + CELL + 17, DIM);
        }

        String[] names = {"KEY", "EXIT", "EXIT OPEN"};
        String[][] shapes = {ObjectPixels.key(), ObjectPixels.exit(false), ObjectPixels.exit(true)};
        int[] colours = {ObjectPixels.KEY_COLOUR, ObjectPixels.EXIT_LOCKED,
                         ObjectPixels.EXIT_UNLOCKED};
        for (int i = 0; i < shapes.length; i++) {
            int x = GAP + (modes.length + i) * (CELL + GAP);
            Map<Character, Integer> palette = ObjectPixels.paletteOf(colours[i]);
            paint(g, shapes[i],
                    ch -> palette.get(ch) == null ? null : new Color(palette.get(ch)), x, y);
            caption(g, names[i], x, y + CELL + 17, DIM);
        }

        g.dispose();
        Path target = out.resolve("14-sprites.png");
        ImageIO.write(image, "png", target.toFile());
        System.out.println("wrote " + target.toAbsolutePath() + "  " + width + "x" + height);
    }

    /** One art pixel per filled square, at a whole-number scale. */
    private static void paint(Graphics2D g, String[] rows, Palette palette, int left, int top) {
        for (int row = 0; row < rows.length; row++) {
            for (int col = 0; col < rows[row].length(); col++) {
                Color colour = palette.colourOf(rows[row].charAt(col));
                if (colour == null) {
                    continue;
                }
                g.setColor(colour);
                g.fillRect(left + col * SCALE, top + row * SCALE, SCALE, SCALE);
            }
        }
    }

    private static void caption(Graphics2D g, String text, int x, int y, Color colour) {
        g.setColor(colour);
        g.drawString(text, x, y);
    }

    @FunctionalInterface
    private interface Palette {
        Color colourOf(char symbol);
    }
}
