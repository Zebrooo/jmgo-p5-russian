package me.efesser.flauncher.voice

import org.jmgo.input.core.MicrophoneKeyPolicy
import org.jmgo.input.core.VoiceRoute
import org.jmgo.input.core.VoiceRoutePolicy
import org.jmgo.input.core.VoiceSessionGate

object NativeVoicePolicy {
    fun shouldConsume(keyCode: Int, action: Int, repeatCount: Int): Boolean =
        MicrophoneKeyPolicy.shouldHandle(keyCode, action, repeatCount)

    fun route(foregroundPackage: String?, resolvesWebVoice: Boolean): VoiceRoute =
        VoiceRoutePolicy.select(!foregroundPackage.isNullOrBlank(), resolvesWebVoice)

    fun validatedResult(
        gate: VoiceSessionGate,
        sessionId: String?,
        nowMs: Long,
        originPackage: String?,
        declaredOriginPackage: String?,
        result: String?,
    ): String? {
        if (originPackage.isNullOrBlank() || originPackage != declaredOriginPackage) return null
        if (sessionId.isNullOrBlank()) return null
        if (result.isNullOrBlank()) return null
        if (gate.currentSessionId(nowMs) != sessionId) return null
        return result.trim()
    }

    fun isWindowReady(
        originPackage: String?,
        currentPackage: String?,
        sameWindow: Boolean,
        hasEditableTarget: Boolean,
    ): Boolean = !originPackage.isNullOrBlank() &&
        originPackage == currentPackage && sameWindow && hasEditableTarget
}
