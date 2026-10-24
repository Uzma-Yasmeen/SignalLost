package com.signallost.ui;

import com.signallost.level.Level;
import com.signallost.level.LevelCatalog;
import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameState;
import com.signallost.progress.PlayerProfile;
import com.signallost.progress.ProgressStore;

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
import java.util.List;

/**
 * The game window, and the route between its screens:
 *
 * <pre>
 *   name  ->  level select  ->  character select  ->  the level
 *               ^                                        |
 *               +-------------- escape, or R ------------+
 * </pre>
 *
 * <p>Input is wired through Swing's {@code InputMap}/{@code ActionMap} rather
 * than a {@code KeyListener}, so keys work whichever component holds focus, and
 * each binding is a named action rather than a branch in a switch. Every action
 * asks which screen is showing, which is how the arrow keys can browse levels,
 * browse characters and walk the player.
 */
public final class GameFrame extends JFrame {

    private static final int SIGHT_RADIUS = 5;

    private static final String NAME_CARD = "name";
    private static final String LEVELS_CARD = "levels";
    private static final String SELECT_CARD = "select";
    private static final String GAME_CARD = "game";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final JPanel gameScreen = new JPanel(new BorderLayout());

    private final List<LevelCatalog.Entry> catalog = LevelCatalog.load();
    private final ProgressStore store;
    private PlayerProfile profile;

    private NamePanel namePanel;
    private LevelSelectPanel levelSelectPanel;
    private CharacterSelectPanel selectPanel;

    private LevelCatalog.Entry currentLevel;
    private Critter critter = Critter.defaultChoice();
    private GameState state;
    private GamePanel gamePanel;
    private HudBar hud;

    /** Which screen is showing, since every key binding needs to know. */
    private String screen = NAME_CARD;

    public GameFrame(String startingLevel) {
        this(startingLevel, ProgressStore.inUserHome());
    }

