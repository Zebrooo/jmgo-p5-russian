package org.futo.voiceinput

import org.junit.Assert.assertTrue
import org.junit.Test

class JmgoKeyboardLayoutTest {
    @Test
    fun providesRussianEnglishAndNumberLayouts() {
        assertTrue(JmgoKeyboardLayout.rows(JmgoKeyboardLanguage.RUSSIAN).flatten().contains("я"))
        assertTrue(JmgoKeyboardLayout.rows(JmgoKeyboardLanguage.ENGLISH).flatten().contains("q"))
        assertTrue(JmgoKeyboardLayout.rows(JmgoKeyboardLanguage.NUMBERS).flatten().contains("7"))
    }

    @Test
    fun everyLayoutHasAtLeastThreeRows() {
        JmgoKeyboardLanguage.entries.forEach { language ->
            assertTrue(JmgoKeyboardLayout.rows(language).size >= 3)
        }
    }
}
