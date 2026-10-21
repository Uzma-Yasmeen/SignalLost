package com.signallost;

import com.signallost.ui.Critter;
import com.signallost.ui.CritterArt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the game draws a character whether or not artwork is installed.
 *
 * <p>The repository ships no sprites, so these run against the fallback. They
 * exist to catch the case that would otherwise only show up as an empty tile:
 * a missing file taking the renderer down with it.
 */
class CritterArtTest {

    @Test
    @DisplayName("With no artwork installed, every character still draws")
    void everyCritterDrawsWithoutArtwork() {
        BufferedImage canvas = new BufferedImage(120, 120, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        try {
            for (Critter critter : Critter.values()) {
                assertDoesNotThrow(() -> CritterArt.paint(g, critter, 60, 60, 40),
                        critter + " failed to draw");
            }
        } finally {
            g.dispose();
        }
    }

    @Test
    @DisplayName("A frame counter past the end of an animation wraps instead of failing")
    void frameCounterWrapsSafely() {
        BufferedImage canvas = new BufferedImage(120, 120, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        try {
            assertDoesNotThrow(() -> CritterArt.paint(g, Critter.CAT, 60, 60, 40, 9999));
            assertDoesNotThrow(() -> CritterArt.paint(g, Critter.CAT, 60, 60, 40, -3));
        } finally {
            g.dispose();
        }
    }

    @Test
    @DisplayName("Drawing actually puts pixels on the canvas")
    void drawingMarksTheCanvas() {
        BufferedImage canvas = new BufferedImage(120, 120, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        CritterArt.paint(g, Critter.CAT, 60, 60, 40);
        g.dispose();

        boolean anythingDrawn = false;
        for (int x = 0; x < canvas.getWidth() && !anythingDrawn; x++) {
            for (int y = 0; y < canvas.getHeight(); y++) {
                if ((canvas.getRGB(x, y) >>> 24) != 0) {
                    anythingDrawn = true;
                    break;
                }
            }
        }
        assertTrue(anythingDrawn, "the character should have been painted");
    }
}
