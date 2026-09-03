package org.jmgo.input.web;

import org.jmgo.input.core.MicrophoneKeyPolicy;

public final class WebKeyPolicy {
    private static final int KEYCODE_BACK = 4;
    private static final int KEYCODE_DPAD_UP = 19;
    private static final int KEYCODE_DPAD_DOWN = 20;
    private static final int KEYCODE_DPAD_LEFT = 21;
    private static final int KEYCODE_DPAD_RIGHT = 22;
    private static final int KEYCODE_DPAD_CENTER = 23;
    private static final int KEYCODE_ENTER = 66;
    private static final int KEYCODE_MENU = 82;

    private WebKeyPolicy() {}

    public static boolean shouldHandle(
            int keyCode,
            int action,
            int repeatCount,
            boolean resumed,
            boolean cursorVisible,
            boolean keyboardVisible,
            boolean fullscreen
    ) {
        if (!resumed || fullscreen || keyCode == KEYCODE_BACK) return false;
        if (keyCode == KEYCODE_MENU) return true;
        if (MicrophoneKeyPolicy.shouldHandle(keyCode, action, repeatCount)) return true;
        if (!cursorVisible && !keyboardVisible) return false;
        return keyCode == KEYCODE_DPAD_UP
                || keyCode == KEYCODE_DPAD_DOWN
                || keyCode == KEYCODE_DPAD_LEFT
                || keyCode == KEYCODE_DPAD_RIGHT
                || keyCode == KEYCODE_DPAD_CENTER
                || keyCode == KEYCODE_ENTER;
    }
}
