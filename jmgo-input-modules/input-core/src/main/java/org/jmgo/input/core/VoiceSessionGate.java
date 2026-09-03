package org.jmgo.input.core;

public final class VoiceSessionGate {
    private final long timeoutMs;
    private String sessionId;
    private long startedAtMs;

    public VoiceSessionGate(long timeoutMs) {
        if (timeoutMs <= 0) throw new IllegalArgumentException("timeoutMs must be positive");
        this.timeoutMs = timeoutMs;
    }

    public synchronized boolean start(String candidateSessionId, long nowMs) {
        expireIfNeeded(nowMs);
        if (sessionId != null || candidateSessionId == null || candidateSessionId.trim().isEmpty()) {
            return false;
        }
        sessionId = candidateSessionId;
        startedAtMs = nowMs;
        return true;
    }

    public synchronized boolean accept(String candidateSessionId, long nowMs) {
        expireIfNeeded(nowMs);
        if (sessionId == null || !sessionId.equals(candidateSessionId)) return false;
        clear();
        return true;
    }

    public synchronized boolean isActive(long nowMs) {
        expireIfNeeded(nowMs);
        return sessionId != null;
    }

    public synchronized String currentSessionId(long nowMs) {
        expireIfNeeded(nowMs);
        return sessionId;
    }

    public synchronized void clear() {
        sessionId = null;
        startedAtMs = 0L;
    }

    private void expireIfNeeded(long nowMs) {
        if (sessionId != null && nowMs - startedAtMs > timeoutMs) clear();
    }
}
