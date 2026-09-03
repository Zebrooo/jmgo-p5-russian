package me.efesser.flauncher.voice

import org.jmgo.input.core.VoiceRoute
import org.jmgo.input.core.VoiceSessionGate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class NativeVoicePolicyTest {
    @Test fun consumesOnlyTheInitialMicrophoneKeyDown() {
        assertTrue(NativeVoicePolicy.shouldConsume(609, 0, 0))
        assertFalse(NativeVoicePolicy.shouldConsume(609, 1, 0))
        assertFalse(NativeVoicePolicy.shouldConsume(609, 0, 1))
        assertFalse(NativeVoicePolicy.shouldConsume(22, 0, 0))
        assertFalse(NativeVoicePolicy.shouldConsume(82, 0, 0))
    }

    @Test fun routesByForegroundWebCapability() {
        assertEquals(VoiceRoute.NONE, NativeVoicePolicy.route(null, false))
        assertEquals(VoiceRoute.WEB, NativeVoicePolicy.route("web.host", true))
        assertEquals(VoiceRoute.NATIVE, NativeVoicePolicy.route("video.app", false))
    }

    @Test fun validatesResultEnvelopeWithoutConsumingTheSession() {
        val gate = VoiceSessionGate(60_000)
        assertTrue(gate.start("session-a", 1_000))
        assertEquals("матрица", NativeVoicePolicy.validatedResult(
            gate, "session-a", 2_000, "video.app", "video.app", " матрица ",
        ))
        assertTrue(gate.isActive(2_001))

        assertEquals(null, NativeVoicePolicy.validatedResult(
            gate, "session-a", 2_002, "video.app", "other.app", "матрица",
        ))
        assertEquals(null, NativeVoicePolicy.validatedResult(
            gate, "session-a", 2_003, "video.app", "video.app", "   ",
        ))
        assertEquals(null, NativeVoicePolicy.validatedResult(
            gate, "wrong", 2_004, "video.app", "video.app", "матрица",
        ))
        gate.clear()
        assertEquals(null, NativeVoicePolicy.validatedResult(
            gate, null, 2_005, "video.app", "video.app", "матрица",
        ))
    }

    @Test fun rejectsExpiredSession() {
        val gate = VoiceSessionGate(60_000)
        assertTrue(gate.start("session-a", 1_000))
        assertEquals(null, NativeVoicePolicy.validatedResult(
            gate, "session-a", 61_001, "video.app", "video.app", "матрица",
        ))
    }

    @Test fun waitsForTheOriginalEditableWindowBeforeApplying() {
        assertFalse(NativeVoicePolicy.isWindowReady(
            "video.app", "org.futo.voiceinput.jmgo", true, true,
        ))
        assertFalse(NativeVoicePolicy.isWindowReady(
            "video.app", "video.app", false, true,
        ))
        assertFalse(NativeVoicePolicy.isWindowReady(
            "video.app", "video.app", true, false,
        ))
        assertTrue(NativeVoicePolicy.isWindowReady(
            "video.app", "video.app", true, true,
        ))
    }
}
