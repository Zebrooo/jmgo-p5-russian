package me.efesser.flauncher

import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Package visibility on Android 11+: the launcher may only resolve the recognizer and the
 * WEB_VOICE capability of other packages if the manifest declares them under `<queries>`.
 */
class ManifestQueriesTest {
    private val manifest: Element by lazy {
        val file = listOf("src/main/AndroidManifest.xml", "flauncher-jmgo/android/app/src/main/AndroidManifest.xml")
            .map(::File)
            .first { it.exists() }
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(file).documentElement
    }

    private fun queries(tag: String, name: String): Boolean {
        val queries = manifest.getElementsByTagName("queries")
        for (index in 0 until queries.length) {
            val nodes = (queries.item(index) as Element).getElementsByTagName(tag)
            for (nodeIndex in 0 until nodes.length) {
                val element = nodes.item(nodeIndex) as Element
                if (element.getAttributeNS(ANDROID_NS, "name") == name) return true
            }
        }
        return false
    }

    @Test
    fun declaresVisibilityOfTheRecognizerPackage() {
        assertTrue(queries("package", "org.futo.voiceinput.jmgo"))
    }

    @Test
    fun declaresVisibilityOfTheWebVoiceCapabilityAndSpeechRecognition() {
        assertTrue(queries("action", "org.jmgo.input.action.WEB_VOICE"))
        assertTrue(queries("action", "android.speech.action.RECOGNIZE_SPEECH"))
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
