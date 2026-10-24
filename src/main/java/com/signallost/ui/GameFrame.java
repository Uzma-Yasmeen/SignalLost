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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The game window, and the route between its screens:
 *
 * <pre>
 *   home  -&gt;  name  -&gt;  character  -&gt;  level select  -&gt;  the level
 *                                           ^                 |
 *                                           +---- Esc --------+
 *
 * <p>Esc steps back one screen at a time, so there is always a way out of
 * wherever the player is.
 * </pre>
 *
 * <p>Input goes through Swing's {@code InputMap}/{@code ActionMap} rather than a
 * {@code KeyListener}, so keys work whichever component holds focus.
 */
public final class GameFrame extends JFrame {

    private static final int SIGHT_RADIUS = 5;

    private static final String HOME_CARD = "home";
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

    private TitlePanel titlePanel;
    private NamePanel namePanel;
    private LevelSelectPanel levelSelectPanel;
    private CharacterSelectPanel selectPanel;

    private LevelCatalog.Entry currentLevel;
    private Critter critter = Critter.defaultChoice();
    private GameState state;
    private GamePanel gamePanel;
    private HudBar hud;

    /** Which screen is showing, since every key binding needs to know. */
    private String screen = HOME_CARD;

    /** Letters already given a job, so the catch-all pass skips them. */
    private final Set<Character> boundLetters = new HashSet<>();

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

        showHome();
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

    private void showHome() {
        screen = HOME_CARD;
        titlePanel = new TitlePanel(profile, catalog.size());
        swapIn(titlePanel, HOME_CARD);
    }

    private void showNameEntry() {
        screen = NAME_CARD;
        namePanel = new NamePanel(profile == null ? null : profile.name(), chosenName -> {
            if (profile == null) {
                profile = new PlayerProfile(chosenName);
            } else {
                profile.rename(chosenName);
            }
            store.save(profile);
            showCharacterSelect();
        });
        swapIn(namePanel, NAME_CARD);
    }

    private void showCharacterSelect() {
        screen = SELECT_CARD;
        selectPanel = new CharacterSelectPanel(critter, chosen -> {
            critter = chosen;
            showLevelSelect();
        });
        swapIn(selectPanel, SELECT_CARD);
    }

    private void showLevelSelect() {
        screen = LEVELS_CARD;
        levelSelectPanel = new LevelSelectPanel(catalog, profile, entry -> {
            currentLevel = entry;
            startRun();
        });
        swapIn(levelSelectPanel, LEVELS_CARD);
    }

