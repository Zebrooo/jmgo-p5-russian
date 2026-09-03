package org.futo.voiceinput

import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JmgoVoiceSignalTest {
    @Test
    fun routesFirstSignalToStartAndSecondSignalToFinish() {
        val session = JmgoVoiceSession()

        assertEquals(JmgoVoiceAction.START, session.onSignal(hasActiveInput = true))
        assertEquals(JmgoVoiceAction.FINISH, session.onSignal(hasActiveInput = true))
        assertEquals(JmgoVoiceAction.START, session.onSignal(hasActiveInput = true))
    }

    @Test
    fun explicitSignalsAreIdempotent() {
        val session = JmgoVoiceSession()

        assertEquals(JmgoVoiceAction.START, session.onStartSignal(hasActiveInput = true))
        assertEquals(JmgoVoiceAction.IGNORE, session.onStartSignal(hasActiveInput = true))
        assertEquals(JmgoVoiceAction.FINISH, session.onFinishSignal())
        assertEquals(JmgoVoiceAction.IGNORE, session.onFinishSignal())
        assertEquals(JmgoVoiceAction.IGNORE, session.onStartSignal(hasActiveInput = false))
    }

    @Test
    fun jmgoBuildDoesNotExposeItsImeSurfaceInNativeOrWebHosts() {
        assertFalse(JmgoImeSurfacePolicy.shouldShowInputView(isJmgoBuild = true))
        assertTrue(JmgoImeSurfacePolicy.shouldShowInputView(isJmgoBuild = false))
    }

    @Test
    fun ignoresSignalsWithoutAnActiveTextFieldAndResetsWithInputLifecycle() {
        val session = JmgoVoiceSession()

        assertEquals(JmgoVoiceAction.IGNORE, session.onSignal(hasActiveInput = false))
        assertEquals(JmgoVoiceAction.START, session.onSignal(hasActiveInput = true))
        session.reset()
        assertEquals(JmgoVoiceAction.START, session.onSignal(hasActiveInput = true))
    }

    @Test
    fun submitsOnlySearchLikeEditorActionsAfterRecognition() {
        assertEquals(EditorInfo.IME_ACTION_GO, JmgoVoiceSignal.submitAction(EditorInfo.IME_ACTION_GO))
        assertEquals(EditorInfo.IME_ACTION_SEARCH, JmgoVoiceSignal.submitAction(EditorInfo.IME_ACTION_SEARCH))
        assertEquals(EditorInfo.IME_ACTION_SEND, JmgoVoiceSignal.submitAction(EditorInfo.IME_ACTION_SEND))
        assertEquals(EditorInfo.IME_ACTION_DONE, JmgoVoiceSignal.submitAction(EditorInfo.IME_ACTION_DONE))
        assertEquals(null, JmgoVoiceSignal.submitAction(EditorInfo.IME_ACTION_NEXT))
        assertEquals(null, JmgoVoiceSignal.submitAction(EditorInfo.IME_ACTION_NONE))
        assertEquals(
            EditorInfo.IME_ACTION_SEARCH,
            JmgoVoiceSignal.submitAction(EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_SEARCH),
        )
    }

    @Test
    fun sendsPhysicalEnterForWebSearchActions() {
        assertTrue(JmgoVoiceSignal.shouldSendEnterKey(EditorInfo.IME_ACTION_GO))
        assertTrue(JmgoVoiceSignal.shouldSendEnterKey(EditorInfo.IME_ACTION_SEARCH))
        assertFalse(JmgoVoiceSignal.shouldSendEnterKey(EditorInfo.IME_ACTION_SEND))
        assertFalse(JmgoVoiceSignal.shouldSendEnterKey(EditorInfo.IME_ACTION_DONE))
        assertFalse(JmgoVoiceSignal.shouldSendEnterKey(EditorInfo.IME_ACTION_NONE))
    }

    @Test
    fun handlesOnlyInitialJmgoMicrophoneKeyDown() {
        assertTrue(JmgoVoiceSignal.shouldHandleKey(609, KeyEvent.ACTION_DOWN, 0))
        assertFalse(JmgoVoiceSignal.shouldHandleKey(609, KeyEvent.ACTION_UP, 0))
        assertFalse(JmgoVoiceSignal.shouldHandleKey(609, KeyEvent.ACTION_DOWN, 1))
        assertFalse(JmgoVoiceSignal.shouldHandleKey(KeyEvent.KEYCODE_ENTER, KeyEvent.ACTION_DOWN, 0))
    }

    @Test
    fun finishesOnlyActiveRecognitionForJmgoVoiceSignal() {
        assertTrue(JmgoVoiceSignal.shouldFinish(JmgoVoiceSignal.ACTION, true))
        assertFalse(JmgoVoiceSignal.shouldFinish(JmgoVoiceSignal.ACTION, false))
        assertFalse(JmgoVoiceSignal.shouldFinish("another.action", true))
    }

    @Test
    fun repeatedSpeechRecognitionRequestFinishesCurrentSession() {
        assertTrue(
            JmgoVoiceSignal.shouldFinishRepeatedRequest(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH,
            ),
        )
        assertFalse(JmgoVoiceSignal.shouldFinishRepeatedRequest("another.action"))
        assertFalse(JmgoVoiceSignal.shouldFinishRepeatedRequest(null))
    }

    @Test
    fun defaultsToRussianForJmgoInstallation() {
        assertEquals("ru", JmgoVoiceSignal.DEFAULT_LANGUAGE)
        assertTrue(JmgoVoiceSignal.DEFAULT_MULTILINGUAL)
        assertEquals(0, JmgoVoiceSignal.DEFAULT_MULTILINGUAL_MODEL_INDEX)
        assertFalse(JmgoVoiceSignal.DEFAULT_BEAM_SEARCH)
    }
}
