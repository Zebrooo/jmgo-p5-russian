package org.jmgo.input.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public final class InputCoreTest {
    @Test
    public void microphonePolicyConsumesOnlyInitialJmgoKeyDown() {
        assertTrue(MicrophoneKeyPolicy.shouldHandle(609, 0, 0));
        assertFalse(MicrophoneKeyPolicy.shouldHandle(609, 1, 0));
        assertFalse(MicrophoneKeyPolicy.shouldHandle(609, 0, 1));
        assertFalse(MicrophoneKeyPolicy.shouldHandle(23, 0, 0));
    }

    @Test
    public void voiceResultReturnsFirstTrimmedNonBlankPhrase() {
        assertEquals("русский запрос", VoiceResult.firstNonBlank(
                Arrays.asList("  ", "  русский запрос  ", "другой")));
        assertEquals("", VoiceResult.firstNonBlank(Collections.singletonList("  ")));
        assertEquals("", VoiceResult.firstNonBlank(null));
    }

    @Test
    public void sessionAcceptsOnlyMatchingUnexpiredResult() {
        VoiceSessionGate gate = new VoiceSessionGate(60_000L);

        assertTrue(gate.start("session-a", 1_000L));
        assertFalse(gate.start("session-b", 1_001L));
        assertFalse(gate.accept("wrong", 2_000L));
        assertTrue(gate.accept("session-a", 2_000L));
        assertFalse(gate.isActive(2_001L));

        assertTrue(gate.start("session-c", 3_000L));
        assertFalse(gate.accept("session-c", 63_001L));
        assertFalse(gate.isActive(63_001L));
    }

    @Test
    public void targetPolicyPrefersFocusedSafeFieldInOriginPackage() {
        EditableCandidate fallback = new EditableCandidate("video.app", true, false, true, false);
        EditableCandidate focused = new EditableCandidate("video.app", true, false, true, true);
        EditableCandidate foreign = new EditableCandidate("other.app", true, false, true, true);

        assertEquals(1, EditableTargetPolicy.select(
                Arrays.asList(fallback, focused, foreign), "video.app"));
    }

    @Test
    public void targetPolicyRejectsPasswordsAndForeignOrInvisibleFields() {
        assertEquals(-1, EditableTargetPolicy.select(Arrays.asList(
                new EditableCandidate("video.app", true, true, true, true),
                new EditableCandidate("other.app", true, false, true, true),
                new EditableCandidate("video.app", true, false, false, true)
        ), "video.app"));
    }

    @Test
    public void routingUsesWebCapabilityInsteadOfPackageNames() {
        assertEquals(VoiceRoute.NONE, VoiceRoutePolicy.select(false, true));
        assertEquals(VoiceRoute.WEB, VoiceRoutePolicy.select(true, true));
        assertEquals(VoiceRoute.NATIVE, VoiceRoutePolicy.select(true, false));
    }
}