    /** Builds a fresh run of the chosen level and shows it. */
    private void startRun() {
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
     * <p>Saving on the way out rather than every turn means one write per run,
     * and nothing is lost that the player would notice.
     */
    private void recordAndReturnToLevels() {
        if (state != null && state.hasEscaped() && profile != null
                && profile.record(currentLevel.name(), state.score())) {
            store.save(profile);
        }
        showLevelSelect();
    }

    // ----------------------------------------------------------------- input

    /*
     * Every key is bound exactly once.
     *
     * An earlier version bound U, R, H, M, C and WASD to game actions and then
     * looped over A-Z binding each letter to "type this into the name box".
     * The second pass silently replaced the first in the InputMap, so undo and
     * restart did nothing at all. One binding per key, deciding by screen
     * inside the action, is the only arrangement that cannot drift like that,
     * and bindKey now refuses a second binding rather than overwriting one.
     */

    private void bindKeys() {
        bindKey("UP", "up", () -> steer(0, -1, Direction.UP));
        bindKey("DOWN", "down", () -> steer(0, 1, Direction.DOWN));
        bindKey("LEFT", "left", () -> steer(-1, 0, Direction.LEFT));
        bindKey("RIGHT", "right", () -> steer(1, 0, Direction.RIGHT));

        bindKey("SPACE", "space", this::onSpace);
        bindKey("ENTER", "confirm", this::onEnter);
        bindKey("BACK_SPACE", "backspace", () -> {
            if (onNameScreen()) {
                namePanel.backspace();
            }
        });
        bindKey("ESCAPE", "back", this::onEscape);
        bindKey("PERIOD", "wait", () -> {
            if (inGame() && state.movePlayer(Direction.WAIT)) {
                refresh();
            }
        });

        bindLetter('W', () -> steer(0, -1, Direction.UP));
        bindLetter('A', () -> steer(-1, 0, Direction.LEFT));
        bindLetter('S', () -> steer(0, 1, Direction.DOWN));
        bindLetter('D', () -> steer(1, 0, Direction.RIGHT));

        bindLetter('U', () -> {
            if (inGame() && state.undoLastMove()) {
                refresh();
            }
        });
        bindLetter('H', () -> {
            if (inGame()) {
                gamePanel.toggleHint();
                hud.repaint();
            }
        });
        bindLetter('M', () -> {
            if (inGame()) {
                state.fog().toggleRevealAll();
                refresh();
            }
        });
        bindLetter('R', () -> {
            if (inGame()) {
                loadLevel();
                pack();
                refresh();
            }
        });
        bindLetter('C', () -> {
            if (inGame()) {
                showCharacterSelect();
            }
        });
        bindLetter('N', () -> {
            if (onLevelSelect()) {
                showNameEntry();
            }
        });
        bindLetter('P', () -> {
            if (onLevelSelect()) {
                showCharacterSelect();
            }
        });

        // The rest of the alphabet exists only so a name can be typed with it.
        for (char letter = 'A'; letter <= 'Z'; letter++) {
            if (!boundLetters.contains(letter)) {
                bindLetter(letter, () -> {
                    // Nothing to do outside the name screen.
                });
            }
        }
        bindNumberKeys();
    }

    /** Letters type on the name screen and do their own job everywhere else. */
    private void bindLetter(char letter, Runnable whenNotTyping) {
        boundLetters.add(letter);
        bindKey(String.valueOf(letter), "key" + letter, () -> {
            if (onNameScreen()) {
                namePanel.type(letter);
            } else {
                whenNotTyping.run();
            }
        });
    }

    private void bindNumberKeys() {
        for (int i = 1; i <= 9; i++) {
            int index = i - 1;
            char digit = (char) ('0' + i);
            bindKey(String.valueOf(i), "pick" + i, () -> {
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

    /** One direction, meaning whatever the screen showing needs it to mean. */
    private void steer(int columnStep, int rowStep, Direction direction) {
        switch (screen) {
            case LEVELS_CARD -> levelSelectPanel.moveSelection(columnStep, rowStep);
            case SELECT_CARD -> selectPanel.moveSelection(columnStep, rowStep);
            case NAME_CARD, HOME_CARD -> {
                // Nothing to steer while typing a name or waiting on the title.
            }
            default -> {
                if (state.movePlayer(direction)) {
                    refresh();
                }
            }
        }
    }

    private boolean inGame() {
        return screen.equals(GAME_CARD);
    }

    private boolean onNameScreen() {
        return screen.equals(NAME_CARD);
    }

    private boolean onLevelSelect() {
        return screen.equals(LEVELS_CARD);
    }

    private void onSpace() {
        switch (screen) {
            case HOME_CARD -> showNameEntry();
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
            case HOME_CARD -> showNameEntry();
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

    /** Esc steps back one screen, so nowhere is a dead end. */
    private void onEscape() {
        switch (screen) {
            case NAME_CARD -> showHome();
            case SELECT_CARD -> showNameEntry();
            case LEVELS_CARD -> showCharacterSelect();
            case GAME_CARD -> recordAndReturnToLevels();
            default -> {
                // The home screen is the back of the queue.
            }
        }
    }

    /**
     * Binds one key, refusing to replace an existing binding.
     *
     * <p>The refusal is the point. Silently overwriting is what broke undo and
     * restart, and it broke them invisibly: the game ran, the keys did nothing,
     * and only playing it found out.
     */
    private void bindKey(String keyStroke, String actionName, Runnable action) {
        InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = root.getActionMap();
        KeyStroke stroke = KeyStroke.getKeyStroke(keyStroke);

        Object existing = inputMap.get(stroke);
        if (existing != null) {
            throw new IllegalStateException(
                    "Key " + keyStroke + " is already bound to " + existing
                            + "; binding it again to " + actionName
                            + " would silently replace it.");
        }
        inputMap.put(stroke, actionName);
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

    /** The running level, so a test can press a key and check what it did. */
    GameState gameState() {
        return state;
    }
}
