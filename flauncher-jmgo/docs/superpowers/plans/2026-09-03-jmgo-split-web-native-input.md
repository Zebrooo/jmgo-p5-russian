# JMGO Split Web and Native Input Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build reusable WebView input modules and a microphone-only native-app bridge so third-party applications retain their native UI and navigation.

**Architecture:** A Java 8 `input-core` module owns platform-neutral policies and contracts; an Android `web-input` module owns WebView cursor, keyboard, DOM, and web recognition. Zagonka consumes both shared modules, while FLauncher consumes only `input-core` and hosts the global accessibility/capture bridge.

**Tech Stack:** Java 8, Android SDK 33, Android WebView, Android AccessibilityService, Kotlin 1.6.10, JUnit 4, Node.js built-in test runner, Gradle 7.4/Android Gradle Plugin 7.1.1.

**Spec:** `docs/superpowers/specs/2026-09-03-jmgo-split-web-native-input-design.md`

## Global Constraints

- Do not run ADB, connect to the projector, install APKs, or change device settings.
- FUTO remains the offline recognizer and every recognition intent uses `ru-RU`.
- No native package allowlist may determine web/native routing.
- Every non-microphone native key must pass through unchanged.
- Password fields, recognized text, node text, and field contents must never be logged or persisted.
- Result broadcasts must be explicit/package-scoped and validated by an unguessable session identifier.

---

### Task 1: Shared input-core policies

**Files:**
- Create: `/Users/dmitrii/projects/jmgo-input-modules/input-core/build.gradle`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/input-core/src/main/java/org/jmgo/input/core/InputContract.java`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/input-core/src/main/java/org/jmgo/input/core/MicrophoneKeyPolicy.java`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/input-core/src/main/java/org/jmgo/input/core/VoiceResult.java`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/input-core/src/main/java/org/jmgo/input/core/VoiceSessionGate.java`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/input-core/src/main/java/org/jmgo/input/core/EditableCandidate.java`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/input-core/src/main/java/org/jmgo/input/core/EditableTargetPolicy.java`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/input-core/src/main/java/org/jmgo/input/core/VoiceRoute.java`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/input-core/src/main/java/org/jmgo/input/core/VoiceRoutePolicy.java`
- Test: `/Users/dmitrii/projects/jmgo-input-modules/input-core/src/test/java/org/jmgo/input/core/InputCoreTest.java`

**Interfaces:**
- Produces: `MicrophoneKeyPolicy.shouldHandle(int keyCode, int action, int repeatCount)`.
- Produces: `VoiceResult.firstNonBlank(List<String>)`.
- Produces: `VoiceSessionGate.start(String id, long now)`, `accept(String id, long now)`, `isActive(long now)`, and `clear()` with a 60-second constructor timeout.
- Produces: `EditableTargetPolicy.select(List<EditableCandidate>, String originPackage)` returning a candidate index or `-1`.
- Produces: `VoiceRoutePolicy.select(boolean hasForegroundPackage, boolean resolvesWebVoice)`.

- [ ] **Step 1: Add failing core tests**

Write JUnit tests requiring key `609`/down/repeat-zero, first trimmed nonblank recognition text, duplicate/expired session rejection, focused-safe-field preference, password rejection, origin-package matching, and capability-based WEB/NATIVE/NONE routing.

- [ ] **Step 2: Run the core tests and verify RED**

Temporarily include the module from Zagonka settings and run:

```bash
gradle -p /Users/dmitrii/projects/zagonka-tv-wrapper :input-core:test
```

Expected: compilation fails because the production policy classes do not exist.

- [ ] **Step 3: Implement the minimal Java policies**

Use immutable value objects, no Android imports, constant `MICROPHONE_KEY_CODE = 609`, action-down primitive value `0`, and strict package equality in editable selection.

- [ ] **Step 4: Run the focused tests and verify GREEN**

Run `gradle -p /Users/dmitrii/projects/zagonka-tv-wrapper :input-core:test` and require zero failures.

### Task 2: Generic DOM, keyboard, and cursor models

