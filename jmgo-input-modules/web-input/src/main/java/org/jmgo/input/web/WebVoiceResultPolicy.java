package org.jmgo.input.web;

public final class WebVoiceResultPolicy {
    private WebVoiceResultPolicy() {}

    public static boolean shouldQueue(boolean resumed, boolean sessionMatches, String result) {
        return !resumed && sessionMatches && result != null && !result.trim().isEmpty();
    }

    public static boolean shouldInspect(boolean resumed, boolean sessionAccepted, String result) {
        return resumed && sessionAccepted && result != null && !result.trim().isEmpty();
    }

    public static boolean shouldApply(boolean resumed, boolean safeElementActive) {
        return resumed && safeElementActive;
    }
}
