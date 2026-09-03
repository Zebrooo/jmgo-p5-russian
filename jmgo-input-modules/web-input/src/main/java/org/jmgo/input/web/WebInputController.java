package org.jmgo.input.web;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.io.IOException;
import java.util.UUID;
import org.jmgo.input.core.InputContract;
import org.jmgo.input.core.MicrophoneKeyPolicy;
import org.jmgo.input.core.VoiceSessionGate;

public final class WebInputController {
    private final Activity activity;
    private final FrameLayout root;
    private final WebView webView;
    private final WebSiteAdapter siteAdapter;
    private final CursorState cursorState;
    private final VoiceSessionGate voiceSession = new VoiceSessionGate(InputContract.DEFAULT_SESSION_TIMEOUT_MS);
    private final RemoteCursorView cursorView;
    private final TvKeyboardView keyboardView;
    private boolean attached;
    private boolean resumed;
    private boolean fullscreen;
    private boolean editableFocused;
    private boolean receiverRegistered;

    private final BroadcastReceiver voiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String sessionId = intent.getStringExtra(InputContract.EXTRA_SESSION_ID);
            long now = SystemClock.elapsedRealtime();
            if (InputContract.ACTION_WEB_VOICE_STARTED.equals(intent.getAction())) {
                voiceSession.start(sessionId, now);
                return;
            }
            if (!InputContract.ACTION_WEB_VOICE_RESULT.equals(intent.getAction())
                    || !voiceSession.accept(sessionId, now)) return;
            String result = intent.getStringExtra(InputContract.EXTRA_RESULT);
            if (result == null || result.trim().isEmpty()) return;
            webView.evaluateJavascript(WebDomScripts.hasSafeActiveElement(), value -> {
                if ("true".equals(value)) webView.evaluateJavascript(siteAdapter.insertScript(result.trim()), null);
                else Toast.makeText(activity, "Активное поле поиска потеряно", Toast.LENGTH_SHORT).show();
            });
        }
    };

    public WebInputController(
            Activity activity,
            FrameLayout root,
            WebView webView,
            WebSiteAdapter siteAdapter
    ) {
        this.activity = activity;
        this.root = root;
        this.webView = webView;
        this.siteAdapter = siteAdapter == null ? new DefaultWebSiteAdapter() : siteAdapter;
        this.cursorState = new CursorState(1920, 1080);
        this.cursorView = new RemoteCursorView(activity);
        this.keyboardView = new TvKeyboardView(activity);
    }

    public void attach() {
        if (attached) return;
        attached = true;
        cursorView.setVisibility(View.GONE);
        root.addView(cursorView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        keyboardView.setVisibility(View.GONE);
        keyboardView.setListener(new TvKeyboardView.Listener() {
            @Override public void onText(String text) {
                webView.evaluateJavascript(siteAdapter.insertScript(text), null);
            }
            @Override public void onBackspace() {
                webView.evaluateJavascript(siteAdapter.backspaceScript(), null);
            }
            @Override public void onHide() { hideKeyboard(); }
            @Override public void onSubmit() {
                webView.evaluateJavascript(siteAdapter.submitScript(), null);
                hideKeyboard();
            }
        });
        FrameLayout.LayoutParams keyboardParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(412), Gravity.BOTTOM);
        root.addView(keyboardView, keyboardParams);
        webView.addJavascriptInterface(new JsBridge(), "JmgoWebBridge");
        registerVoiceReceiver();
        cursorView.post(() -> {
            syncCursorBounds();
            cursorView.moveTo(cursorState.x(), cursorState.y());
        });
    }

    public void onPageFinished() {
        if (!attached) return;
        voiceSession.clear();
        editableFocused = false;
        hideKeyboard();
        try {
            webView.evaluateJavascript(WebDomScripts.runtime(activity), ignored ->
                    webView.evaluateJavascript(WebDomScripts.install(), null));
        } catch (IOException error) {
            Toast.makeText(activity, "Не удалось загрузить модуль ввода", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean handleKeyEvent(KeyEvent event) {
        boolean cursorVisible = cursorState.isEnabled() && keyboardView.getVisibility() != View.VISIBLE;
        boolean keyboardVisible = keyboardView.getVisibility() == View.VISIBLE;
        if (!WebKeyPolicy.shouldHandle(
                event.getKeyCode(), event.getAction(), event.getRepeatCount(), resumed,
                cursorVisible, keyboardVisible, fullscreen)) return false;

        if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                cursorState.toggle();
                updateCursorVisibility();
                Toast.makeText(activity,
                        cursorState.isEnabled() ? "Мышь включена" : "Обычная навигация",
                        Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        if (MicrophoneKeyPolicy.shouldHandle(event.getKeyCode(), event.getAction(), event.getRepeatCount())) {
            startVoiceRecognition();
            return true;
        }

        if (keyboardVisible) return keyboardView.handleKeyEvent(event);
        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
        CursorState.Direction direction = cursorDirection(event.getKeyCode());
        if (direction != null) {
            syncCursorBounds();
            cursorState.move(direction, event.getRepeatCount());
            cursorView.moveTo(cursorState.x(), cursorState.y());
            scrollAtEdge(direction);
        } else if ((event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER
                || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                && event.getRepeatCount() == 0) {
            clickAtCursor();
        }
        return true;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        if (fullscreen) hideKeyboard();
        updateCursorVisibility();
    }

    public void onResume() { resumed = true; }

    public void onPause() { resumed = false; }

    public void destroy() {
        resumed = false;
        voiceSession.clear();
        if (receiverRegistered) {
            activity.unregisterReceiver(voiceReceiver);
            receiverRegistered = false;
        }
        if (attached) {
            webView.removeJavascriptInterface("JmgoWebBridge");
            root.removeView(cursorView);
            root.removeView(keyboardView);
            attached = false;
        }
    }

    private void registerVoiceReceiver() {
        IntentFilter filter = new IntentFilter(InputContract.ACTION_WEB_VOICE_STARTED);
        filter.addAction(InputContract.ACTION_WEB_VOICE_RESULT);
        if (Build.VERSION.SDK_INT >= 33) activity.registerReceiver(voiceReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else activity.registerReceiver(voiceReceiver, filter);
        receiverRegistered = true;
    }

    private void startVoiceRecognition() {
        long now = SystemClock.elapsedRealtime();
        if (voiceSession.isActive(now)) {
            activity.sendBroadcast(new Intent(InputContract.ACTION_FINISH_RECOGNITION)
                    .setPackage(InputContract.FUTO_PACKAGE));
            return;
        }
        if (!editableFocused) {
            Toast.makeText(activity, "Сначала выберите поле поиска", Toast.LENGTH_SHORT).show();
            return;
        }
        String sessionId = UUID.randomUUID().toString();
        if (!voiceSession.start(sessionId, now)) return;
        Intent intent = new Intent(InputContract.ACTION_WEB_VOICE)
                .setPackage(activity.getPackageName())
                .putExtra(InputContract.EXTRA_SESSION_ID, sessionId)
                .putExtra(InputContract.EXTRA_ORIGIN_PACKAGE, activity.getPackageName());
        try {
            activity.startActivity(intent);
        } catch (RuntimeException error) {
            voiceSession.clear();
            Toast.makeText(activity, "Голосовой ввод недоступен", Toast.LENGTH_SHORT).show();
        }
    }

    private void showKeyboard() {
        if (!attached || fullscreen) return;
        hideSystemKeyboard();
        keyboardView.setVisibility(View.VISIBLE);
        cursorView.setVisibility(View.GONE);
    }

    private void hideKeyboard() {
        keyboardView.setVisibility(View.GONE);
        updateCursorVisibility();
    }

    private void hideSystemKeyboard() {
        InputMethodManager manager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(webView.getWindowToken(), 0);
    }

    private void updateCursorVisibility() {
        boolean visible = attached && resumed && !fullscreen && cursorState.isEnabled()
                && keyboardView.getVisibility() != View.VISIBLE;
        cursorView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void syncCursorBounds() {
        cursorState.setBounds(Math.max(cursorView.getWidth(), 1), Math.max(cursorView.getHeight(), 1));
    }

    private void scrollAtEdge(CursorState.Direction direction) {
        if (direction == CursorState.Direction.DOWN
                && cursorState.y() >= cursorView.getHeight() - CursorState.EDGE_PADDING) {
            webView.scrollBy(0, dp(90));
        } else if (direction == CursorState.Direction.UP
                && cursorState.y() <= CursorState.EDGE_PADDING) {
            webView.scrollBy(0, -dp(90));
        }
    }

    private void clickAtCursor() {
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN,
                cursorState.x(), cursorState.y(), 0);
        MotionEvent up = MotionEvent.obtain(now, now + 40, MotionEvent.ACTION_UP,
                cursorState.x(), cursorState.y(), 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        webView.dispatchTouchEvent(down);
        webView.dispatchTouchEvent(up);
        down.recycle();
        up.recycle();
    }

    private CursorState.Direction cursorDirection(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: return CursorState.Direction.LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return CursorState.Direction.RIGHT;
            case KeyEvent.KEYCODE_DPAD_UP: return CursorState.Direction.UP;
            case KeyEvent.KEYCODE_DPAD_DOWN: return CursorState.Direction.DOWN;
            default: return null;
        }
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private final class JsBridge {
        @JavascriptInterface
        public void onEditableFocus(boolean focused) {
            activity.runOnUiThread(() -> {
                editableFocused = focused;
                if (focused) showKeyboard();
                else hideKeyboard();
            });
        }
    }
}
