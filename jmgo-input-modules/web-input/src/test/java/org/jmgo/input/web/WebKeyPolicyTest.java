package org.jmgo.input.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class WebKeyPolicyTest {
    @Test
    public void ownsMenuAndInitialMicrophoneDownOnlyWhileHostCanHandleThem() {
        assertTrue(WebKeyPolicy.shouldHandle(82, 0, 0, true, false, false, false));
        assertTrue(WebKeyPolicy.shouldHandle(82, 1, 0, true, false, false, false));
        assertTrue(WebKeyPolicy.shouldHandle(609, 0, 0, true, false, false, false));
        assertFalse(WebKeyPolicy.shouldHandle(609, 0, 1, true, false, false, false));
        assertFalse(WebKeyPolicy.shouldHandle(609, 0, 0, false, false, false, false));
        assertFalse(WebKeyPolicy.shouldHandle(82, 0, 0, true, false, false, true));
    }

    @Test
    public void ownsNavigationOnlyForVisibleCursorOrKeyboardOutsideFullscreen() {
        assertFalse(WebKeyPolicy.shouldHandle(22, 0, 0, true, false, false, false));
        assertTrue(WebKeyPolicy.shouldHandle(22, 0, 0, true, true, false, false));
        assertTrue(WebKeyPolicy.shouldHandle(23, 0, 0, true, false, true, false));
        assertFalse(WebKeyPolicy.shouldHandle(22, 0, 0, true, true, false, true));
        assertFalse(WebKeyPolicy.shouldHandle(4, 0, 0, true, true, false, false));
    }
}
