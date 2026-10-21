package com.signallost.ui;

import com.signallost.level.Level;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameState;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;

/**
 * The game window. It shows one of two screens: the character select, or the
 * level itself with a HUD above and the controls below.
 *
 * <p>Input is wired through Swing's {@code InputMap}/{@code ActionMap} rather than
 * a {@code KeyListener}, so keys work regardless of which child component holds
 * focus, and each binding is a named action rather than a branch in a switch.
 * Each action asks which screen is showing, which is why the arrow keys can both
 * browse characters and walk the player.
 */
public final class GameFrame extends JFrame {

    private static final int SIGHT_RADIUS = 5;
    private static final String SELECT_CARD = "select";
    private static final String GAME_CARD = "game";

    private final String levelName;
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final JPanel gameScreen = new JPanel(new BorderLayout());

    private CharacterSelectPanel selectPanel;
    private Critter critter = Critter.defaultChoice();
    private GameState state;
    private GamePanel gamePanel;
    private HudBar hud;

    private boolean choosing = true;

    public GameFrame(String levelName) {
        super("Signal Lost");
        this.levelName = levelName;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        root.setBackground(Palette.BACKDROP);
        gameScreen.setBackground(Palette.BACKDROP);
        setContentPane(root);

        showCharacterSelect();
        bindKeys();

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    // --------------------------------------------------------------- screens

    private void showCharacterSelect() {
        choosing = true;
        selectPanel = new CharacterSelectPanel(critter, this::startRun);

        root.add(selectPanel, SELECT_CARD);
        cards.show(root, SELECT_CARD);
        pack();
    }

    /** Called when a character is confirmed: builds a fresh run and shows it. */
    private void startRun(Critter chosen) {
        critter = chosen;
        choosing = false;
        loadLevel();

        root.add(gameScreen, GAME_CARD);
        cards.show(root, GAME_CARD);
        pack();
    }

    /** Builds a fresh game from the level file and swaps it into the screen. */
    private void loadLevel() {
        Level level = LevelLoader.loadFromResource(levelName);
        state = new GameState(level.map(), level.guards(), SIGHT_RADIUS);
        gamePanel = new GamePanel(state, critter);
        hud = new HudBar(state, critter, levelName);

        gameScreen.removeAll();
        gameScreen.add(hud, BorderLayout.NORTH);
        gameScreen.add(gamePanel, BorderLayout.CENTER);
        gameScreen.add(new FooterBar(), BorderLayout.SOUTH);
        gameScreen.revalidate();
        gameScreen.repaint();
    }

    // ----------------------------------------------------------------- input

    private void bindKeys() {
        // The arrows do double duty: browsing characters, then walking.
        bindDirection("UP", "W", Direction.UP, 0, -1);
        bindDirection("DOWN", "S", Direction.DOWN, 0, 1);
        bindDirection("LEFT", "A", Direction.LEFT, -1, 0);
        bindDirection("RIGHT", "D", Direction.RIGHT, 1, 0);

        bind("SPACE", "waitOrConfirm", () -> {
            if (choosing) {
                selectPanel.confirmSelection();
            } else if (state.movePlayer(Direction.WAIT)) {
                refresh();
            }
        });
        bind("ENTER", "confirm", () -> {
            if (choosing) {
                selectPanel.confirmSelection();
            }
        });
        bind("PERIOD", "wait", () -> {
            if (!choosing && state.movePlayer(Direction.WAIT)) {
                refresh();
            }
        });

        bind("U", "undo", () -> {
            if (!choosing && state.undoLastMove()) {
                refresh();
            }
        });
        bind("H", "toggleHint", () -> {
            if (!choosing) {
                gamePanel.toggleHint();
                hud.repaint();
            }
        });
        bind("M", "toggleReveal", () -> {
            if (!choosing) {
                state.fog().toggleRevealAll();
                refresh();
            }
        });
        bind("R", "restart", () -> {
            if (!choosing) {
                loadLevel();
                pack();
                refresh();
            }
        });
        bind("C", "changeCharacter", () -> {
            if (!choosing) {
                showCharacterSelect();
            }
        });

        // Number keys jump straight to a character while the roster is showing.
        for (int i = 0; i < Critter.values().length; i++) {
            int index = i;
            bind(String.valueOf(i + 1), "pick" + (i + 1), () -> {
                if (choosing) {
                    selectPanel.selectIndex(index);
                }
            });
        }
    }

    /**
     * Binds one arrow, plus its WASD twin, to walking in {@code direction} during
     * play and to moving the highlight on the select screen.
     */
    private void bindDirection(String arrowKey, String letterKey, Direction direction,
                               int columnStep, int rowStep) {
        Runnable action = () -> {
            if (choosing) {
                selectPanel.moveSelection(columnStep, rowStep);
            } else if (state.movePlayer(direction)) {
                refresh();
            }
        };
        String name = "go" + direction.name();
        bind(arrowKey, name, action);
        bind(letterKey, name + "Alt", action);
    }

    private void bind(String keyStroke, String actionName, Runnable action) {
        InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = root.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(keyStroke), actionName);
        actionMap.put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }

    private void refresh() {
        gamePanel.repaint();
        hud.repaint();
    }
}
