package org.jmgo.input.web;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.UUID;
import org.jmgo.input.core.InputContract;
import org.jmgo.input.core.VoiceResult;

public final class WebVoiceActivity extends Activity {
    private static final int REQUEST_RECOGNITION = 4101;
    private String sessionId;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) sessionId = state.getString(InputContract.EXTRA_SESSION_ID);
        if (sessionId == null) sessionId = validSessionId(getIntent().getStringExtra(InputContract.EXTRA_SESSION_ID));
        if (sessionId == null || !InputContract.ACTION_WEB_VOICE.equals(getIntent().getAction())) {
            finish();
            return;
        }
        if (state == null) {
            broadcast(InputContract.ACTION_WEB_VOICE_STARTED, "");
            launchRecognizer();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(InputContract.EXTRA_SESSION_ID, sessionId);
        super.onSaveInstanceState(outState);
    }

    private void launchRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .setPackage(InputContract.FUTO_PACKAGE)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, InputContract.RUSSIAN_LANGUAGE)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите");
        try {
            startActivityForResult(intent, REQUEST_RECOGNITION);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "Русский голосовой ввод не найден", Toast.LENGTH_SHORT).show();
            publishResult("");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_RECOGNITION) return;
        ArrayList<String> candidates = resultCode == RESULT_OK && data != null
                ? data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) : null;
        publishResult(VoiceResult.firstNonBlank(candidates));
    }

    private void publishResult(String result) {
        // No artificial delay: WebInputController keeps a matching result in memory while the
        // host is paused and applies it from onResume() once the host is back in front.
        sendBroadcast(resultBroadcast(result));
        finish();
    }

    private void broadcast(String action, String result) {
        Intent intent = new Intent(action)
                .setPackage(getPackageName())
                .putExtra(InputContract.EXTRA_SESSION_ID, sessionId);
        if (InputContract.ACTION_WEB_VOICE_RESULT.equals(action)) {
            intent.putExtra(InputContract.EXTRA_RESULT, result);
        }
        sendBroadcast(intent);
    }

    private Intent resultBroadcast(String result) {
        return new Intent(InputContract.ACTION_WEB_VOICE_RESULT)
                .setPackage(getPackageName())
                .putExtra(InputContract.EXTRA_SESSION_ID, sessionId)
                .putExtra(InputContract.EXTRA_RESULT, result);
    }

    private static String validSessionId(String value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value).toString();
        } catch (IllegalArgumentException error) {
            return null;
        }
    }
}
