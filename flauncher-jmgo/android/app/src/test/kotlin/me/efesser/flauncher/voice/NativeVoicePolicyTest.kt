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

    @Test fun acceptsOnlyMatchingPackageSessionAndNonblankResult() {
        val gate = VoiceSessionGate(60_000)
        assertTrue(gate.start("session-a", 1_000))
        assertTrue(NativeVoicePolicy.canApplyResult(
            gate, "session-a", 2_000, "video.app", "video.app", " матрица ",
        ))

        assertTrue(gate.start("session-b", 3_000))
        assertFalse(NativeVoicePolicy.canApplyResult(
            gate, "session-b", 4_000, "video.app", "other.app", "матрица",
        ))
        gate.clear()

        assertTrue(gate.start("session-c", 5_000))
        assertFalse(NativeVoicePolicy.canApplyResult(
            gate, "session-c", 6_000, "video.app", "video.app", "   ",
        ))
    }

    @Test fun rejectsExpiredSession() {
        val gate = VoiceSessionGate(60_000)
        assertTrue(gate.start("session-a", 1_000))
        assertFalse(NativeVoicePolicy.canApplyResult(
            gate, "session-a", 61_001, "video.app", "video.app", "матрица",
        ))
    }
}
