package ru.zagonka.tv.wrapper;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import org.jmgo.input.web.WebInputController;

public final class MainActivity extends Activity {
    private static final String HOME = "https://www.zagonka-tv.org/";
    private static final int IMMERSIVE_FLAGS = View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

    private WebView webView;
    private FrameLayout root;
    private WebInputController inputController;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private final FullscreenState fullscreenState = new FullscreenState();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        root = new FrameLayout(this);
        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                showFullscreen(view, callback);
            }

            @Override
            public void onHideCustomView() {
                hideFullscreen();
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(TvWebPolicy.viewportScript(), null);
                if (inputController != null) inputController.onPageFinished();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !UrlPolicy.isAllowed(request.getUrl().toString());
            }
        });

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUserAgentString(TvWebPolicy.userAgent());
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        webView.setInitialScale(50);

        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        inputController = new WebInputController(this, root, webView, new ZagonkaSiteAdapter());
        inputController.attach();
        webView.loadUrl(HOME);
        webView.requestFocus();
    }

    private void showFullscreen(View view, WebChromeClient.CustomViewCallback callback) {
        if (!fullscreenState.enter()) {
            callback.onCustomViewHidden();
            return;
        }
        inputController.setFullscreen(true);
        fullscreenView = view;
        fullscreenCallback = callback;
        webView.setVisibility(View.GONE);
        root.addView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        getWindow().getDecorView().setSystemUiVisibility(IMMERSIVE_FLAGS);
    }

    private void hideFullscreen() {
        if (!fullscreenState.exit()) return;
        if (fullscreenView != null) root.removeView(fullscreenView);
        fullscreenView = null;
        webView.setVisibility(View.VISIBLE);
        webView.requestFocus();
        if (fullscreenCallback != null) fullscreenCallback.onCustomViewHidden();
        fullscreenCallback = null;
        inputController.setFullscreen(false);
        getWindow().getDecorView().setSystemUiVisibility(IMMERSIVE_FLAGS);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (inputController != null && inputController.handleKeyEvent(event)) return true;
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (fullscreenState.isFullscreen()) {
                hideFullscreen();
                return true;
            }
            if (webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        if (inputController != null) inputController.onPause();
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        if (inputController != null) inputController.onResume();
    }

    @Override
    protected void onDestroy() {
        hideFullscreen();
        if (inputController != null) inputController.destroy();
        root.removeView(webView);
        webView.destroy();
        super.onDestroy();
    }
}
