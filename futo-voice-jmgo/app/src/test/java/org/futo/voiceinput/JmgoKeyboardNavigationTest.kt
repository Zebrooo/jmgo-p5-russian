package org.futo.voiceinput

import org.junit.Assert.assertEquals
import org.junit.Test

class JmgoKeyboardNavigationTest {
    @Test
    fun movesAcrossLettersAndClampsAtEdges() {
        val rows = JmgoKeyboardLayout.rows(JmgoKeyboardLanguage.RUSSIAN)

        assertEquals(
            JmgoKeyboardSelection(0, 1),
            JmgoKeyboardNavigation.move(
                rows,
                JmgoKeyboardSelection(0, 0),
                JmgoKeyboardDirection.RIGHT,
            ),
        )
        assertEquals(
            JmgoKeyboardSelection(0, 0),
            JmgoKeyboardNavigation.move(
                rows,
                JmgoKeyboardSelection(0, 0),
                JmgoKeyboardDirection.LEFT,
            ),
        )
    }

    @Test
    fun preservesRelativeHorizontalPositionBetweenUnequalRows() {
        val rows = JmgoKeyboardLayout.rows(JmgoKeyboardLanguage.RUSSIAN)

        assertEquals(
            JmgoKeyboardSelection(1, 10),
            JmgoKeyboardNavigation.move(
                rows,
                JmgoKeyboardSelection(0, 11),
                JmgoKeyboardDirection.DOWN,
            ),
        )
        assertEquals(
            JmgoKeyboardSelection(2, 3),
            JmgoKeyboardNavigation.move(
                rows,
                JmgoKeyboardSelection(3, 2),
                JmgoKeyboardDirection.UP,
            ),
        )
    }

    @Test
    fun bottomRowExposesLanguageNumbersSpaceBackspaceAndSearch() {
        val rows = JmgoKeyboardLayout.rows(JmgoKeyboardLanguage.RUSSIAN)

        assertEquals(JmgoKeyboardKey.LANGUAGE, JmgoKeyboardNavigation.keyAt(rows, JmgoKeyboardSelection(3, 0)))
        assertEquals(JmgoKeyboardKey.NUMBERS, JmgoKeyboardNavigation.keyAt(rows, JmgoKeyboardSelection(3, 1)))
        assertEquals(JmgoKeyboardKey.SPACE, JmgoKeyboardNavigation.keyAt(rows, JmgoKeyboardSelection(3, 2)))
        assertEquals(JmgoKeyboardKey.BACKSPACE, JmgoKeyboardNavigation.keyAt(rows, JmgoKeyboardSelection(3, 3)))
        assertEquals("HIDE", JmgoKeyboardNavigation.keyAt(rows, JmgoKeyboardSelection(3, 4)).name)
        assertEquals(JmgoKeyboardKey.ENTER, JmgoKeyboardNavigation.keyAt(rows, JmgoKeyboardSelection(3, 5)))
    }

    @Test
    fun selectedLetterReturnsItsText() {
        val rows = JmgoKeyboardLayout.rows(JmgoKeyboardLanguage.RUSSIAN)

        assertEquals(JmgoKeyboardKey.TEXT, JmgoKeyboardNavigation.keyAt(rows, JmgoKeyboardSelection(0, 0)))
        assertEquals("й", JmgoKeyboardNavigation.textAt(rows, JmgoKeyboardSelection(0, 0)))
    }
}
