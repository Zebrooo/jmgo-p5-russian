package me.efesser.flauncher

import org.jmgo.input.core.InputContract
import org.jmgo.input.core.MicrophoneKeyPolicy

object VoiceKey {
    const val RECOGNIZER_PACKAGE = InputContract.FUTO_PACKAGE

    fun shouldHandle(keyCode: Int, action: Int, repeatCount: Int): Boolean =
        MicrophoneKeyPolicy.shouldHandle(keyCode, action, repeatCount)
}
