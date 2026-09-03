package org.jmgo.input.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import android.webkit.WebView;
import android.widget.FrameLayout;
import java.util.UUID;
import org.jmgo.input.core.InputContract;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

/** Lifecycle contract between {@link WebVoiceActivity} broadcasts and the host controller. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.TIRAMISU)
public final class WebInputControllerTest {
    private ActivityController<Activity> host;
    private Activity activity;
    private WebView webView;
    private WebInputController controller;

    @Before
    public void setUp() {
        host = Robolectric.buildActivity(Activity.class).setup();
        activity = host.get();
        FrameLayout root = new FrameLayout(activity);
        webView = new WebView(activity);
        root.addView(webView);
        activity.setContentView(root);
        controller = new WebInputController(activity, root, webView, null);
        controller.attach();
        controller.onResume();
        idle();
    }

    @After
    public void tearDown() {
        controller.destroy();
    }

    @Test
    public void holdsAResultWhilePausedAndAppliesItToASafeFieldAfterResume() {
        String sessionId = UUID.randomUUID().toString();
        started(sessionId);
        controller.onPause();

        result(sessionId, " матрица ");
        assertNull("nothing may touch the DOM while the host is paused", lastScript());

        controller.onResume();
        assertEquals(WebDomScripts.hasSafeActiveElement(), lastScript());

        shadowOf(webView).getLastEvaluatedJavascriptCallback().onReceiveValue("true");
        assertEquals(WebDomScripts.insert("матрица"), lastScript());
    }

    @Test
    public void appliesAResultImmediatelyWhileResumed() {
        String sessionId = UUID.randomUUID().toString();
        started(sessionId);

        result(sessionId, "кино");
        assertEquals(WebDomScripts.hasSafeActiveElement(), lastScript());
        shadowOf(webView).getLastEvaluatedJavascriptCallback().onReceiveValue("true");

        assertEquals(WebDomScripts.insert("кино"), lastScript());
    }

    @Test
    public void dropsTheResultWhenTheSafeFieldIsGone() {
        String sessionId = UUID.randomUUID().toString();
        started(sessionId);

        result(sessionId, "кино");
        shadowOf(webView).getLastEvaluatedJavascriptCallback().onReceiveValue("false");

        assertEquals(WebDomScripts.hasSafeActiveElement(), lastScript());
    }

    @Test
    public void ignoresResultsForUnknownOrStaleSessions() {
        started(UUID.randomUUID().toString());

        result(UUID.randomUUID().toString(), "чужой результат");
        assertNull(lastScript());

        result(null, "без сессии");
        assertNull(lastScript());
    }

    @Test
    public void acceptsEachSessionOnlyOnce() {
        String sessionId = UUID.randomUUID().toString();
        started(sessionId);
        result(sessionId, "первый");
        shadowOf(webView).getLastEvaluatedJavascriptCallback().onReceiveValue("true");
        assertEquals(WebDomScripts.insert("первый"), lastScript());

        result(sessionId, "повтор");

        assertEquals(WebDomScripts.insert("первый"), lastScript());
    }

    @Test
    public void cancelledSessionReleasesTheGateWithoutTouchingTheDom() {
        String first = UUID.randomUUID().toString();
        started(first);
        result(first, "");
        assertNull(lastScript());

        String second = UUID.randomUUID().toString();
        started(second);
        result(second, "второй");

        assertEquals(WebDomScripts.hasSafeActiveElement(), lastScript());
    }

    @Test
    public void pageNavigationDiscardsAQueuedResult() {
        String sessionId = UUID.randomUUID().toString();
        started(sessionId);
        controller.onPause();
        result(sessionId, "устарело");

        controller.onPageFinished();
        controller.onResume();

        // onPageFinished re-injects the runtime; nothing may probe or insert the stale result.
        String script = lastScript();
        assertNotEquals(WebDomScripts.hasSafeActiveElement(), script);
        assertNotEquals(WebDomScripts.insert("устарело"), script);
        assertFalse(script != null && script.contains("устарело"));
    }

    private void started(String sessionId) {
        Intent intent = new Intent(InputContract.ACTION_WEB_VOICE_STARTED)
                .setPackage(activity.getPackageName())
                .putExtra(InputContract.EXTRA_SESSION_ID, sessionId);
        activity.sendBroadcast(intent);
        idle();
    }

    private void result(String sessionId, String text) {
        Intent intent = new Intent(InputContract.ACTION_WEB_VOICE_RESULT)
                .setPackage(activity.getPackageName())
                .putExtra(InputContract.EXTRA_SESSION_ID, sessionId)
                .putExtra(InputContract.EXTRA_RESULT, text);
        activity.sendBroadcast(intent);
        idle();
    }

    private String lastScript() {
        return shadowOf(webView).getLastEvaluatedJavascript();
    }

    private static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }
}
