package com.signallost.android;

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

    NameView(Context context, String current, Consumer<String> onChosen) {
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
                // Nobody should be stopped at the door by a validation error.
                chosen = "Player";
            }
            hideKeyboard(field);
            onChosen.accept(chosen.length() > 40 ? chosen.substring(0, 40) : chosen);
        };

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
