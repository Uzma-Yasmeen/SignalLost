package com.signallost.web;

import com.signallost.level.LevelLoader;
import com.signallost.model.Direction;
import com.signallost.model.GameState;

import org.teavm.jso.browser.Window;

import java.util.List;

/**
 * A first check that the game's rules survive being compiled to JavaScript.
 *
 * <p>Before building a front end on top of TeaVM it is worth knowing that the
 * parts underneath it work: the map parser, the fog, the guards and the turn
 * system all have to run in a browser with no filesystem and no threads.
 *
 * <p>The level is written out here rather than loaded. {@code LevelLoader} can
 * read from the classpath, which a browser does not have — but it also parses a
 * list of lines, and that is the door the web version goes through.
 */
public final class WebGame {

    private static final List<String> TINY_LEVEL = List.of(
            "#########",
            "#@......#",
            "#.#####.#",
            "#......K#",
            "#.#####.#",
            "#E......#",
            "#########",
            "---",
            "turns 60",
            "patroller 3,2 3,6");

    private WebGame() {
    }

    public static void main(String[] args) {
        var level = LevelLoader.parseLevel("probe", TINY_LEVEL);
        GameState state = new GameState(level.map(), level.guards(),
                GameState.DEFAULT_SIGHT_RADIUS, level.turnLimit());

        StringBuilder report = new StringBuilder();
        report.append("level parsed: ").append(level.map().cols())
                .append("x").append(level.map().rows()).append("\n");
        report.append("player starts at ").append(state.player()).append("\n");
        report.append("guards: ").append(state.guards().size()).append("\n");
        report.append("tiles the player can see: ")
                .append(state.fog().visibleTiles().size()).append("\n");

        state.movePlayer(Direction.RIGHT);
        state.movePlayer(Direction.RIGHT);
        report.append("after two moves, player at ").append(state.player()).append("\n");
        report.append("guard moved to ").append(state.guards().get(0).position()).append("\n");
        state.undoLastMove();
        report.append("after undo, player at ").append(state.player()).append("\n");
        report.append("a hint suggests ").append(state.hintRoute(1)).append("\n");

        Window.current().getDocument().getBody().setInnerHTML(
                "<pre style='color:#e8ecf6;background:#0a0c12;padding:24px;font:14px monospace'>"
                        + report + "</pre>");
    }
}
