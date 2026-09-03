package org.futo.voiceinput

import android.os.SystemClock
import android.util.Log
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

object JmgoVoskEngine {
    private const val SAMPLE_RATE = 16000.0f
    private val lock = Any()

    @Volatile
    private var loadedPath: String? = null

    @Volatile
    private var model: Model? = null

    @Volatile
    private var recognizer: Recognizer? = null

    fun prepare(modelDirectory: File) {
        val path = modelDirectory.absolutePath
        if (model != null && recognizer != null && loadedPath == path) return

        synchronized(lock) {
            if (model != null && recognizer != null && loadedPath == path) return
            recognizer?.close()
            recognizer = null
            model?.close()
            model = null
            loadedPath = null

            val newModel = Model(path)
            try {
                val newRecognizer = Recognizer(newModel, SAMPLE_RATE)
                model = newModel
                recognizer = newRecognizer
                loadedPath = path
            } catch (error: Throwable) {
                newModel.close()
                throw error
            }
        }
    }

    fun transcribe(modelDirectory: File, samples: FloatArray): String = synchronized(lock) {
        prepare(modelDirectory)
        val activeRecognizer = requireNotNull(recognizer)
        val startedAt = SystemClock.elapsedRealtime()
        activeRecognizer.reset()
        val pcm16 = JmgoVoskPolicy.toPcm16(samples)
        val decodeStartedAt = SystemClock.elapsedRealtime()
        activeRecognizer.acceptWaveForm(pcm16, pcm16.size)
        val result = JmgoVoskPolicy.parseFinalResult(activeRecognizer.finalResult)
        Log.i(
            "JmgoVoiceInput",
            "vosk timing totalMs=${SystemClock.elapsedRealtime() - startedAt} " +
                "decodeMs=${SystemClock.elapsedRealtime() - decodeStartedAt}",
        )
        result
    }
}
