package org.jmgo.input.web;

public final class CursorState {
    public static final int EDGE_PADDING = 18;
    private static final int BASE_STEP = 36;
    private int width;
    private int height;
    private int x;
    private int y;
    private boolean enabled;

    public enum Direction { LEFT, RIGHT, UP, DOWN }

    public CursorState(int width, int height) {
        this.width = validDimension(width);
        this.height = validDimension(height);
        x = this.width / 2;
        y = this.height / 2;
        clamp();
    }

    public void setBounds(int width, int height) {
        this.width = validDimension(width);
        this.height = validDimension(height);
        clamp();
    }

    public void move(Direction direction, int repeatCount) {
        int step = BASE_STEP + Math.min(Math.max(repeatCount, 0) * 7, 84);
        switch (direction) {
            case LEFT: x -= step; break;
            case RIGHT: x += step; break;
            case UP: y -= step; break;
            case DOWN: y += step; break;
        }
        clamp();
    }

    public void toggle() { enabled = !enabled; }
    public boolean isEnabled() { return enabled; }
    public int x() { return x; }
    public int y() { return y; }

    private int validDimension(int value) {
        return Math.max(value, EDGE_PADDING * 2 + 1);
    }

    private void clamp() {
        x = Math.max(EDGE_PADDING, Math.min(width - EDGE_PADDING, x));
        y = Math.max(EDGE_PADDING, Math.min(height - EDGE_PADDING, y));
    }
}
