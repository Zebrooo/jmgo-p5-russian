package org.jmgo.input.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class MicrophoneKeyDebouncerTest {
    private static final int MIC = MicrophoneKeyPolicy.MICROPHONE_KEY_CODE;

    @Test
    public void ignoresBouncedPressWithinMinimumInterval() {
        MicrophoneKeyDebouncer debouncer = new MicrophoneKeyDebouncer(350L);

        assertTrue(debouncer.accept(MIC, 0, 0, 1_000L));
        assertFalse(debouncer.accept(MIC, 0, 0, 1_100L));
        assertFalse(debouncer.accept(MIC, 0, 0, 1_349L));
        assertTrue(debouncer.accept(MIC, 0, 0, 1_350L));
    }

    @Test
    public void stillRejectsRepeatsUpEventsAndOtherKeys() {
        MicrophoneKeyDebouncer debouncer = new MicrophoneKeyDebouncer(350L);

        assertFalse(debouncer.accept(MIC, 1, 0, 1_000L));
        assertFalse(debouncer.accept(MIC, 0, 1, 1_000L));
        assertFalse(debouncer.accept(23, 0, 0, 1_000L));
        assertTrue(debouncer.accept(MIC, 0, 0, 1_000L));
    }

    @Test
    public void rejectedEventsDoNotExtendTheInterval() {
        MicrophoneKeyDebouncer debouncer = new MicrophoneKeyDebouncer(350L);

        assertTrue(debouncer.accept(MIC, 0, 0, 1_000L));
        assertFalse(debouncer.accept(MIC, 0, 0, 1_300L));
        assertTrue(debouncer.accept(MIC, 0, 0, 1_360L));
    }

    @Test
    public void resetForgetsThePreviousPress() {
        MicrophoneKeyDebouncer debouncer = new MicrophoneKeyDebouncer(350L);

        assertTrue(debouncer.accept(MIC, 0, 0, 1_000L));
        debouncer.reset();
        assertTrue(debouncer.accept(MIC, 0, 0, 1_001L));
    }

    @Test
    public void zeroIntervalDisablesDebouncing() {
        MicrophoneKeyDebouncer debouncer = new MicrophoneKeyDebouncer(0L);

        assertTrue(debouncer.accept(MIC, 0, 0, 1_000L));
        assertTrue(debouncer.accept(MIC, 0, 0, 1_000L));
    }
}