**Files:**
- Create: `/Users/dmitrii/projects/jmgo-input-modules/web-input/build.gradle`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/web-input/src/main/AndroidManifest.xml`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/web-input/src/main/assets/jmgo-web-input.js`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/web-input/src/main/java/org/jmgo/input/web/WebDomScripts.java`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/web-input/src/main/java/org/jmgo/input/web/KeyboardModel.java`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/web-input/src/main/java/org/jmgo/input/web/CursorState.java`
- Test: `/Users/dmitrii/projects/jmgo-input-modules/web-input/src/test/java/org/jmgo/input/web/KeyboardModelTest.java`
- Test: `/Users/dmitrii/projects/jmgo-input-modules/web-input/src/test/java/org/jmgo/input/web/CursorStateTest.java`
- Test: `/Users/dmitrii/projects/jmgo-input-modules/web-input/src/test/js/jmgo-web-input.test.js`

**Interfaces:**
- Produces: `window.JmgoWebInput.install()`, `insert(text)`, `backspace()`, `submit()`, and `hasSafeActiveElement()`.
- Produces: `KeyboardModel` with `RUSSIAN`, `ENGLISH`, `NUMBERS`, four navigation rows, and actions LANGUAGE/NUMBERS/SPACE/BACKSPACE/HIDE/SUBMIT.
- Produces: `CursorState.toggle()`, `move(Direction, repeatCount)`, `setBounds(width,height)`, and immutable coordinates.

- [ ] **Step 1: Add failing Java model tests**

Require the three layouts, proportional vertical navigation, six bottom actions, cursor-disabled default, 36-pixel base movement, repeat acceleration, and 18-pixel edge clamping.

- [ ] **Step 2: Add failing JavaScript behavior tests**

Using Node's built-in `node:test` and a minimal fake DOM, require rejection of password/disabled inputs, native-setter input insertion, content-editable insertion, `requestSubmit`, submit-control fallback, and Enter fallback.

- [ ] **Step 3: Run both suites and verify RED**

Run:

```bash
gradle -p /Users/dmitrii/projects/zagonka-tv-wrapper :web-input:test
node --test /Users/dmitrii/projects/jmgo-input-modules/web-input/src/test/js/jmgo-web-input.test.js
```

Expected: missing Java classes and missing `jmgo-web-input.js` behavior.

- [ ] **Step 4: Implement models and DOM runtime**

Keep DOM detection semantic rather than domain-based. Dispatch `beforeinput`, `input`, and `change`; escape all Android-to-JavaScript text with JSON-compatible quoting in `WebDomScripts`.

- [ ] **Step 5: Run Java and JavaScript tests and verify GREEN**

Repeat both focused commands and require zero failures.

### Task 3: Reusable Android WebInputController

**Files:**
- Create: `/Users/dmitrii/projects/jmgo-input-modules/web-input/src/main/java/org/jmgo/input/web/WebSiteAdapter.java`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/web-input/src/main/java/org/jmgo/input/web/DefaultWebSiteAdapter.java`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/web-input/src/main/java/org/jmgo/input/web/RemoteCursorView.java`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/web-input/src/main/java/org/jmgo/input/web/TvKeyboardView.java`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/web-input/src/main/java/org/jmgo/input/web/WebVoiceActivity.java`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/web-input/src/main/java/org/jmgo/input/web/WebInputController.java`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/web-input/src/main/res/values/styles.xml`
- Test: `/Users/dmitrii/projects/jmgo-input-modules/web-input/src/test/java/org/jmgo/input/web/WebKeyPolicyTest.java`
- Create: `/Users/dmitrii/projects/jmgo-input-modules/web-input/src/main/java/org/jmgo/input/web/WebKeyPolicy.java`

**Interfaces:**
- Produces: `new WebInputController(Activity, FrameLayout, WebView, WebSiteAdapter)` and lifecycle methods from the spec.
- Produces: `WebSiteAdapter.focusScript()`, `insertScript(String)`, `backspaceScript()`, and `submitScript()`.
- Consumes: FUTO activity package `org.futo.voiceinput.jmgo` and `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`.

- [ ] **Step 1: Add a failing ownership-policy test**

Require Menu ownership outside fullscreen, D-pad/center ownership only while cursor or keyboard is visible, microphone ownership when the host is resumed, and `false` for all unrelated keys.

- [ ] **Step 2: Run the policy test and verify RED**

Run `gradle -p /Users/dmitrii/projects/zagonka-tv-wrapper :web-input:test --tests org.jmgo.input.web.WebKeyPolicyTest`; expect the missing policy class.

- [ ] **Step 3: Implement views, controller, and recognition activity**

Render the keyboard with standard Android `LinearLayout`/`Button` views, not an IME. Register a bridge named `JmgoWebBridge`, hide the system IME after safe DOM focus, use UUID session IDs, scope result broadcasts to the host package, and discard results when paused, expired, or no safe DOM field remains.

- [ ] **Step 4: Declare the capability activity**

The library manifest declares `WebVoiceActivity`, exported `true`, with an intent filter for `org.jmgo.input.action.WEB_VOICE` and a transparent dialog theme. It accepts only explicit/session-bearing launches and always invokes FUTO with `ru-RU`.

- [ ] **Step 5: Run tests and assemble both AARs**

Run `gradle -p /Users/dmitrii/projects/zagonka-tv-wrapper :input-core:test :web-input:test :input-core:assemble :web-input:assemble` and require success.

### Task 4: Migrate Zagonka to the shared web module

**Files:**
- Modify: `/Users/dmitrii/projects/zagonka-tv-wrapper/settings.gradle`
- Modify: `/Users/dmitrii/projects/zagonka-tv-wrapper/app/build.gradle`
- Modify: `/Users/dmitrii/projects/zagonka-tv-wrapper/app/src/main/java/ru/zagonka/tv/wrapper/MainActivity.java`
- Create: `/Users/dmitrii/projects/zagonka-tv-wrapper/app/src/main/java/ru/zagonka/tv/wrapper/ZagonkaSiteAdapter.java`
- Test: `/Users/dmitrii/projects/zagonka-tv-wrapper/app/src/test/java/ru/zagonka/tv/wrapper/ZagonkaSiteAdapterTest.java`
- Delete after migration: Zagonka-local cursor, mouse, keyboard-signal, and voice-session classes that are no longer referenced.

**Interfaces:**
- Consumes: `WebInputController` and `WebSiteAdapter` from Task 3.
- Preserves: URL allowlist, WebView settings, cookies/DOM storage, fullscreen custom view, Back behavior, and the Zagonka autocomplete first-result script.

- [ ] **Step 1: Add a failing adapter-isolation test**

Require the Zagonka adapter's submit script to contain the autocomplete selector while `DefaultWebSiteAdapter.submitScript()` does not.

- [ ] **Step 2: Run the focused test and verify RED**

Run `gradle -p /Users/dmitrii/projects/zagonka-tv-wrapper :app:testDebugUnitTest --tests ru.zagonka.tv.wrapper.ZagonkaSiteAdapterTest`; expect the missing adapter.

- [ ] **Step 3: Integrate the controller**

Construct and attach it after adding the WebView, call `onPageFinished`, delegate `dispatchKeyEvent`, notify fullscreen state, and forward lifecycle methods. Delete only now-unreferenced local input classes; retain site and fullscreen policies.

- [ ] **Step 4: Run all Zagonka tests and build**

Run `gradle -p /Users/dmitrii/projects/zagonka-tv-wrapper testDebugUnitTest assembleDebug` and require success.

### Task 5: Add FLauncher microphone-only native bridge

**Files:**
- Modify: `/Users/dmitrii/projects/flauncher-jmgo/android/settings.gradle`
- Modify: `/Users/dmitrii/projects/flauncher-jmgo/android/app/build.gradle`
- Modify: `/Users/dmitrii/projects/flauncher-jmgo/android/app/src/main/AndroidManifest.xml`
- Modify: `/Users/dmitrii/projects/flauncher-jmgo/android/app/src/main/kotlin/me/efesser/flauncher/MainActivity.kt`
- Modify: `/Users/dmitrii/projects/flauncher-jmgo/android/app/src/main/kotlin/me/efesser/flauncher/VoiceKey.kt`
- Create: `/Users/dmitrii/projects/flauncher-jmgo/android/app/src/main/kotlin/me/efesser/flauncher/voice/NativeVoiceAccessibilityService.kt`
- Create: `/Users/dmitrii/projects/flauncher-jmgo/android/app/src/main/kotlin/me/efesser/flauncher/voice/NativeVoiceCaptureActivity.kt`
- Create: `/Users/dmitrii/projects/flauncher-jmgo/android/app/src/main/kotlin/me/efesser/flauncher/voice/AndroidEditableTarget.kt`
- Create: `/Users/dmitrii/projects/flauncher-jmgo/android/app/src/main/kotlin/me/efesser/flauncher/voice/WebCapabilityResolver.kt`
- Create: `/Users/dmitrii/projects/flauncher-jmgo/android/app/src/main/res/xml/native_voice_accessibility_service.xml`
- Create: `/Users/dmitrii/projects/flauncher-jmgo/android/app/src/main/res/values/voice_strings.xml`
- Test: `/Users/dmitrii/projects/flauncher-jmgo/android/app/src/test/kotlin/me/efesser/flauncher/voice/NativeVoicePolicyTest.kt`

**Interfaces:**
- Consumes: Task 1 core policies and the manifest capability action.
- Produces: accessibility service `.voice.NativeVoiceAccessibilityService` with `canRequestFilterKeyEvents=true` and `FLAG_REQUEST_FILTER_KEY_EVENTS`.
- Produces: package-scoped `InputContract.ACTION_NATIVE_VOICE_RESULT` with session ID and normalized result.

- [ ] **Step 1: Add failing bridge-policy tests**

Require microphone-only consumption, WEB routing when the origin package resolves the capability activity, NATIVE routing otherwise, same-package result validation, safe field preference, and expired-session rejection.

- [ ] **Step 2: Run the focused tests and verify RED**

Run from `/Users/dmitrii/projects/flauncher-jmgo/android`:

```bash
./gradlew app:testDebugUnitTest --tests 'me.efesser.flauncher.voice.NativeVoicePolicyTest'
```

Expected: missing bridge classes.

- [ ] **Step 3: Implement capture and insertion**

Filter only initial key `609` down. Resolve web capability in the foreground package; otherwise launch the transparent native capture activity. After FUTO returns, reacquire the root, flatten candidates without recording node text, apply `ACTION_SET_TEXT`, and call advertised `ACTION_IME_ENTER` on API 30+.

- [ ] **Step 4: Add manifest service configuration and launcher fallback**

Declare the accessibility service as non-exported with `BIND_ACCESSIBILITY_SERVICE`. Change FLauncher's local microphone handler to launch `NativeVoiceCaptureActivity` instead of Zagonka's activity. Do not enable the service in code or settings.

- [ ] **Step 5: Run Android unit tests and APK build**

Run `./gradlew app:testDebugUnitTest app:assembleDebug` from `android` and require success.

### Task 6: Remove the obsolete package-specific IME coupling and verify all artifacts

**Files:**
- Modify: `/Users/dmitrii/projects/futo-voice-jmgo/app/src/main/java/org/futo/voiceinput/JmgoVoiceSignal.kt`
- Modify: `/Users/dmitrii/projects/futo-voice-jmgo/app/src/main/java/org/futo/voiceinput/VoiceInputMethodService.kt`
- Modify: `/Users/dmitrii/projects/futo-voice-jmgo/app/src/test/java/org/futo/voiceinput/JmgoVoiceSignalTest.kt`

**Interfaces:**
- Removes: `ZAGONKA_PACKAGE`, `ACTION_SUBMIT_SEARCH`, and `usesExplicitController()`.
- Preserves: exported FUTO `RecognizeActivity`, `ru-RU` recognition defaults, and legacy IME behavior without package-specific routing.

- [ ] **Step 1: Change the FUTO test to require package-neutral behavior**

Delete the Zagonka explicit-controller expectation and add an assertion that microphone filtering remains package-independent.

- [ ] **Step 2: Run the focused FUTO test before production edits**

Run `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew testPlayStoreDebugUnitTest --tests org.futo.voiceinput.JmgoVoiceSignalTest`; expect compilation or assertion failure while the obsolete API remains required by production code.

- [ ] **Step 3: Remove package-specific branches**

Stop registering or sending `ACTION_SUBMIT_SEARCH`; treat the system microphone signal uniformly inside the legacy IME. Do not remove `RecognizeActivity` or the offline recognizer.

- [ ] **Step 4: Run the complete local verification matrix**

Run, without ADB:

```bash
gradle -p /Users/dmitrii/projects/zagonka-tv-wrapper :input-core:test :web-input:test :app:testDebugUnitTest :input-core:assemble :web-input:assemble :app:assembleDebug
cd /Users/dmitrii/projects/flauncher-jmgo/android && ./gradlew app:testDebugUnitTest app:assembleDebug
cd /Users/dmitrii/projects/futo-voice-jmgo && JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew testPlayStoreDebugUnitTest assemblePlayStoreDebug
node --test /Users/dmitrii/projects/jmgo-input-modules/web-input/src/test/js/jmgo-web-input.test.js
```

Require zero test failures and successful AAR/APK assembly. Do not install any resulting artifact.
