package me.efesser.flauncher.voice

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import org.jmgo.input.core.InputContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class NativeVoiceCaptureActivityTest {
    private val application get() = RuntimeEnvironment.getApplication()

    @Before
    fun clearBroadcasts() = shadowOf(application).clearBroadcastIntents()

    @Test
    fun startsTheRussianRecognizerForTheOriginPackage() {
        val activity = Robolectric.buildActivity(
            NativeVoiceCaptureActivity::class.java,
            NativeVoiceCaptureActivity.intent(application, UUID.randomUUID().toString(), "video.app"),
        ).setup().get()

        val recognizer = shadowOf(activity).nextStartedActivityForResult
        assertNotNull(recognizer)
        assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, recognizer.intent.action)
        assertEquals(InputContract.FUTO_PACKAGE, recognizer.intent.`package`)
        assertEquals(InputContract.RUSSIAN_LANGUAGE, recognizer.intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertFalse(activity.isFinishing)
        assertTrue(shadowOf(application).broadcastIntents.isEmpty())
    }

    @Test
    fun publishesSessionOriginAndTrimmedResultInsideTheLauncherPackage() {
        val sessionId = UUID.randomUUID().toString()
        val activity = Robolectric.buildActivity(
            NativeVoiceCaptureActivity::class.java,
            NativeVoiceCaptureActivity.intent(application, sessionId, "video.app"),
        ).setup().get()
        val recognizer = shadowOf(activity).nextStartedActivityForResult

        shadowOf(activity).receiveResult(
            recognizer.intent,
            Activity.RESULT_OK,
            Intent().putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, arrayListOf(" ", " матрица ")),
        )

        val result = shadowOf(application).broadcastIntents.single()
        assertEquals(InputContract.ACTION_NATIVE_VOICE_RESULT, result.action)
        assertEquals(application.packageName, result.`package`)
        assertEquals(sessionId, result.getStringExtra(InputContract.EXTRA_SESSION_ID))
        assertEquals("video.app", result.getStringExtra(InputContract.EXTRA_ORIGIN_PACKAGE))
        assertEquals("матрица", result.getStringExtra(InputContract.EXTRA_RESULT))
        assertTrue(activity.isFinishing)
    }

    @Test
    fun cancellationPublishesAnEmptyResultSoTheServiceClearsItsSession() {
        val sessionId = UUID.randomUUID().toString()
        val activity = Robolectric.buildActivity(
            NativeVoiceCaptureActivity::class.java,
            NativeVoiceCaptureActivity.intent(application, sessionId, "video.app"),
        ).setup().get()
        val recognizer = shadowOf(activity).nextStartedActivityForResult

        shadowOf(activity).receiveResult(recognizer.intent, Activity.RESULT_CANCELED, null)

        val result = shadowOf(application).broadcastIntents.single()
        assertEquals(sessionId, result.getStringExtra(InputContract.EXTRA_SESSION_ID))
        assertEquals("", result.getStringExtra(InputContract.EXTRA_RESULT))
        assertTrue(activity.isFinishing)
    }

    @Test
    fun refusesMissingOriginOrMalformedSession() {
        val noOrigin = Robolectric.buildActivity(
            NativeVoiceCaptureActivity::class.java,
            Intent(application, NativeVoiceCaptureActivity::class.java)
                .putExtra(InputContract.EXTRA_SESSION_ID, UUID.randomUUID().toString()),
        ).setup().get()
        assertTrue(noOrigin.isFinishing)
        assertNull(shadowOf(noOrigin).nextStartedActivityForResult)

        val badSession = Robolectric.buildActivity(
            NativeVoiceCaptureActivity::class.java,
            NativeVoiceCaptureActivity.intent(application, "not-a-uuid", "video.app"),
        ).setup().get()
        assertTrue(badSession.isFinishing)
        assertNull(shadowOf(badSession).nextStartedActivityForResult)

        assertTrue(shadowOf(application).broadcastIntents.isEmpty())
    }
}
