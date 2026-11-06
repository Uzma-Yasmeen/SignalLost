package com.signallost.android;

import com.signallost.progress.PlayerProfile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.function.Consumer;

/**
 * Who is playing.
 *
 * <p>Progress is stored against this name, so several people can play on one
 * phone without overwriting each other — the same rule the desktop version
 * follows, using the same {@code ProgressStore}.
 *
 * <p>This is the one screen built from ordinary Android widgets rather than
 * drawn onto a canvas. Reimplementing a text field and a keyboard to keep the
 * styling consistent would be a lot of work to end up somewhere worse.
 */
@SuppressLint("ViewConstructor")
class NameView extends LinearLayout {

    NameView(Context context, String current, List<PlayerProfile> returning,
             Consumer<String> onChosen, Runnable onBack) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setBackgroundColor(Palette.BACKDROP);
        int pad = (int) (getResources().getDisplayMetrics().density * 24);
        setPadding(pad * 2, pad, pad * 2, pad);

        TextView heading = new TextView(context);
        heading.setText("WHO IS PLAYING?");
        heading.setTextColor(Palette.TEXT);
        heading.setTextSize(26);
        heading.setGravity(Gravity.CENTER);
        heading.setPadding(0, 0, 0, pad / 2);
        addView(heading);

        TextView note = new TextView(context);
        note.setText("Your stars are saved under this name");
        note.setTextColor(Palette.TEXT_DIM);
        note.setTextSize(14);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, 0, 0, pad);
        addView(note);

        EditText field = new EditText(context);
        field.setHint("name");
        field.setText(current == null ? "" : current);
        field.setSelection(field.getText().length());
        field.setTextColor(Palette.TEXT);
        field.setHintTextColor(Palette.TEXT_DIM);
        field.setTextSize(22);
        field.setGravity(Gravity.CENTER);
        field.setSingleLine(true);
        field.setImeOptions(EditorInfo.IME_ACTION_DONE);
        field.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        field.setBackground(rounded(Palette.PANEL));
        field.setPadding(pad, pad / 2, pad, pad / 2);
        addView(field, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        Button go = new Button(context);
        go.setText("CONTINUE");
        go.setTextColor(Palette.TEXT);
        go.setAllCaps(true);
        go.setBackground(rounded(Palette.BUTTON));
        LayoutParams buttonSize = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        buttonSize.topMargin = pad;
        addView(go, buttonSize);

        Runnable submit = () -> {
            String chosen = field.getText().toString().trim();
            if (chosen.isEmpty()) {
                // The name is the save key, not a greeting. Falling back to
                // "Player" meant everyone who skipped the box shared one set of
                // stars and watched their progress change under them. So the
                // door stays shut, and the box says why.
                field.setError("your stars are saved under this name");
                field.requestFocus();
                return;
            }
            hideKeyboard(field);
            onChosen.accept(chosen.length() > 40 ? chosen.substring(0, 40) : chosen);
        };

        Button back = new Button(context);
        back.setText("BACK");
        back.setTextColor(Palette.TEXT_DIM);
        back.setAllCaps(true);
        back.setBackground(rounded(Palette.PANEL));
        LayoutParams backSize = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        backSize.topMargin = pad / 2;
        addView(back, backSize);
        back.setOnClickListener(v -> {
            hideKeyboard(field);
            onBack.run();
        });

        // Everyone who has played on this phone, to pick rather than retype.
        // Spelling your own name differently by one letter used to open a new
        // empty profile instead of saying the name was unknown, so a player who
        // had reached level 6 came back the next day to level 1 with their
        // stars still on the device under a spelling they could not reproduce.
        if (!returning.isEmpty()) {
            TextView orPick = new TextView(context);
            orPick.setText("or carry on as");
            orPick.setTextColor(Palette.TEXT_DIM);
            orPick.setTextSize(13);
            orPick.setGravity(Gravity.CENTER);
            LayoutParams label = new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            label.topMargin = pad;
            addView(orPick, label);

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            for (PlayerProfile profile : returning.subList(0, Math.min(returning.size(), 3))) {
                Button pick = new Button(context);
                pick.setText(profile.name().toUpperCase()
                        + System.lineSeparator() + profile.totalStars() + " stars");
                pick.setTextColor(Palette.TEXT);
                pick.setAllCaps(false);
                pick.setTextSize(13);
                pick.setBackground(rounded(Palette.BUTTON));
                LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(
                        0, LayoutParams.WRAP_CONTENT, 1f);
                size.leftMargin = pad / 4;
                size.rightMargin = pad / 4;
                row.addView(pick, size);
                pick.setOnClickListener(v -> {
                    hideKeyboard(field);
                    onChosen.accept(profile.name());
                });
            }
            LayoutParams rowSize = new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            rowSize.topMargin = pad / 3;
            addView(row, rowSize);
        }

        go.setOnClickListener(v -> submit.run());
        field.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit.run();
                return true;
            }
            return false;
        });
    }

    private GradientDrawable rounded(int colour) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(colour);
        shape.setCornerRadius(getResources().getDisplayMetrics().density * 14);
        shape.setStroke((int) getResources().getDisplayMetrics().density,
                Color.argb(90, 120, 140, 190));
        return shape;
    }

    private void hideKeyboard(View from) {
        InputMethodManager keyboard =
                (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) {
            keyboard.hideSoftInputFromWindow(from.getWindowToken(), 0);
        }
    }
}
