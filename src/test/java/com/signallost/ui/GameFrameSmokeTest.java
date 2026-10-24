package com.signallost.ui;

import com.signallost.progress.ProgressStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Builds the real window and walks it all the way from typing a name to a
 * level running.
 *
 * <p>The other tests cover the rules; this one covers the wiring between
 * screens, which is where a mistyped action name or a screen that forgets to
 * hand over would otherwise only show up by playing. It needs a display, so it
 * stands aside on a headless build server rather than failing there.
 */
class GameFrameSmokeTest {

    @Test
    @DisplayName("Name, then level, then character, then the level is running")
    void theWholeFlowReachesALevel(@TempDir Path folder) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");

        ProgressStore store = new ProgressStore(folder.resolve("progress.properties"));
        AtomicReference<GameFrame> holder = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> holder.set(new GameFrame("level1", store)));
        GameFrame frame = holder.get();

        try {
            JPanel root = (JPanel) frame.getContentPane();

            // A fresh profile means the name screen comes first.
            assertNotNull(find(root, NamePanel.class), "should ask who is playing");
            assertNull(find(root, GamePanel.class), "no level should be running yet");

            type(root, "S");
            type(root, "A");
            type(root, "M");
            fire(root, "confirm");
            assertNotNull(find(root, LevelSelectPanel.class), "should reach the level select");

            fire(root, "confirm");
            assertNotNull(find(root, CharacterSelectPanel.class), "should reach the roster");

            fire(root, "confirm");
            assertNotNull(find(root, GamePanel.class), "should start the level");
            assertNotNull(find(root, HudBar.class), "the level should come with its HUD");
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    private static void type(JPanel root, String letter) throws Exception {
        fire(root, "type" + letter);
    }

    private static void fire(JPanel root, String actionName) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Action action = root.getActionMap().get(actionName);
            assertNotNull(action, actionName + " should be bound");
            action.actionPerformed(new ActionEvent(root, ActionEvent.ACTION_PERFORMED, actionName));
        });
    }

    /**
     * First component of the given type on the screen currently showing.
     *
     * <p>Only visible children are followed, because a CardLayout keeps every
     * card it has ever been given: without that check, the name screen would
     * still be "found" long after the game had moved on from it.
     */
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
