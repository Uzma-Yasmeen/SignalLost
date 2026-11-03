package com.signallost.android;

import com.signallost.art.Critter;
import com.signallost.level.LevelCatalog;
import com.signallost.model.Score;
import com.signallost.progress.PlayerProfile;
import com.signallost.progress.ProgressStore;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.List;

/**
 * The only activity: it swaps one screen for another and keeps the save file.
 *
 * <p>The flow is the desktop game's — title, name, character, level select, the
 * level — and each screen is a view that draws itself, so there is no layout
 * XML anywhere in this project.
 *
 * <p>Everything that decides anything lives in classes shared with the desktop
 * build, {@link ProgressStore} and the characters included. The one platform
 * difference is the line below that hands the store a directory.
 */
public class GameActivity extends Activity {

    private FrameLayout root;
    private ProgressStore store;
    private PlayerProfile profile;
    private Critter critter = Critter.defaultChoice();
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
        // phone is the first launch of every install. This is the empty case,
        // not an error.
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
        show(new TitleView(this, greeting, this::showNameEntry, this::finish));
    }

    private void showNameEntry() {
        show(new NameView(this, profile == null ? null : profile.name(), chosen -> {
            profile = store.loadFor(chosen);
            showCharacterSelect();
        }, this::showTitle));
    }

    private void showCharacterSelect() {
        board = null;
        show(new CharacterSelectView(this, critter, picked -> {
            critter = picked;
            showLevelSelect();
        }, this::showNameEntry));
    }

    private void showLevelSelect() {
        board = null;
        show(new LevelSelectView(this, profile, this::startLevel,
                this::showCharacterSelect));
    }

    private void startLevel(String levelName) {
        board = new BoardView(this, levelName, critter, this::recordResult,
                this::showLevelSelect, () -> showNextAfter(levelName));
        show(board);
    }

    /**
     * Straight on to the next level, if the stars just earned have unlocked it.
     *
     * <p>Sending a player back to the grid after every win is a tap they never
     * wanted; sending them to a level they have not unlocked is worse. If the
     * next one is not open yet, the grid is the honest answer.
     */
    private void showNextAfter(String levelName) {
        List<LevelCatalog.Entry> levels = LevelCatalog.load();
        for (int i = 0; i < levels.size() - 1; i++) {
            if (levels.get(i).name().equals(levelName)) {
                LevelCatalog.Entry next = levels.get(i + 1);
                if (profile.totalStars() >= next.starsToUnlock()) {
                    startLevel(next.name());
                    return;
                }
                break;
            }
        }
        showLevelSelect();
    }

    /** Keeps the better of what was just done and what was already there. */
    private void recordResult(String levelName, Score score) {
        if (profile.record(levelName, score)) {
            store.save(profile);
        }
    }

    /**
     * The hardware back button follows the same path the on-screen one does.
     *
     * <p>In a level it undoes, because that is what a player pressing back in a
     * turn-based game means. With nothing left to undo it steps out to the
     * levels, and from there back through the character and name screens to the
     * title, where it finally leaves the game.
     */
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
        View current = root.getChildCount() > 0 ? root.getChildAt(0) : null;
        if (current instanceof LevelSelectView) {
            showCharacterSelect();
        } else if (current instanceof CharacterSelectView) {
            showNameEntry();
        } else if (current instanceof NameView) {
            showTitle();
        } else {
            super.onBackPressed();
        }
    }
}
