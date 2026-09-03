package org.jmgo.input.core;

import java.util.List;

public final class VoiceResult {
    private VoiceResult() {}

    public static String firstNonBlank(List<String> candidates) {
        if (candidates == null) return "";
        for (String candidate : candidates) {
            if (candidate == null) continue;
            String normalized = candidate.trim();
            if (!normalized.isEmpty()) return normalized;
        }
        return "";
    }
}
