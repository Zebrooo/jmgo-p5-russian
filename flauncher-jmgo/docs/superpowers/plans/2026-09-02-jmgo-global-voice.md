# JMGO Global Russian Voice Input Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Capture JMGO microphone key `609` globally and insert FUTO's Russian result into the active search field.

**Architecture:** Install Key Mapper FOSS as the first global key-event provider. Add a focused bridge activity and fallback accessibility service to the existing launcher package; both share a small, unit-tested voice-result protocol. The accessibility service remembers the originating package, launches FUTO, restores the app, sets text only on a non-password editable node, and submits search through per-app adapters.

**Tech Stack:** Android 13, Kotlin, Android AccessibilityService, Activity Result API/RecognizerIntent, JUnit 4, ADB, Key Mapper FOSS, FUTO Voice Input.

**Spec:** `docs/superpowers/specs/2026-09-02-jmgo-global-voice-zagonka-design.md`

## Global Constraints

- All components must be free or open source.
- FUTO recognition must remain offline and use `ru-RU`.
- Recognized text must never be logged or inserted into password fields.
- Only one global key-capture accessibility service may be enabled at once.
- JMGO Chinese voice and Pinyin input services remain disabled.

---

### Task 1: Install and probe Key Mapper FOSS

**Files:**
- Create: `/Users/dmitrii/projects/flauncher-jmgo/docs/jmgo-keymapper-probe.md`

**Interfaces:**
- Consumes: JMGO remote key code `609`.
- Produces: recorded evidence `captures609=true|false` and the installed Key Mapper package/component names.

- [ ] **Step 1: Download the latest signed FOSS APK from the official Key Mapper GitHub release**

Run `curl` against the asset URL returned by the GitHub releases API and save it under a `mktemp -d` directory. Record the release tag and SHA-256 in the probe document.

- [ ] **Step 2: Install and enable the accessibility service**

Run `adb install -r <apk>`, inspect `dumpsys package` for the exact accessibility component, and enable it with `settings put secure enabled_accessibility_services` without removing already approved services.

- [ ] **Step 3: Verify code 609 capture**

Open Key Mapper's trigger recorder, press the physical microphone button, and confirm its accessibility log records `keyCode=609`. If firmware immediately disables the service, record `captures609=false` and use Task 3's fallback.

- [ ] **Step 4: Commit the probe evidence**

Run `git add docs/jmgo-keymapper-probe.md && git commit -m "docs: record JMGO Key Mapper compatibility"`.

### Task 2: Voice result and editable-node selection

**Files:**
- Create: `android/app/src/main/kotlin/me/efesser/flauncher/voice/VoiceResult.kt`
- Create: `android/app/src/main/kotlin/me/efesser/flauncher/voice/EditableNodeSelector.kt`
- Test: `android/app/src/test/kotlin/me/efesser/flauncher/voice/VoiceResultTest.kt`

**Interfaces:**
- Produces: `VoiceResult.normalize(List<String>?): String` and `EditableCandidate(isEditable: Boolean, isPassword: Boolean, isFocused: Boolean, visible: Boolean, depth: Int)` with `EditableNodeSelector.select(List<EditableCandidate>): Int?`.

- [ ] **Step 1: Write failing normalization and node-selection tests**

Test that whitespace is trimmed, the first nonblank result wins, a focused editable visible node is preferred, a visible editable fallback is accepted, and password nodes are always rejected.

- [ ] **Step 2: Run the focused tests and verify RED**

Run `./gradlew app:testDebugUnitTest --tests '*VoiceResultTest'`; expect missing production classes.

- [ ] **Step 3: Implement the pure Kotlin helpers**

Implement deterministic selection with no Android framework types so local JUnit can execute it.

- [ ] **Step 4: Run tests and commit**

Run the focused test plus `app:testDebugUnitTest`, then commit production and test files as `feat: select safe voice input targets`.

### Task 3: Global key-capture fallback service

**Files:**
- Create: `android/app/src/main/kotlin/me/efesser/flauncher/voice/GlobalVoiceAccessibilityService.kt`
- Create: `android/app/src/main/res/xml/global_voice_accessibility_service.xml`
- Modify: `android/app/src/main/AndroidManifest.xml`
- Modify: `android/app/src/main/kotlin/me/efesser/flauncher/VoiceKey.kt`
- Test: `android/app/src/test/kotlin/me/efesser/flauncher/VoiceKeyTest.kt`

**Interfaces:**
- Consumes: `VoiceKey.shouldHandle(609, ACTION_DOWN, 0)`.
- Produces: exported=false accessibility service component `.voice.GlobalVoiceAccessibilityService` with `canRequestFilterKeyEvents=true` and `FLAG_REQUEST_FILTER_KEY_EVENTS`.

