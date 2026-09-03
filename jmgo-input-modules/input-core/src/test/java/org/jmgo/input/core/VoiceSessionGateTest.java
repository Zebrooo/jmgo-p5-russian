package org.jmgo.input.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class VoiceSessionGateTest {
    @Test
    public void duplicateResultForAnAcceptedSessionIsRejected() {
        VoiceSessionGate gate = new VoiceSessionGate(60_000L);

        assertTrue(gate.start("session-a", 1_000L));
        assertTrue(gate.accept("session-a", 2_000L));
        assertFalse(gate.accept("session-a", 2_001L));
        assertFalse(gate.isActive(2_002L));
    }

    @Test
    public void staleSessionIdIsRejectedAfterANewSessionStarts() {
        VoiceSessionGate gate = new VoiceSessionGate(60_000L);

        assertTrue(gate.start("session-a", 1_000L));
        gate.clear();
        assertTrue(gate.start("session-b", 1_500L));
        assertFalse(gate.accept("session-a", 2_000L));
        assertTrue(gate.isActive(2_001L));
        assertEquals("session-b", gate.currentSessionId(2_002L));
        assertTrue(gate.accept("session-b", 2_003L));
    }

    @Test
    public void expiredSessionReleasesTheGateForANewSession() {
        VoiceSessionGate gate = new VoiceSessionGate(60_000L);

        assertTrue(gate.start("session-a", 1_000L));
        assertFalse(gate.start("session-b", 61_000L));
        assertTrue(gate.start("session-b", 61_001L));
        assertFalse(gate.accept("session-a", 61_002L));
        assertTrue(gate.accept("session-b", 61_003L));
    }

    @Test
    public void resultExactlyAtTheTimeoutBoundaryIsStillAccepted() {
        VoiceSessionGate gate = new VoiceSessionGate(60_000L);

        assertTrue(gate.start("session-a", 1_000L));
        assertTrue(gate.accept("session-a", 61_000L));
    }

    @Test
    public void blankOrNullSessionIdsNeverStartOrMatch() {
        VoiceSessionGate gate = new VoiceSessionGate(60_000L);

        assertFalse(gate.start(null, 1_000L));
        assertFalse(gate.start("   ", 1_000L));
        assertNull(gate.currentSessionId(1_001L));
        assertFalse(gate.accept(null, 1_002L));

        assertTrue(gate.start("session-a", 2_000L));
        assertFalse(gate.accept(null, 2_001L));
        assertTrue(gate.isActive(2_002L));
    }

    @Test
    public void rejectsNonPositiveTimeout() {
        try {
            new VoiceSessionGate(0L);
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }
}
