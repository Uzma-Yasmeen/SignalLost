package com.signallost.ui;

import com.signallost.progress.ProgressStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Presses keys the way a player does: through the input map.
 *
 * <p>This exists because of a bug it would have caught. Undo and restart were
 * bound, and then a later loop bound every letter of the alphabet to typing,
 * which silently replaced them. The game still ran and the tests still passed,
 * because the other window test fired actions by name and so never went
 * through the input map at all. Pressing U did nothing, and only playing the
 * game found out.
 */
class KeyBindingTest {

    @Test
    @DisplayName("U undoes a move, pressed the way a player presses it")
    void undoKeyActuallyUndoes(@TempDir Path folder) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");

        GameFrame frame = startedLevel(folder);
        try {
            JPanel root = (JPanel) frame.getContentPane();
            int before = frame.gameState().turnsTaken();

            press(root, "RIGHT");
            assertEquals(before + 1, frame.gameState().turnsTaken(), "the move should count");

            press(root, "U");
            assertEquals(before, frame.gameState().turnsTaken(), "U should take it back");
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    @Test
    @DisplayName("R restarts the level, pressed the way a player presses it")
    void restartKeyActuallyRestarts(@TempDir Path folder) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");

        GameFrame frame = startedLevel(folder);
        try {
            JPanel root = (JPanel) frame.getContentPane();
            press(root, "RIGHT");
            press(root, "RIGHT");
            assertTrue(frame.gameState().turnsTaken() > 0);

            press(root, "R");

            assertEquals(0, frame.gameState().turnsTaken(), "R should start the level over");
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    @Test
    @DisplayName("Esc steps back one screen at a time rather than trapping the player")
    void escapeStepsBack(@TempDir Path folder) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");

        ProgressStore store = new ProgressStore(folder.resolve("progress.properties"));
        AtomicReference<GameFrame> holder = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> holder.set(new GameFrame("level1", store)));
        GameFrame frame = holder.get();

        try {
            JPanel root = (JPanel) frame.getContentPane();
            press(root, "ENTER");
            press(root, "S");
            press(root, "ENTER");
            press(root, "ENTER");
            assertNotNull(showing(root, LevelSelectPanel.class));

            press(root, "ESCAPE");
            assertNotNull(showing(root, CharacterSelectPanel.class), "back to the roster");

            press(root, "ESCAPE");
            assertNotNull(showing(root, NamePanel.class), "back to the name");

            press(root, "ESCAPE");
            assertNotNull(showing(root, TitlePanel.class), "back to the title");
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    @Test
    @DisplayName("Every letter and arrow is bound to something")
    void everyPlayKeyIsBound(@TempDir Path folder) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");

        GameFrame frame = startedLevel(folder);
        try {
            JPanel root = (JPanel) frame.getContentPane();
            for (String key : new String[]{"UP", "DOWN", "LEFT", "RIGHT",
                    "W", "A", "S", "D", "U", "H", "M", "R", "C", "SPACE", "ENTER", "ESCAPE"}) {
                InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
                assertNotNull(inputMap.get(KeyStroke.getKeyStroke(key)), key + " is not bound");
            }
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    @Test
    @DisplayName("The screens arrive in order: title, name, roster, levels, then the level")
    void screensArriveInOrder(@TempDir Path folder) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");

        ProgressStore store = new ProgressStore(folder.resolve("progress.properties"));
        AtomicReference<GameFrame> holder = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> holder.set(new GameFrame("level1", store)));
        GameFrame frame = holder.get();

        try {
            JPanel root = (JPanel) frame.getContentPane();

            assertNotNull(showing(root, TitlePanel.class), "should open on the title");

            press(root, "ENTER");
            assertNotNull(showing(root, NamePanel.class), "should ask who is playing");

            press(root, "S");
            press(root, "A");
            press(root, "M");
            press(root, "ENTER");
            assertNotNull(showing(root, CharacterSelectPanel.class), "should reach the roster");

            press(root, "ENTER");
            assertNotNull(showing(root, LevelSelectPanel.class), "should reach the level select");

            press(root, "ENTER");
            assertNotNull(showing(root, GamePanel.class), "should start the level");
            assertNotNull(showing(root, HudBar.class), "the level should come with its HUD");
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    /**
     * First component of the given type on the screen currently showing.
     *
     * <p>Only visible children are followed, because a CardLayout keeps every
     * card it has ever been given: without that, the name screen would still be
     * "found" long after the game had moved on from it.
     */
    private static <T extends java.awt.Component> T showing(java.awt.Container parent,
                                                            Class<T> type) {
        for (java.awt.Component child : parent.getComponents()) {
            if (!child.isVisible()) {
                continue;
            }
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof java.awt.Container container) {
                T found = showing(container, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** Walks a fresh window from the name screen into a running level. */
    private static GameFrame startedLevel(Path folder) throws Exception {
        ProgressStore store = new ProgressStore(folder.resolve("progress.properties"));
        AtomicReference<GameFrame> holder = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> holder.set(new GameFrame("level1", store)));
        GameFrame frame = holder.get();

        JPanel root = (JPanel) frame.getContentPane();
        press(root, "ENTER");   // title
        press(root, "S");
        press(root, "A");
        press(root, "M");
        press(root, "ENTER");   // name
        press(root, "ENTER");   // character
        press(root, "ENTER");   // level
        assertNotNull(frame.gameState(), "the level should be running");
        return frame;
    }

    /** Looks the key up in the input map and runs whatever it names. */
    private static void press(JPanel root, String key) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = root.getActionMap();
            Object actionName = inputMap.get(KeyStroke.getKeyStroke(key));
            assertNotNull(actionName, key + " is not bound to anything");
            Action action = actionMap.get(actionName);
            assertNotNull(action, actionName + " has no action");
            action.actionPerformed(new ActionEvent(root, ActionEvent.ACTION_PERFORMED, "press"));
        });
    }
}
