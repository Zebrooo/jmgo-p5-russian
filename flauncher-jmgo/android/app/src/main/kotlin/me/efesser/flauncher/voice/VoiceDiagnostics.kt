package me.efesser.flauncher.voice

/**
 * In-memory, text-free diagnostics for the setup screen.
 *
 * Only timestamps, counters and outcome names are recorded. Recognized speech, field
 * contents, node text and raw key codes other than the microphone key are never stored.
 */
object VoiceDiagnostics {
    enum class Outcome {
        SESSION_STARTED,
        ROUTED_TO_WEB_HOST,
        NO_FOREGROUND_WINDOW,
        RECOGNIZER_MISSING,
        RESULT_EMPTY_OR_STALE,
        INSERTED,
        FIELD_OR_WINDOW_LOST,
    }

    private val lock = Any()
    private var serviceConnectedAtMs: Long? = null
    private var keyEventsSeenByService: Long = 0
    private var microphoneKeysSeenByService: Long = 0
    private var lastMicrophoneKeyInServiceAtMs: Long? = null
    private var lastMicrophoneKeyInLauncherAtMs: Long? = null
    private var lastOutcome: Outcome? = null
    private var lastOutcomeAtMs: Long? = null

    fun serviceConnected(nowMs: Long) = synchronized(lock) { serviceConnectedAtMs = nowMs }

    fun serviceDisconnected() = synchronized(lock) { serviceConnectedAtMs = null }

    fun keyEventSeenByService(isMicrophoneKey: Boolean, nowMs: Long) = synchronized(lock) {
        keyEventsSeenByService += 1
        if (isMicrophoneKey) {
            microphoneKeysSeenByService += 1
            lastMicrophoneKeyInServiceAtMs = nowMs
        }
    }

    fun microphoneKeySeenByLauncher(nowMs: Long) = synchronized(lock) {
        lastMicrophoneKeyInLauncherAtMs = nowMs
    }

    fun record(outcome: Outcome, nowMs: Long) = synchronized(lock) {
        lastOutcome = outcome
        lastOutcomeAtMs = nowMs
    }

    fun reset() = synchronized(lock) {
        serviceConnectedAtMs = null
        keyEventsSeenByService = 0
        microphoneKeysSeenByService = 0
        lastMicrophoneKeyInServiceAtMs = null
        lastMicrophoneKeyInLauncherAtMs = null
        lastOutcome = null
        lastOutcomeAtMs = null
    }

    fun snapshot(): Map<String, Any?> = synchronized(lock) {
        mapOf(
            "serviceConnectedAtMs" to serviceConnectedAtMs,
            "keyEventsSeenByService" to keyEventsSeenByService,
            "microphoneKeysSeenByService" to microphoneKeysSeenByService,
            "lastMicrophoneKeyInServiceAtMs" to lastMicrophoneKeyInServiceAtMs,
            "lastMicrophoneKeyInLauncherAtMs" to lastMicrophoneKeyInLauncherAtMs,
            "lastOutcome" to lastOutcome?.name,
            "lastOutcomeAtMs" to lastOutcomeAtMs,
        )
    }
}