- [ ] **Step 1: Extend the failing key-filter test**

Add assertions that repeats and key-up events are ignored and that one press cannot launch two recognition activities.

- [ ] **Step 2: Implement the service and manifest configuration**

On key down, save `rootInActiveWindow.packageName`, reject a second request while one is active, launch `VoiceBridgeActivity` with `FLAG_ACTIVITY_NEW_TASK`, consume the event, and clear state after result or timeout.

- [ ] **Step 3: Run unit tests and assemble a debug APK**

Run `./gradlew app:testDebugUnitTest app:assembleDebug`; expect success.

- [ ] **Step 4: Enable exactly one capture service**

If Task 1 captured `609`, keep Key Mapper enabled and leave the fallback disabled. Otherwise enable only `.voice.GlobalVoiceAccessibilityService` through secure settings and verify it remains enabled for 60 seconds.

- [ ] **Step 5: Commit**

Commit as `feat: capture JMGO microphone key globally`.

### Task 4: FUTO bridge and safe text insertion

**Files:**
- Create: `android/app/src/main/kotlin/me/efesser/flauncher/voice/VoiceBridgeActivity.kt`
- Modify: `android/app/src/main/kotlin/me/efesser/flauncher/voice/GlobalVoiceAccessibilityService.kt`
- Modify: `android/app/src/main/AndroidManifest.xml`
- Test: `android/app/src/test/kotlin/me/efesser/flauncher/voice/VoiceResultTest.kt`

**Interfaces:**
- Consumes: FUTO `RecognizerIntent.EXTRA_RESULTS` and the originating package saved by the service.
- Produces: internal broadcast `me.efesser.flauncher.VOICE_RESULT` scoped to `me.efesser.flauncher` with extra `query: String`.

- [ ] **Step 1: Add failing tests for blank and valid result payloads**

Verify a blank result emits no insertion request and valid Russian text is normalized unchanged.

- [ ] **Step 2: Implement the transparent bridge activity**

Launch `ACTION_RECOGNIZE_SPEECH` explicitly in `org.futo.voiceinput`, set `EXTRA_LANGUAGE=ru-RU`, receive the first result, send the package-scoped broadcast, and finish. Display a Russian Toast when FUTO is unavailable.

- [ ] **Step 3: Implement accessibility insertion**

Restore the originating package, wait for `TYPE_WINDOW_STATE_CHANGED`, locate focused/non-password editable nodes, call `ACTION_SET_TEXT` with `ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE`, then invoke the app adapter from Task 5.

- [ ] **Step 4: Run tests and commit**

Run all Android unit tests and commit as `feat: insert offline voice result into focused field`.

### Task 5: Search submission adapters

**Files:**
- Create: `android/app/src/main/kotlin/me/efesser/flauncher/voice/SearchSubmitAdapter.kt`
- Test: `android/app/src/test/kotlin/me/efesser/flauncher/voice/SearchSubmitAdapterTest.kt`

**Interfaces:**
- Produces: `SearchSubmitAdapter.labelsFor(packageName: String): Set<String>` for `org.smarttube.stable`, `com.vk.tv`, `ru.kinopoisk.tv`, and `ru.zagonka.tv.wrapper`.

- [ ] **Step 1: Write failing per-package label tests**

Require Russian and English accessibility labels such as `Поиск`, `Найти`, `Search`, and `Submit` for the four supported packages; unknown packages return an empty set.

- [ ] **Step 2: Implement adapter lookup and node click**

After insertion, search visible clickable nodes by normalized content description/text and call `ACTION_CLICK`. If none exists, leave the query in place without synthesizing unsafe input events.

- [ ] **Step 3: Run tests and commit**

Run `app:testDebugUnitTest` and commit as `feat: submit voice searches in TV apps`.

### Task 6: Install and device verification

**Files:**
- Modify: `docs/jmgo-keymapper-probe.md`

**Interfaces:**
- Consumes: assembled profile APK and installed Key Mapper/FUTO.
- Produces: verified persistent device configuration.

- [ ] **Step 1: Build and install AOT profile APK**

Use Android ARM target, install with `adb install -r`, reapply HOME role, FUTO IME, microphone permission, and the selected accessibility service.

- [ ] **Step 2: Verify all four applications**

For SmartTube, VK Video, Kinopoisk, and Zagonka: open search, focus the field, press `609`, speak Russian, verify the query appears, and confirm search is submitted or safely remains ready for OK.

- [ ] **Step 3: Reboot and persistence test**

Reboot, wait for ADB, verify HOME resolves to FLauncher, the selected service remains enabled, FUTO is the default IME, and all six TV tiles remain present.

- [ ] **Step 4: Record evidence and commit**

Add package/component state, timings, and pass/fail results to the probe document and commit as `test: verify global JMGO voice input`.
