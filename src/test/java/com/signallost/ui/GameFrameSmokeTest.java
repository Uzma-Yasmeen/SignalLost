package com.signallost.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Builds the real window and walks it from the character select to the level.
 *
 * <p>The other tests cover the rules; this one covers the wiring, which is where
 * a mistyped action name or a missing constructor argument would otherwise only
 * show up by running the game by hand. It needs a display, so it stands aside on
 * a headless build server rather than failing there.
 */
class GameFrameSmokeTest {

    @Test
    @DisplayName("The window opens on the roster and Enter starts the level")
    void selectingACharacterStartsTheLevel() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");

        AtomicReference<GameFrame> holder = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> holder.set(new GameFrame("level1")));
        GameFrame frame = holder.get();

        try {
            JPanel root = (JPanel) frame.getContentPane();
            assertNotNull(findDescendant(root, CharacterSelectPanel.class),
                    "the roster should be on screen first");
            assertNull(findDescendant(root, GamePanel.class),
                    "no level should be running yet");

            SwingUtilities.invokeAndWait(() -> {
                Action confirm = root.getActionMap().get("confirm");
                assertNotNull(confirm, "Enter should be bound to a confirm action");
                confirm.actionPerformed(new ActionEvent(root, ActionEvent.ACTION_PERFORMED, "confirm"));
            });

            assertNotNull(findDescendant(root, GamePanel.class),
                    "confirming a character should start the level");
            assertNotNull(findDescendant(root, HudBar.class),
                    "the level should come with its HUD");
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    /** First component of the given type anywhere under {@code parent}, or null. */
    private static <T extends Component> T findDescendant(Container parent, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T found = findDescendant(container, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
