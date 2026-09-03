package me.efesser.flauncher.voice

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import me.efesser.flauncher.R
import org.jmgo.input.core.InputContract
import org.jmgo.input.core.VoiceResult
import java.util.UUID

class NativeVoiceCaptureActivity : Activity() {
    private var sessionId: String? = null
    private var originPackage: String? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        sessionId = state?.getString(InputContract.EXTRA_SESSION_ID)
            ?: validSessionId(intent.getStringExtra(InputContract.EXTRA_SESSION_ID))
        originPackage = state?.getString(InputContract.EXTRA_ORIGIN_PACKAGE)
            ?: intent.getStringExtra(InputContract.EXTRA_ORIGIN_PACKAGE)?.takeIf { it.isNotBlank() }
        if (sessionId == null || originPackage == null) {
            finish()
            return
        }
        if (state == null) launchRecognizer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(InputContract.EXTRA_SESSION_ID, sessionId)
        outState.putString(InputContract.EXTRA_ORIGIN_PACKAGE, originPackage)
        super.onSaveInstanceState(outState)
    }

    @Suppress("DEPRECATION")
    private fun launchRecognizer() {
        val recognizer = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .setPackage(InputContract.FUTO_PACKAGE)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, InputContract.RUSSIAN_LANGUAGE)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.native_voice_prompt))
        try {
            startActivityForResult(recognizer, REQUEST_RECOGNITION)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.native_voice_missing, Toast.LENGTH_SHORT).show()
            publishResult("")
        }
    }

    @Deprecated("Deprecated in Android framework")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_RECOGNITION) return
        val candidates = if (resultCode == RESULT_OK) {
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        } else {
            null
        }
        publishResult(VoiceResult.firstNonBlank(candidates))
    }

    private fun publishResult(result: String) {
        // No artificial delay: the accessibility service holds the result until the originating
        // window is back in front and re-reads the accessibility tree at that moment.
        sendBroadcast(
            Intent(InputContract.ACTION_NATIVE_VOICE_RESULT)
                .setPackage(packageName)
                .putExtra(InputContract.EXTRA_SESSION_ID, sessionId)
                .putExtra(InputContract.EXTRA_ORIGIN_PACKAGE, originPackage)
                .putExtra(InputContract.EXTRA_RESULT, result),
        )
        finish()
    }

    companion object {
        private const val REQUEST_RECOGNITION = 4201

        fun intent(context: Context, sessionId: String, originPackage: String): Intent =
            Intent(context, NativeVoiceCaptureActivity::class.java)
                .putExtra(InputContract.EXTRA_SESSION_ID, sessionId)
                .putExtra(InputContract.EXTRA_ORIGIN_PACKAGE, originPackage)

        private fun validSessionId(value: String?): String? = try {
            value?.let { UUID.fromString(it).toString() }
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
