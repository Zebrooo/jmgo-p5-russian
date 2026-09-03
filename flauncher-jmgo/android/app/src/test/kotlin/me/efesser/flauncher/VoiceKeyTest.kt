package me.efesser.flauncher

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceKeyTest {
    @Test fun targetsTheJmgoRussianVoicePackage() {
        assertEquals("org.futo.voiceinput.jmgo", VoiceKey.RECOGNIZER_PACKAGE)
    }

    @Test fun handlesOnlyInitialJmgoMicrophoneKeyDown() {
        assertTrue(VoiceKey.shouldHandle(609, KeyEvent.ACTION_DOWN, 0))
        assertFalse(VoiceKey.shouldHandle(609, KeyEvent.ACTION_UP, 0))
        assertFalse(VoiceKey.shouldHandle(609, KeyEvent.ACTION_DOWN, 1))
        assertFalse(VoiceKey.shouldHandle(KeyEvent.KEYCODE_SEARCH, KeyEvent.ACTION_DOWN, 0))
    }
}
