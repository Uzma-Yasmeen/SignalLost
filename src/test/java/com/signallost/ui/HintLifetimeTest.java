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
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests that a hint answers one turn and then gets out of the way.
 *
 * <p>It used to be a toggle, so one press left the arrow answering every turn
 * for the rest of the level. That is the whole route again, delivered a tile at
 * a time, and it is exactly what a hint should not be.
 */
class HintLifetimeTest {

    @Test
    @DisplayName("A hint lasts the turn it was asked for and no longer")
    void hintExpiresWhenThePlayerMoves(@TempDir Path folder) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");

        GameFrame frame = startedLevel(folder);
        try {
            JPanel root = (JPanel) frame.getContentPane();
            GamePanel board = find(root, GamePanel.class);
            assertNotNull(board);

            assertFalse(board.isHintShowing(), "no hint until it is asked for");

            press(root, "H");
            assertTrue(board.isHintShowing(), "asking should show one");

            press(root, "RIGHT");
            assertFalse(board.isHintShowing(), "and moving should spend it");

            press(root, "H");
            assertTrue(board.isHintShowing(), "asking again should work");
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    @Test
    @DisplayName("Asking twice on the same turn puts it away")
    void askingTwiceHidesIt(@TempDir Path folder) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");

        GameFrame frame = startedLevel(folder);
        try {
            JPanel root = (JPanel) frame.getContentPane();
            GamePanel board = find(root, GamePanel.class);

            press(root, "H");
            assertTrue(board.isHintShowing());
            press(root, "H");
            assertFalse(board.isHintShowing());
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    private static GameFrame startedLevel(Path folder) throws Exception {
        ProgressStore store = new ProgressStore(folder.resolve("progress.properties"));
        AtomicReference<GameFrame> holder = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> holder.set(new GameFrame("level1", store)));
        GameFrame frame = holder.get();

        JPanel root = (JPanel) frame.getContentPane();
        press(root, "ENTER");
        press(root, "S");
        press(root, "ENTER");
        press(root, "ENTER");
        press(root, "ENTER");
        assertNotNull(frame.gameState(), "the level should be running");
        return frame;
    }

    private static void press(JPanel root, String key) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = root.getActionMap();
            Object actionName = inputMap.get(KeyStroke.getKeyStroke(key));
            assertNotNull(actionName, key + " is not bound");
            Action action = actionMap.get(actionName);
            action.actionPerformed(new ActionEvent(root, ActionEvent.ACTION_PERFORMED, "press"));
        });
    }

    private static <T extends Component> T find(Container parent, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (!child.isVisible()) {
                continue;
            }
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T found = find(container, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
