package org.jmgo.input.web;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.List;

final class TvKeyboardView extends LinearLayout {
    interface Listener {
        void onText(String text);
        void onBackspace();
        void onHide();
        void onSubmit();
    }

    private static final int NORMAL_COLOR = Color.rgb(105, 78, 171);
    private static final int SELECTED_COLOR = Color.rgb(194, 38, 34);
    private final KeyboardModel model = new KeyboardModel();
    private Listener listener;

    TvKeyboardView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(8), dp(8), dp(8), dp(8));
        setBackgroundColor(Color.rgb(250, 248, 252));
        render();
    }

    void setListener(Listener listener) { this.listener = listener; }

    boolean handleKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT: model.move(KeyboardModel.Direction.LEFT); render(); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: model.move(KeyboardModel.Direction.RIGHT); render(); return true;
            case KeyEvent.KEYCODE_DPAD_UP: model.move(KeyboardModel.Direction.UP); render(); return true;
            case KeyEvent.KEYCODE_DPAD_DOWN: model.move(KeyboardModel.Direction.DOWN); render(); return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER: activateSelected(); return true;
            default: return false;
        }
    }

    private void render() {
        removeAllViews();
        List<List<String>> rows = KeyboardModel.rows(model.language());
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            LinearLayout row = row();
            List<String> labels = rows.get(rowIndex);
            for (int column = 0; column < labels.size(); column++) {
                final int selectedRow = rowIndex;
                final int selectedColumn = column;
                Button button = button(labels.get(column), 1.0f, selectedRow, selectedColumn);
                button.setOnClickListener(view -> {
                    model.setSelection(new KeyboardModel.Selection(selectedRow, selectedColumn));
                    activateSelected();
                });
                row.addView(button);
            }
            addView(row);
        }

        LinearLayout actions = row();
        addAction(actions, model.language() == KeyboardModel.Language.RUSSIAN ? "EN" : "РУ", 1.2f, 0);
        addAction(actions, "123", 1.2f, 1);
        addAction(actions, "Пробел", 5.0f, 2);
        addAction(actions, "⌫", 1.5f, 3);
        addAction(actions, "▼ Скрыть", 1.8f, 4);
        addAction(actions, "Поиск", 1.8f, 5);
        addView(actions);
    }

    private void addAction(LinearLayout row, String label, float weight, int column) {
        Button button = button(label, weight, 3, column);
        button.setOnClickListener(view -> {
            model.setSelection(new KeyboardModel.Selection(3, column));
            activateSelected();
        });
        row.addView(button);
    }

    private void activateSelected() {
        KeyboardModel.Key key = model.selectedKey();
        if (key.type() == KeyboardModel.KeyType.LANGUAGE || key.type() == KeyboardModel.KeyType.NUMBERS) {
            model.activateModeKey();
            render();
            return;
        }
        if (listener == null) return;
        switch (key.type()) {
            case TEXT: listener.onText(key.text()); break;
            case SPACE: listener.onText(" "); break;
            case BACKSPACE: listener.onBackspace(); break;
            case HIDE: listener.onHide(); break;
            case SUBMIT: listener.onSubmit(); break;
            default: break;
        }
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dp(2), 0, dp(2));
        row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f));
        return row;
    }

    private Button button(String label, float weight, int row, int column) {
        Button button = new Button(getContext());
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(17.0f);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        LayoutParams params = new LayoutParams(0, LayoutParams.MATCH_PARENT, weight);
        int margin = dp(2);
        params.setMargins(margin, 0, margin, 0);
        button.setLayoutParams(params);
        GradientDrawable background = new GradientDrawable();
        background.setColor(model.selection().equals(new KeyboardModel.Selection(row, column))
                ? SELECTED_COLOR : NORMAL_COLOR);
        background.setCornerRadius(dp(28));
        button.setBackground(background);
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
