package com.signallost.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

/**
 * The only activity.
 *
 * <p>There is no layout XML and no AndroidX: the game is one custom view that
 * draws itself, which is how the desktop version works too. What is worth
 * noticing is how little is here. Everything that decides anything — what you
 * can see, where a guard goes, whether you were spotted, what a run scored — is
 * in classes shared with the desktop build, unchanged.
 */
public class GameActivity extends Activity {

    private BoardView board;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        board = new BoardView(this, "level1");
        setContentView(board);
    }

    @Override
    public void onBackPressed() {
        if (board != null && board.state().canUndo()) {
            board.state().undoLastMove();
            board.invalidate();
            return;
        }
        super.onBackPressed();
    }
}
