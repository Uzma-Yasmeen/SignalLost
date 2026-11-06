package com.signallost;

import com.signallost.level.BoardScale;
import com.signallost.level.LevelCatalog;
import com.signallost.level.LevelLoader;
import com.signallost.model.GameMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps {@link BoardScale} between its two failure modes.
 *
 * <p>Calibrate the tiles to the biggest level and every level is drawn at the
 * size the biggest one needs, so the early levels shrink to fix the late ones.
 * Calibrate to the smallest and almost every level scrolls. Both are silent, so
 * both get a check.
 */
class BoardScaleTest {

    @Test
    void theEarlyLevelsFitWithoutScrolling() {
        for (LevelCatalog.Entry entry : LevelCatalog.load().subList(0, 4)) {
            GameMap map = LevelLoader.loadFromResource(entry.name()).map();
            assertTrue(map.rows() <= BoardScale.ROWS_THAT_FIT,
                    entry.name() + " is " + map.rows() + " rows but tiles are calibrated to"
                            + " BoardScale.ROWS_THAT_FIT (" + BoardScale.ROWS_THAT_FIT + "), so"
                            + " it scrolls. The first levels should sit on screen whole.");
            assertTrue(map.cols() <= BoardScale.COLS_THAT_FIT,
                    entry.name() + " is " + map.cols() + " columns but tiles are calibrated to"
                            + " BoardScale.COLS_THAT_FIT (" + BoardScale.COLS_THAT_FIT + ").");
        }
    }

    @Test
    void theScaleIsNotCalibratedToTheBiggestLevel() {
        int tallest = 0;
        int widest = 0;
        for (LevelCatalog.Entry entry : LevelCatalog.load()) {
            GameMap map = LevelLoader.loadFromResource(entry.name()).map();
            tallest = Math.max(tallest, map.rows());
            widest = Math.max(widest, map.cols());
        }

        assertTrue(BoardScale.ROWS_THAT_FIT < tallest || tallest == BoardScale.ROWS_THAT_FIT,
                "sanity: ROWS_THAT_FIT (" + BoardScale.ROWS_THAT_FIT + ") exceeds every level");
        assertTrue(BoardScale.ROWS_THAT_FIT <= tallest,
                "BoardScale.ROWS_THAT_FIT is " + BoardScale.ROWS_THAT_FIT + ", larger than the"
                        + " tallest level (" + tallest + "). Every tile in the game is then drawn"
                        + " smaller to leave room for a level that does not exist.");
        assertTrue(BoardScale.COLS_THAT_FIT <= widest,
                "BoardScale.COLS_THAT_FIT is " + BoardScale.COLS_THAT_FIT + ", larger than the"
                        + " widest level (" + widest + "). Every tile is drawn smaller for"
                        + " nothing.");
    }
}
