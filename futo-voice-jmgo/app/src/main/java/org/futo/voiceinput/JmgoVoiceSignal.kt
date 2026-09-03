package org.futo.voiceinput

import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo

enum class JmgoVoiceAction {
    START,
    FINISH,
    IGNORE,
}

class JmgoVoiceSession {
    var isActive: Boolean = false
        private set

    fun onSignal(hasActiveInput: Boolean): JmgoVoiceAction {
        if (!hasActiveInput) {
            reset()
            return JmgoVoiceAction.IGNORE
        }

        return if (isActive) onFinishSignal() else onStartSignal(hasActiveInput = true)
    }

    fun onStartSignal(hasActiveInput: Boolean): JmgoVoiceAction {
        if (!hasActiveInput || isActive) return JmgoVoiceAction.IGNORE
        isActive = true
        return JmgoVoiceAction.START
    }

    fun onFinishSignal(): JmgoVoiceAction {
        if (!isActive) return JmgoVoiceAction.IGNORE
        isActive = false
        return JmgoVoiceAction.FINISH
    }

    fun reset() {
        isActive = false
    }
}

object JmgoVoiceSignal {
    const val ACTION = "com.jmgo.action.AI_VOICE"
    private const val MICROPHONE_KEY_CODE = 609
    const val DEFAULT_LANGUAGE = "ru"
    const val DEFAULT_MULTILINGUAL = true
    const val DEFAULT_MULTILINGUAL_MODEL_INDEX = 0
    const val DEFAULT_BEAM_SEARCH = false

    fun shouldFinish(action: String?, isRecording: Boolean): Boolean =
        action == ACTION && isRecording

    fun shouldFinishRepeatedRequest(action: String?): Boolean =
        action == RecognizerIntent.ACTION_RECOGNIZE_SPEECH

    fun shouldHandleKey(
        keyCode: Int,
        action: Int,
        repeatCount: Int,
    ): Boolean =
        keyCode == MICROPHONE_KEY_CODE &&
            action == KeyEvent.ACTION_DOWN &&
            repeatCount == 0

    fun submitAction(imeOptions: Int): Int? {
        val action = imeOptions and EditorInfo.IME_MASK_ACTION
        return when (action) {
            EditorInfo.IME_ACTION_GO,
            EditorInfo.IME_ACTION_SEARCH,
            EditorInfo.IME_ACTION_SEND,
            EditorInfo.IME_ACTION_DONE -> action
            else -> null
        }
    }

    fun shouldSendEnterKey(imeOptions: Int): Boolean =
        when (imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_GO,
            EditorInfo.IME_ACTION_SEARCH -> true
            else -> false
        }
}

object JmgoImeSurfacePolicy {
    fun shouldShowInputView(isJmgoBuild: Boolean): Boolean = !isJmgoBuild
}
