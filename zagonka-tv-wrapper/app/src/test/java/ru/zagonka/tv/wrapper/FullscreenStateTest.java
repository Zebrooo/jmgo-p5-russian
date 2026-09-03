package ru.zagonka.tv.wrapper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class FullscreenStateTest {
    @Test
    public void transitionsAreIdempotent() {
        FullscreenState state = new FullscreenState();

        assertFalse(state.isFullscreen());
        assertTrue(state.enter());
        assertTrue(state.isFullscreen());
        assertFalse(state.enter());
        assertTrue(state.exit());
        assertFalse(state.isFullscreen());
        assertFalse(state.exit());
    }
}
