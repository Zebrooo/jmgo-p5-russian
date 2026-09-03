# JMGO Split Web and Native Input Design

## Goal

Provide one reusable input implementation for Android `WebView` hosts while keeping third-party native applications on their own navigation and UI. In native applications, the only globally replaced behavior is the JMGO microphone key: key code `609` starts Russian FUTO recognition and inserts the result into the currently focused non-password field.

This phase changes and verifies source code only. It must not connect to the projector, run ADB, install APKs, or change device settings.

## Project layout

Create a shared Gradle project at `/Users/dmitrii/projects/jmgo-input-modules` with two modules:

- `input-core`: a Java 8 library with no Android UI. It owns microphone-key filtering, single-session state, result normalization, web/native routing contracts, and safe-field selection models.
- `web-input`: an Android Java library depending on `input-core`. It owns the WebView controller, remote cursor, TV keyboard, generic DOM scripts, and the web recognition activity.

Both modules use Java APIs consumable from the existing Java Zagonka wrapper and Kotlin FLauncher project. Zagonka includes both shared modules through Gradle project directories. FLauncher includes `input-core`; it does not depend on `web-input`.

FUTO remains the offline Russian speech-recognition provider exposed through `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`. The new architecture does not add another speech engine.

## Capability-based routing

Routing must not use a hard-coded website or native-application list.

Every application that embeds `web-input` receives a manifest-declared activity for the shared action `org.jmgo.input.action.WEB_VOICE`. The global accessibility service checks whether the foreground package resolves this action:

- If the action resolves inside the foreground package, that package is a web host. The service launches the resolved web voice activity and does not inspect or modify the native accessibility tree.
- If it does not resolve, the foreground package is treated as native. The service launches FLauncher's native voice-capture activity and later inserts the result through the accessibility tree.

This capability check makes future WebView wrappers work without changing a package allowlist. Native applications such as VK Video and SmartTube do not declare the action and therefore never load the web cursor or keyboard.

## Shared core

`input-core` exposes focused, deterministic policies:

- `MicrophoneKeyPolicy` accepts only key code `609`, `ACTION_DOWN`, and repeat count `0`.
- `VoiceSessionGate` permits one recognition request at a time and clears on result, cancellation, or timeout.
- `VoiceResult` returns the first trimmed nonblank recognition result and never logs recognized text.
- `EditableCandidate` and `EditableTargetPolicy` prefer a visible focused editable node, allow a visible editable fallback in the same originating package, and always reject password nodes.
- `VoiceRoute` represents `WEB`, `NATIVE`, or `NONE`; Android-specific resolver code supplies the capability facts used to select the route.

Core policies accept primitive values and plain data objects so local JVM tests do not require an emulator.

## Web input module

### Host API

`WebInputController` is attached by a WebView activity using an `Activity`, root `FrameLayout`, and `WebView`. Its public lifecycle is:

- `attach()` to add the cursor and keyboard overlays and install the JavaScript bridge;
- `onPageFinished()` to inject the generic editable-element listener;
- `handleKeyEvent(KeyEvent)` to process the menu toggle, D-pad cursor movement, keyboard navigation, center click, and microphone fallback;
- `onResume()`, `onPause()`, and `destroy()` to register or release receivers and views.

The controller returns `false` for every event it does not own. It owns navigation only while its cursor or keyboard is visible. This prevents a host from swallowing normal WebView or video-player input.

### DOM behavior

The injected script recognizes visible, enabled:

- `input` elements except password, hidden, checkbox, radio, file, and button types;
- `textarea` elements;
- elements with `contenteditable=true`.

Focus notifications contain no field text. They only tell Android whether a safe editable element is active. Android hides the system IME and shows the library's TV keyboard for that field.

Text insertion uses the element's native value setter when applicable and dispatches bubbling `beforeinput`, `input`, and `change` events. Content-editable elements receive text through a selection-aware DOM operation and the same observable events.

Submission follows this order:

1. Call `requestSubmit()` on the owning form.
2. Click a visible enabled submit/search control belonging to that form.
3. Dispatch Enter keyboard events to the active editable element.

`WebSiteAdapter` may override focus, insertion, or submission scripts for a nonstandard site. The default module contains no Zagonka selectors. The existing Zagonka autocomplete-result behavior becomes a small host-side adapter.

