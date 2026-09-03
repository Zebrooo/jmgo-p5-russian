package org.jmgo.input.core;

/**
 * Stateful companion of {@link MicrophoneKeyPolicy}.
 *
 * <p>Some remotes and firmware builds deliver the microphone key twice for a single
 * physical press (a bounced hardware contact, or the same event arriving both through
 * the accessibility key filter and the foreground activity). A second accepted press
 * within {@code minIntervalMs} of the previous one is ignored so that a single physical
 * press never starts recognition and immediately finishes it again.</p>
 */
public final class MicrophoneKeyDebouncer {
    public static final long DEFAULT_MIN_INTERVAL_MS = 350L;

    private final long minIntervalMs;
    private long lastAcceptedAtMs = Long.MIN_VALUE;

    public MicrophoneKeyDebouncer() {
        this(DEFAULT_MIN_INTERVAL_MS);
    }

    public MicrophoneKeyDebouncer(long minIntervalMs) {
        if (minIntervalMs < 0) throw new IllegalArgumentException("minIntervalMs must not be negative");
        this.minIntervalMs = minIntervalMs;
    }

    /**
     * @return {@code true} only for the first KEY_DOWN of the microphone key that is not a
     * bounce of a press accepted less than {@code minIntervalMs} earlier.
     */
    public synchronized boolean accept(int keyCode, int action, int repeatCount, long nowMs) {
        if (!MicrophoneKeyPolicy.shouldHandle(keyCode, action, repeatCount)) return false;
        if (lastAcceptedAtMs != Long.MIN_VALUE && nowMs - lastAcceptedAtMs < minIntervalMs) return false;
        lastAcceptedAtMs = nowMs;
        return true;
    }

    public synchronized void reset() {
        lastAcceptedAtMs = Long.MIN_VALUE;
    }
}
