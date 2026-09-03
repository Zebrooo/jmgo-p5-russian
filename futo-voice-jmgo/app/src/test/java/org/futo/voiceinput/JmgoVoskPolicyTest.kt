package org.futo.voiceinput

import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JmgoVoskPolicyTest {
    @Test
    fun selectsVoskOnlyForJmgoPackageWithInstalledModel() {
        assertTrue(JmgoVoskPolicy.shouldUse("org.futo.voiceinput.jmgo", true))
        assertFalse(JmgoVoskPolicy.shouldUse("org.futo.voiceinput", true))
        assertFalse(JmgoVoskPolicy.shouldUse("org.futo.voiceinput.jmgo", false))
    }

    @Test
    fun extractsOnlyFinalTextFromVoskJson() {
        assertEquals("матрица", JmgoVoskPolicy.parseFinalResult("{\"text\":\"  матрица  \"}"))
        assertEquals("", JmgoVoskPolicy.parseFinalResult("{\"partial\":\"матри\"}"))
        assertEquals("", JmgoVoskPolicy.parseFinalResult("not-json"))
    }

    @Test
    fun acceptsOnlyNonBlankRecognitionResults() {
        assertTrue(JmgoVoskPolicy.shouldAcceptResult("матрица"))
        assertFalse(JmgoVoskPolicy.shouldAcceptResult(""))
        assertFalse(JmgoVoskPolicy.shouldAcceptResult("   "))
    }

    @Test
    fun restoresNormalizedAudioToPcm16Amplitude() {
        assertArrayEquals(
            shortArrayOf(Short.MIN_VALUE, -16383, 0, 16383, Short.MAX_VALUE),
            JmgoVoskPolicy.toPcm16(floatArrayOf(-1.0f, -0.5f, 0.0f, 0.5f, 1.0f)),
        )
    }
}
