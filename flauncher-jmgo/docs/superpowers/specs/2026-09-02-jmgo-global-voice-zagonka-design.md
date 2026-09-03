# JMGO P5: global Russian voice input and Zagonka fullscreen

## Goal

Provide a completely free solution where remote microphone key code `609` starts Russian offline speech recognition and inserts the result into the currently focused search field. Fix Zagonka video fullscreen behavior for D-pad navigation.

## Components

### Global key capture

Use the open-source Key Mapper accessibility service to capture JMGO key code `609` outside the launcher. Configure it through ADB so the mapping survives reboot. Keep the JMGO Chinese voice package disabled.

If the JMGO firmware removes the third-party accessibility service, use a minimal project-owned accessibility service with `canRequestFilterKeyEvents=true` as a compatible fallback. Only one mapping service may be active at once.

### Russian speech bridge

FUTO Voice Input remains the offline Russian recognizer. A small bridge activity records the originating package, launches FUTO with `ru-RU`, receives `EXTRA_RESULTS`, returns to the originating app, and asks the accessibility service to insert the normalized first result into the focused editable node using `ACTION_SET_TEXT`.

The service searches for an editable focused node first, then a visible editable node. It must never type into password fields. If no editable node exists, it shows a short Russian error and leaves the user in the original app. App-specific adapters may click a visible Search button after insertion for SmartTube, VK Video, Kinopoisk, and Zagonka; otherwise the text remains in the field for manual confirmation.

### Zagonka fullscreen

The Zagonka WebView wrapper implements `WebChromeClient.onShowCustomView` and `onHideCustomView`. Fullscreen video replaces the WebView, hides system UI, receives D-pad focus, and exits on Back. Outside video fullscreen, Back navigates WebView history and then exits the app. The wrapper preserves cookies, DOM storage, media playback, and the existing URL allowlist.

## Permissions and privacy

The solution uses only free/open-source components and local code. Accessibility access is limited to key capture, focused editable-node discovery, text insertion, and optional Search-button activation. Recognized text is not logged or sent by the bridge. FUTO remains offline.

## Failure handling

- If FUTO is missing, show a Russian message and do not change focus.
- If accessibility is disabled, the launcher-local microphone handler remains available and a message explains how to restore global mode.
- If firmware disables Key Mapper, switch to the project-owned fallback service rather than enabling both.
- If an app exposes no editable accessibility node, report that the app does not support automatic insertion.

## Verification

- Unit-test key-code filtering, result normalization, editable-node selection, and fullscreen state transitions.
- Verify key `609` from launcher, SmartTube, VK Video, Kinopoisk, and Zagonka.
- In each application, focus its search field, speak Russian, confirm inserted text, and submit search.
- In Zagonka, start a video, enter fullscreen, navigate with D-pad, exit with Back, and verify WebView history still works.
- Reboot the projector and confirm the launcher, accessibility mapping, FUTO input method, and app tiles persist.
