package com.signallost;

import com.signallost.ui.GameFrame;

import javax.swing.SwingUtilities;

/** Entry point. Builds the window on the Swing event dispatch thread. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        String level = args.length > 0 ? args[0] : "level1";
        SwingUtilities.invokeLater(() -> new GameFrame(level).setVisible(true));
    }
}
