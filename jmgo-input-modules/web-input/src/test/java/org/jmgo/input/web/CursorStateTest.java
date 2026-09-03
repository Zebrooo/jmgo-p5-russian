package org.jmgo.input.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class CursorStateTest {
    @Test
    public void startsCenteredAndDisabledThenMovesWithAcceleration() {
        CursorState state = new CursorState(1920, 1080);
        assertFalse(state.isEnabled());
        assertEquals(960, state.x());
        assertEquals(540, state.y());

        state.toggle();
        state.move(CursorState.Direction.RIGHT, 0);
        assertTrue(state.isEnabled());
        assertEquals(996, state.x());
        state.move(CursorState.Direction.DOWN, 2);
        assertEquals(590, state.y());
    }

    @Test
    public void clampsToEighteenPixelEdgePadding() {
        CursorState state = new CursorState(100, 80);
        for (int index = 0; index < 10; index++) {
            state.move(CursorState.Direction.LEFT, 10);
            state.move(CursorState.Direction.UP, 10);
        }
        assertEquals(18, state.x());
        assertEquals(18, state.y());
    }
}
