package com.signallost.android;

import com.signallost.model.Score;
import com.signallost.progress.PlayerProfile;
import com.signallost.progress.ProgressStore;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * The only activity: it swaps one screen for another and keeps the save file.
 *
 * <p>The flow is the desktop game's, minus the character roster: title, name,
 * level select, the level. Each screen is a view that draws itself, so there is
 * no layout XML anywhere in this project.
 *
 * <p>Everything that decides anything lives in classes shared with the desktop
 * build, {@link ProgressStore} included. The one platform difference is the
 * line below that hands it a directory, which is the single concession the
 * rules layer makes to running on a phone.
 */
public class GameActivity extends Activity {

    private FrameLayout root;
    private ProgressStore store;
    private PlayerProfile profile;
    private BoardView board;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // The desktop uses the user's home directory; Android gives each app a
        // private one. ProgressStore was written to be told rather than to
        // assume, which is the whole of the porting work on this side.
        store = new ProgressStore(getFilesDir().toPath().resolve("progress.properties"));

        // load() returns null when nobody has played here before, which on a
        // phone is the first launch of every install. Worth stating plainly
        // because it is not obvious from the call: this is the empty case, not
        // an error.
        profile = store.load();

        root = new FrameLayout(this);
        setContentView(root);
        showTitle();
    }

    private void show(View screen) {
        root.removeAllViews();
        root.addView(screen, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        screen.requestFocus();
    }

    private void showTitle() {
        board = null;
        String greeting = (profile != null && profile.totalStars() > 0)
                ? "welcome back, " + profile.name() + "  ·  " + profile.totalStars() + " stars"
                : null;
        show(new TitleView(this, greeting, this::showNameEntry));
    }

    private void showNameEntry() {
        show(new NameView(this, profile == null ? null : profile.name(), chosen -> {
            profile = store.loadFor(chosen);
            showLevelSelect();
        }));
    }

    private void showLevelSelect() {
        board = null;
        show(new LevelSelectView(this, profile, this::startLevel));
    }

    private void startLevel(String levelName) {
        board = new BoardView(this, levelName, this::recordResult, this::showLevelSelect);
        show(board);
    }

    /** Keeps the better of what was just done and what was already there. */
    private void recordResult(String levelName, Score score) {
        if (profile.record(levelName, score)) {
            store.save(profile);
        }
    }

    @Override
    public void onBackPressed() {
        if (board != null) {
            if (board.state().canUndo()) {
                board.state().undoLastMove();
                board.invalidate();
            } else {
                showLevelSelect();
            }
            return;
        }
        super.onBackPressed();
    }
}
