package org.jmgo.input.core;

public final class MicrophoneKeyPolicy {
    public static final int MICROPHONE_KEY_CODE = 609;
    private static final int ACTION_DOWN = 0;

    private MicrophoneKeyPolicy() {}

    public static boolean shouldHandle(int keyCode, int action, int repeatCount) {
        return keyCode == MICROPHONE_KEY_CODE && action == ACTION_DOWN && repeatCount == 0;
    }
}
