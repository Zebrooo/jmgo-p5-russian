package org.jmgo.input.web;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class KeyboardModel {
    private static final int ACTION_KEY_COUNT = 6;
    private static final List<List<String>> RUSSIAN = rowsOf(
            new String[]{"й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х", "ъ"},
            new String[]{"ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"},
            new String[]{"я", "ч", "с", "м", "и", "т", "ь", "б", "ю"}
    );
    private static final List<List<String>> ENGLISH = rowsOf(
            new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
            new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l"},
            new String[]{"z", "x", "c", "v", "b", "n", "m"}
    );
    private static final List<List<String>> NUMBERS = rowsOf(
            new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
            new String[]{"-", "/", ":", ";", "(", ")", "₽", "&", "@"},
            new String[]{".", ",", "?", "!", "'", "\"", "+", "="}
    );

    public enum Language { RUSSIAN, ENGLISH, NUMBERS }
    public enum Direction { LEFT, RIGHT, UP, DOWN }
    public enum KeyType { TEXT, LANGUAGE, NUMBERS, SPACE, BACKSPACE, HIDE, SUBMIT }

    public static final class Selection {
        private final int row;
        private final int column;

        public Selection(int row, int column) {
            this.row = row;
            this.column = column;
        }

        public int row() { return row; }
        public int column() { return column; }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Selection)) return false;
            Selection that = (Selection) other;
            return row == that.row && column == that.column;
        }

        @Override
        public int hashCode() { return Objects.hash(row, column); }

        @Override
        public String toString() { return "Selection(" + row + "," + column + ")"; }
    }

    public static final class Key {
        private final KeyType type;
        private final String text;

        private Key(KeyType type, String text) {
            this.type = type;
            this.text = text;
        }

        public KeyType type() { return type; }
        public String text() { return text; }
    }

    private Language language = Language.RUSSIAN;
    private Selection selection = new Selection(0, 0);

    public static List<List<String>> rows(Language language) {
        switch (language) {
            case ENGLISH: return ENGLISH;
            case NUMBERS: return NUMBERS;
            case RUSSIAN:
            default: return RUSSIAN;
        }
    }

    public Language language() { return language; }
    public Selection selection() { return selection; }

    public void setSelection(Selection next) {
        selection = clamp(next);
    }

    public void move(Direction direction) {
        int row = selection.row();
        int column = selection.column();
        int oldCount = keyCount(row);
        switch (direction) {
            case LEFT:
                selection = new Selection(row, Math.max(0, column - 1));
                break;
            case RIGHT:
                selection = new Selection(row, Math.min(oldCount - 1, column + 1));
                break;
            case UP:
                selection = moveVertical(-1);
                break;
            case DOWN:
                selection = moveVertical(1);
                break;
        }
    }

    public Key selectedKey() {
        int row = selection.row();
        int column = selection.column();
        List<List<String>> rows = rows(language);
        if (row < rows.size()) return new Key(KeyType.TEXT, rows.get(row).get(column));
        switch (column) {
            case 0: return new Key(KeyType.LANGUAGE, language == Language.RUSSIAN ? "EN" : "РУ");
            case 1: return new Key(KeyType.NUMBERS, "123");
            case 2: return new Key(KeyType.SPACE, "Пробел");
            case 3: return new Key(KeyType.BACKSPACE, "⌫");
            case 4: return new Key(KeyType.HIDE, "▼ Скрыть");
            default: return new Key(KeyType.SUBMIT, "Поиск");
        }
    }

    public void activateModeKey() {
        KeyType type = selectedKey().type();
        if (type == KeyType.LANGUAGE) {
            language = language == Language.RUSSIAN ? Language.ENGLISH : Language.RUSSIAN;
        } else if (type == KeyType.NUMBERS) {
            language = Language.NUMBERS;
        }
        selection = clamp(selection);
    }

    private Selection moveVertical(int delta) {
        int oldRow = selection.row();
        int newRow = Math.max(0, Math.min(rows(language).size(), oldRow + delta));
        if (newRow == oldRow) return selection;
        int oldCount = keyCount(oldRow);
        int newCount = keyCount(newRow);
        double position = oldCount <= 1 ? 0.0 : selection.column() / (double) (oldCount - 1);
        return new Selection(newRow, (int) Math.round(position * (newCount - 1)));
    }

    private Selection clamp(Selection value) {
        int row = Math.max(0, Math.min(rows(language).size(), value.row()));
        int column = Math.max(0, Math.min(keyCount(row) - 1, value.column()));
        return new Selection(row, column);
    }

    private int keyCount(int row) {
        List<List<String>> rows = rows(language);
        return row < rows.size() ? rows.get(row).size() : ACTION_KEY_COUNT;
    }

    private static List<List<String>> rowsOf(String[] first, String[] second, String[] third) {
        return Collections.unmodifiableList(Arrays.asList(
                Collections.unmodifiableList(Arrays.asList(first)),
                Collections.unmodifiableList(Arrays.asList(second)),
                Collections.unmodifiableList(Arrays.asList(third))
        ));
    }
}
