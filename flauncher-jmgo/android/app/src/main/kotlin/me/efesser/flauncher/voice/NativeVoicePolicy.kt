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

    fun canApplyResult(
        gate: VoiceSessionGate,
        sessionId: String?,
        nowMs: Long,
        originPackage: String?,
        currentPackage: String?,
        result: String?,
    ): Boolean {
        if (originPackage.isNullOrBlank() || originPackage != currentPackage) return false
        if (result.isNullOrBlank()) return false
        return gate.accept(sessionId, nowMs)
    }
}