### Web keyboard and cursor

The TV keyboard preserves Russian, English, and numeric layouts, D-pad selection, backspace, space, Search, and `▼ Скрыть`. It is rendered inside the web host rather than by the system input method.

The cursor remains disabled by default and toggles only on the remote Menu button. It is hidden whenever the keyboard or fullscreen video is visible. Entering fullscreen releases all navigation to the video view; Back exits fullscreen before affecting WebView history.

### Web voice flow

The manifest-provided web voice activity launches FUTO with language `ru-RU`. After the transparent activity finishes, it publishes a package-scoped result back to `WebInputController`. A valid result that arrives while the host is paused is held in memory and processed on resume; it is never inserted while paused. The controller then revalidates the session and active safe DOM element, refusing stale results after navigation, loss of focus, or timeout. Cancellation makes no DOM change.

## Native voice bridge

FLauncher owns `NativeVoiceAccessibilityService` and `NativeVoiceCaptureActivity`. The service is exported so Android can bind to it, and is protected by `BIND_ACCESSIBILITY_SERVICE` so only the system can bind.

The accessibility service requests key filtering and consumes only the accepted microphone press. All other key events return `false` unchanged. On an accepted press it records only the originating package and window identity, resolves web capability, and routes as described above.

For a native route, the transparent capture activity launches FUTO with `ru-RU`. When it finishes, Android reveals the originating app again. The service reacquires the active accessibility tree instead of retaining a stale `AccessibilityNodeInfo`.

Insertion proceeds only when all of these remain true:

- the foreground package matches the originating package;
- the session has not timed out;
- a visible editable non-password node is available;
- the result is nonblank.

The service waits briefly for the originating window to return, then calls `ACTION_SET_TEXT`. On Android 11 or newer it calls `ACTION_IME_ENTER` only when the target node advertises that action. Otherwise it leaves the inserted text in the field so the application's native confirmation continues to work. It does not search for or click arbitrary buttons.

While FLauncher itself is foreground, its existing `dispatchKeyEvent` handler remains a fallback for installations where the accessibility service is disabled. It launches the same native capture activity and uses the same session gate, avoiding a second voice implementation.

## System input method boundary

The JMGO FUTO input method is no longer responsible for cursor navigation or the full TV keyboard outside web hosts. Native UI remains owned by the native application and the device's normal keyboard. A later device-deployment step may switch the default system input method without changing this code.

No source-code path automatically switches the default IME, modifies secure settings, or enables accessibility. Those are explicit deployment operations and are outside this phase.

## Failure handling and privacy

- Missing FUTO: show a short Russian message and leave the current field unchanged.
- Missing accessibility permission: the launcher fallback still handles the microphone key while FLauncher is foreground; other native apps remain entirely native.
- Missing or changed target field: discard the result and show a short Russian message.
- Recognition cancellation, blank result, duplicate key-down, or timeout: clear the session without insertion.
- Password fields: always reject before recognition result insertion.
- Recognized text, accessibility-node text, and field contents are never logged or persisted.
- Broadcasts carrying results are explicit and package-scoped. Session identifiers are unpredictable and results with unknown or expired identifiers are rejected.

## Zagonka migration

Zagonka keeps its URL policy, cookies, media settings, fullscreen implementation, and site URL. `MainActivity` delegates input behavior to `WebInputController` and retains only its site-specific `WebSiteAdapter` for autocomplete-result submission.

The old Zagonka-to-FUTO keyboard-navigation broadcasts and the package-specific `usesExplicitController()` path are removed after the shared controller provides equivalent behavior. No native application imports or starts `web-input`.

## Verification

Local verification for this phase includes:

- JVM tests for microphone filtering, session gating, result normalization, route selection, and safe editable-target selection;
- JVM tests for keyboard layout/navigation and cursor state;
- JavaScript tests for generic input, textarea, content-editable insertion, password rejection, form submission, submit-button fallback, and Enter fallback;
- FLauncher Android unit tests for web capability resolution decisions and native result handling;
- Zagonka unit tests proving that unowned keys pass through and its adapter remains isolated;
- successful debug builds of the shared AAR modules, FLauncher APK, Zagonka APK, and the existing FUTO JMGO APK.

Device installation, ADB verification, changing the default input method, and enabling accessibility are explicitly deferred until the user requests projector changes.
