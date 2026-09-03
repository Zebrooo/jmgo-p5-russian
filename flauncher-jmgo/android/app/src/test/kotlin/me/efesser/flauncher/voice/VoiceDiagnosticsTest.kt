package me.efesser.flauncher.voice

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VoiceDiagnosticsTest {
    @Before fun resetBefore() = VoiceDiagnostics.reset()

    @After fun resetAfter() = VoiceDiagnostics.reset()

    @Test fun startsEmpty() {
        val snapshot = VoiceDiagnostics.snapshot()
        assertNull(snapshot["serviceConnectedAtMs"])
        assertEquals(0L, snapshot["keyEventsSeenByService"])
        assertEquals(0L, snapshot["microphoneKeysSeenByService"])
        assertNull(snapshot["lastMicrophoneKeyInServiceAtMs"])
        assertNull(snapshot["lastMicrophoneKeyInLauncherAtMs"])
        assertNull(snapshot["lastOutcome"])
        assertNull(snapshot["lastOutcomeAtMs"])
    }

    @Test fun countsKeyEventsAndRemembersOnlyTheMicrophoneKeyTimestamp() {
        VoiceDiagnostics.serviceConnected(1_000L)
        VoiceDiagnostics.keyEventSeenByService(false, 2_000L)
        VoiceDiagnostics.keyEventSeenByService(true, 3_000L)
        VoiceDiagnostics.keyEventSeenByService(false, 4_000L)
        VoiceDiagnostics.microphoneKeySeenByLauncher(5_000L)
        VoiceDiagnostics.record(VoiceDiagnostics.Outcome.INSERTED, 6_000L)

        val snapshot = VoiceDiagnostics.snapshot()
        assertEquals(1_000L, snapshot["serviceConnectedAtMs"])
        assertEquals(3L, snapshot["keyEventsSeenByService"])
        assertEquals(1L, snapshot["microphoneKeysSeenByService"])
        assertEquals(3_000L, snapshot["lastMicrophoneKeyInServiceAtMs"])
        assertEquals(5_000L, snapshot["lastMicrophoneKeyInLauncherAtMs"])
        assertEquals("INSERTED", snapshot["lastOutcome"])
        assertEquals(6_000L, snapshot["lastOutcomeAtMs"])
    }

    @Test fun snapshotNeverCarriesFreeFormText() {
        VoiceDiagnostics.record(VoiceDiagnostics.Outcome.FIELD_OR_WINDOW_LOST, 1L)
        val outcomeNames = VoiceDiagnostics.Outcome.values().map { it.name }.toSet()
        for ((_, value) in VoiceDiagnostics.snapshot()) {
            assertTrue(value == null || value is Long || value in outcomeNames)
        }
    }

    @Test fun disconnectClearsTheConnectionTimestampOnly() {
        VoiceDiagnostics.serviceConnected(1_000L)
        VoiceDiagnostics.keyEventSeenByService(true, 2_000L)
        VoiceDiagnostics.serviceDisconnected()
        val snapshot = VoiceDiagnostics.snapshot()
        assertNull(snapshot["serviceConnectedAtMs"])
        assertEquals(1L, snapshot["microphoneKeysSeenByService"])
    }
}
