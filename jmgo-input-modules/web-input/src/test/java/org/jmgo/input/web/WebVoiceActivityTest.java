package org.jmgo.input.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.speech.RecognizerIntent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.jmgo.input.core.InputContract;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.TIRAMISU)
public final class WebVoiceActivityTest {
    private Application application;
    private String packageName;

    @Before
    public void setUp() {
        application = RuntimeEnvironment.getApplication();
        packageName = application.getPackageName();
        shadowOf(application).clearBroadcastIntents();
    }

    @Test
    public void announcesTheSessionAndStartsTheRussianRecognizer() {
        String sessionId = UUID.randomUUID().toString();
        WebVoiceActivity activity = Robolectric.buildActivity(WebVoiceActivity.class, launch(sessionId)).setup().get();

        Intent started = onlyBroadcast(InputContract.ACTION_WEB_VOICE_STARTED);
        assertEquals(packageName, started.getPackage());
        assertEquals(sessionId, started.getStringExtra(InputContract.EXTRA_SESSION_ID));

        ShadowActivity.IntentForResult recognizer = shadowOf(activity).getNextStartedActivityForResult();
        assertNotNull(recognizer);
        assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, recognizer.intent.getAction());
        assertEquals(InputContract.FUTO_PACKAGE, recognizer.intent.getPackage());
        assertEquals(InputContract.RUSSIAN_LANGUAGE, recognizer.intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE));
        assertFalse(activity.isFinishing());
    }

    @Test
    public void publishesTheFirstNonBlankResultPackageScopedAndFinishes() {
        String sessionId = UUID.randomUUID().toString();
        WebVoiceActivity activity = Robolectric.buildActivity(WebVoiceActivity.class, launch(sessionId)).setup().get();
        ShadowActivity.IntentForResult recognizer = shadowOf(activity).getNextStartedActivityForResult();

        Intent data = new Intent().putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS,
                new ArrayList<>(Arrays.asList("  ", " матрица ", "другое")));
        shadowOf(activity).receiveResult(recognizer.intent, Activity.RESULT_OK, data);

        Intent result = onlyBroadcast(InputContract.ACTION_WEB_VOICE_RESULT);
        assertEquals(packageName, result.getPackage());
        assertEquals(sessionId, result.getStringExtra(InputContract.EXTRA_SESSION_ID));
        assertEquals("матрица", result.getStringExtra(InputContract.EXTRA_RESULT));
        assertTrue(activity.isFinishing());
    }

    @Test
    public void cancelledRecognitionPublishesAnEmptyResultSoTheHostReleasesTheSession() {
        String sessionId = UUID.randomUUID().toString();
        WebVoiceActivity activity = Robolectric.buildActivity(WebVoiceActivity.class, launch(sessionId)).setup().get();
        ShadowActivity.IntentForResult recognizer = shadowOf(activity).getNextStartedActivityForResult();

        shadowOf(activity).receiveResult(recognizer.intent, Activity.RESULT_CANCELED, null);

        Intent result = onlyBroadcast(InputContract.ACTION_WEB_VOICE_RESULT);
        assertEquals(sessionId, result.getStringExtra(InputContract.EXTRA_SESSION_ID));
        assertEquals("", result.getStringExtra(InputContract.EXTRA_RESULT));
        assertTrue(activity.isFinishing());
    }

    @Test
    public void refusesMalformedSessionIdsWithoutTouchingTheRecognizer() {
        WebVoiceActivity activity = Robolectric.buildActivity(WebVoiceActivity.class, launch("not-a-uuid")).setup().get();

        assertTrue(activity.isFinishing());
        assertNull(shadowOf(activity).getNextStartedActivityForResult());
        assertTrue(broadcasts().isEmpty());
    }

    @Test
    public void refusesForeignActionsEvenWithAValidSession() {
        Intent intent = launch(UUID.randomUUID().toString()).setAction(Intent.ACTION_VIEW);
        WebVoiceActivity activity = Robolectric.buildActivity(WebVoiceActivity.class, intent).setup().get();

        assertTrue(activity.isFinishing());
        assertNull(shadowOf(activity).getNextStartedActivityForResult());
        assertTrue(broadcasts().isEmpty());
    }

    private Intent launch(String sessionId) {
        return new Intent(InputContract.ACTION_WEB_VOICE)
                .setPackage(packageName)
                .putExtra(InputContract.EXTRA_SESSION_ID, sessionId)
                .putExtra(InputContract.EXTRA_ORIGIN_PACKAGE, packageName);
    }

    private List<Intent> broadcasts() {
        return shadowOf(application).getBroadcastIntents();
    }

    private Intent onlyBroadcast(String action) {
        Intent match = null;
        for (Intent intent : broadcasts()) {
            if (action.equals(intent.getAction())) {
                assertNull("expected a single " + action, match);
                match = intent;
            }
        }
        assertNotNull("expected broadcast " + action, match);
        return match;
    }
}