    /**
     * @param store where progress is kept; a test passes a temporary one so it
     *              does not depend on whether the developer has played
     */
    public GameFrame(String startingLevel, ProgressStore store) {
        super("Signal Lost");
        this.store = store;
        this.profile = store.load();
        this.currentLevel = findLevel(startingLevel);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        root.setBackground(Palette.BACKDROP);
        gameScreen.setBackground(Palette.BACKDROP);
        setContentPane(root);

        if (profile == null) {
            showNameEntry();
        } else {
            showLevelSelect();
        }
        bindKeys();

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private LevelCatalog.Entry findLevel(String name) {
        return catalog.stream()
                .filter(entry -> entry.name().equals(name))
                .findFirst()
                .orElse(catalog.get(0));
    }

    // --------------------------------------------------------------- screens

    private void showNameEntry() {
        screen = NAME_CARD;
        namePanel = new NamePanel(profile == null ? null : profile.name(), chosenName -> {
            if (profile == null) {
                profile = new PlayerProfile(chosenName);
            } else {
                profile.rename(chosenName);
            }
            store.save(profile);
            showLevelSelect();
        });
        swapIn(namePanel, NAME_CARD);
    }

    private void showLevelSelect() {
        screen = LEVELS_CARD;
        levelSelectPanel = new LevelSelectPanel(catalog, profile, entry -> {
            currentLevel = entry;
            showCharacterSelect();
        });
        swapIn(levelSelectPanel, LEVELS_CARD);
    }

    private void showCharacterSelect() {
        screen = SELECT_CARD;
        selectPanel = new CharacterSelectPanel(critter, this::startRun);
        swapIn(selectPanel, SELECT_CARD);
    }

    /** Builds a fresh run of the chosen level and shows it. */
    private void startRun(Critter chosen) {
        critter = chosen;
        screen = GAME_CARD;
        loadLevel();

        root.add(gameScreen, GAME_CARD);
        cards.show(root, GAME_CARD);
        pack();
    }

    private void loadLevel() {
        Level level = LevelLoader.loadFromResource(currentLevel.name());
        state = new GameState(level.map(), level.guards(), SIGHT_RADIUS, level.turnLimit());
        gamePanel = new GamePanel(state, critter);
        hud = new HudBar(state, critter, currentLevel.name());

        gameScreen.removeAll();
        gameScreen.add(hud, BorderLayout.NORTH);
        gameScreen.add(gamePanel, BorderLayout.CENTER);
        gameScreen.add(new FooterBar(), BorderLayout.SOUTH);
        gameScreen.revalidate();
        gameScreen.repaint();
    }

    private void swapIn(JPanel panel, String card) {
        root.add(panel, card);
        cards.show(root, card);
        pack();
    }

    /**
     * Files the finished run before leaving the level.
     *
     * <p>Saving on the way out rather than on every turn means one write per
     * run, and nothing is lost that the player would notice.
     */
    private void recordAndReturnToLevels() {
        if (state != null && state.hasEscaped() && profile != null) {
            if (profile.record(currentLevel.name(), state.score())) {
                store.save(profile);
            }
        }
        showLevelSelect();
    }

    // ----------------------------------------------------------------- input

    private void bindKeys() {
        bindDirection("UP", "W", Direction.UP, 0, -1);
        bindDirection("DOWN", "S", Direction.DOWN, 0, 1);
        bindDirection("LEFT", "A", Direction.LEFT, -1, 0);
        bindDirection("RIGHT", "D", Direction.RIGHT, 1, 0);

        bind("SPACE", "space", this::onSpace);
        bind("ENTER", "confirm", this::onEnter);
        bind("BACK_SPACE", "backspace", () -> {
            if (screen.equals(NAME_CARD)) {
                namePanel.backspace();
            }
        });
        bind("ESCAPE", "back", () -> {
            if (screen.equals(SELECT_CARD)) {
                showLevelSelect();
            } else if (screen.equals(GAME_CARD)) {
                recordAndReturnToLevels();
            }
        });

        bind("PERIOD", "wait", () -> {
            if (inGame() && state.movePlayer(Direction.WAIT)) {
                refresh();
            }
        });
        bind("U", "undo", () -> {
            if (inGame() && state.undoLastMove()) {
                refresh();
            }
        });
        bind("H", "toggleHint", () -> {
            if (inGame()) {
                gamePanel.toggleHint();
                hud.repaint();
            }
        });
        bind("M", "toggleReveal", () -> {
            if (inGame()) {
                state.fog().toggleRevealAll();
                refresh();
            }
        });
        bind("R", "restart", () -> {
            if (inGame()) {
                recordAndReturnToLevels();
            }
        });
        bind("C", "changeCharacter", () -> {
            if (inGame()) {
                recordAndReturnToLevels();
            }
        });
        bind("N", "changeName", () -> {
            if (screen.equals(LEVELS_CARD)) {
                showNameEntry();
            }
        });

        bindTypedLetters();
        bindNumberKeys();
    }

    private boolean inGame() {
        return screen.equals(GAME_CARD);
    }

    private void onSpace() {
        switch (screen) {
            case NAME_CARD -> namePanel.type(' ');
            case LEVELS_CARD -> levelSelectPanel.confirmSelection();
            case SELECT_CARD -> selectPanel.confirmSelection();
            default -> {
                if (state.isAttemptOver()) {
                    // Space is how a life gets spent, so a capture is always a
                    // deliberate acknowledgement rather than something that
                    // scrolls past before the player has read it.
                    state.beginNextLife();
                    refresh();
                } else if (state.movePlayer(Direction.WAIT)) {
                    refresh();
                }
            }
        }
    }

    private void onEnter() {
        switch (screen) {
            case NAME_CARD -> namePanel.confirm();
            case LEVELS_CARD -> levelSelectPanel.confirmSelection();
            case SELECT_CARD -> selectPanel.confirmSelection();
            default -> {
                if (state.hasEscaped() || state.isRunOver()) {
                    recordAndReturnToLevels();
                }
            }
        }
    }

    /**
     * Binds one arrow, plus its letter twin, to walking during play and to
     * moving the highlight on the two select screens.
     */
    private void bindDirection(String arrowKey, String letterKey, Direction direction,
                               int columnStep, int rowStep) {
        Runnable action = () -> {
            switch (screen) {
                case LEVELS_CARD -> levelSelectPanel.moveSelection(columnStep, rowStep);
                case SELECT_CARD -> selectPanel.moveSelection(columnStep, rowStep);
                case NAME_CARD -> {
                    // Nothing to steer while typing a name.
                }
                default -> {
                    if (state.movePlayer(direction)) {
                        refresh();
                    }
                }
            }
        };
        bind(arrowKey, "go" + direction.name(), action);
        // The letter twins have to give way to typing on the name screen.
        bind(letterKey, "go" + direction.name() + "Alt", () -> {
            if (!screen.equals(NAME_CARD)) {
                action.run();
            }
        });
    }

    /** Letters and digits reach the name screen; elsewhere they do nothing. */
    private void bindTypedLetters() {
        for (char letter = 'A'; letter <= 'Z'; letter++) {
            char typed = letter;
            bind(String.valueOf(letter), "type" + letter, () -> {
                if (screen.equals(NAME_CARD)) {
                    namePanel.type(typed);
                }
            });
        }
    }

    private void bindNumberKeys() {
        for (int i = 1; i <= 9; i++) {
            int index = i - 1;
            char digit = (char) ('0' + i);
            bind(String.valueOf(i), "pick" + i, () -> {
                switch (screen) {
                    case NAME_CARD -> namePanel.type(digit);
                    case LEVELS_CARD -> levelSelectPanel.selectIndex(index);
                    case SELECT_CARD -> selectPanel.selectIndex(index);
                    default -> {
                        // Digits do nothing during play.
                    }
                }
            });
        }
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
