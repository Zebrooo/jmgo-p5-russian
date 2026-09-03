# Zagonka WebView Fullscreen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Zagonka video enter and exit true fullscreen reliably with the JMGO D-pad remote.

**Architecture:** Move fullscreen state transitions into a small controller and connect it to WebChromeClient custom-view callbacks. The Activity swaps between WebView and the HTML5 video custom view, applies immersive system UI flags, and gives Back deterministic priority: close video, navigate WebView history, then finish.

**Tech Stack:** Android 13, Java, WebView/WebChromeClient, JUnit 4, Gradle, ADB.

**Spec:** `/Users/dmitrii/projects/flauncher-jmgo/docs/superpowers/specs/2026-09-02-jmgo-global-voice-zagonka-design.md`

## Global Constraints

- Preserve cookies, DOM storage, media autoplay behavior, and the URL allowlist.
- Fullscreen must be controllable using only D-pad, OK, and Back.
- Back exits fullscreen before navigating or closing the app.
- No paid dependency or external fullscreen library.

---

### Task 1: Fullscreen state controller

**Files:**
- Create: `app/src/main/java/ru/zagonka/tv/wrapper/FullscreenState.java`
- Test: `app/src/test/java/ru/zagonka/tv/wrapper/FullscreenStateTest.java`

**Interfaces:**
- Produces: `FullscreenState.enter(): boolean`, `exit(): boolean`, and `isFullscreen(): boolean`.

- [ ] **Step 1: Write failing transition tests**

Test initial=false, first enter=true, duplicate enter=false, first exit=true, and duplicate exit=false.

- [ ] **Step 2: Run RED test**

Run `./gradlew testDebugUnitTest --tests '*FullscreenStateTest'`; expect missing class.

- [ ] **Step 3: Implement minimal state machine**

Use one private boolean; return whether each call changed state.

- [ ] **Step 4: Run tests and commit**

Run all unit tests and commit as `feat: model Zagonka fullscreen state`.

### Task 2: WebChromeClient custom-view integration

**Files:**
- Modify: `app/src/main/java/ru/zagonka/tv/wrapper/MainActivity.java`
- Test: `app/src/test/java/ru/zagonka/tv/wrapper/FullscreenStateTest.java`

**Interfaces:**
- Consumes: `FullscreenState`.
- Produces: Activity methods `showFullscreen(View, CustomViewCallback)` and `hideFullscreen()`.

- [ ] **Step 1: Add an idempotence regression test**

Verify duplicate `onShowCustomView`/`onHideCustomView` transitions cannot orphan the original WebView state.

- [ ] **Step 2: Implement a root FrameLayout**

Place WebView in a root `FrameLayout`. On custom view, hide WebView, attach video with MATCH_PARENT, store callback, request focus, and apply `IMMERSIVE_STICKY | FULLSCREEN | HIDE_NAVIGATION`. On hide, remove video, show/focus WebView, clear flags as appropriate, and call `onCustomViewHidden()` once.

- [ ] **Step 3: Implement Back priority**

When fullscreen, call `hideFullscreen()` and consume Back. Otherwise navigate WebView history; if none exists, delegate to Activity.

- [ ] **Step 4: Preserve lifecycle safely**

Call `hideFullscreen()` before destroying WebView, pause/resume WebView timers in Activity lifecycle, and avoid retaining detached custom views.

- [ ] **Step 5: Run tests and commit**

Run `./gradlew testDebugUnitTest assembleDebug` and commit as `feat: support fullscreen Zagonka video`.

### Task 3: Device verification

**Files:**
- Create: `docs/zagonka-fullscreen-verification.md`

**Interfaces:**
- Consumes: debug APK.
- Produces: verified APK installed as `ru.zagonka.tv.wrapper`.

- [ ] **Step 1: Install and launch**

Run `adb install -r app/build/outputs/apk/debug/app-debug.apk` and launch `.MainActivity`.

- [ ] **Step 2: Verify navigation and fullscreen**

Use D-pad to select a playable item, start playback, activate the page fullscreen control, verify video covers 1920x1080, press Back once to return to the page, and press Back again to navigate history.

- [ ] **Step 3: Verify voice-search field compatibility**

Focus Zagonka's search input and confirm the global bridge can set Russian text and activate its search control.

- [ ] **Step 4: Record evidence and commit**

Record tested URL, focus behavior, screenshots, and package version in the verification document; commit as `test: verify Zagonka fullscreen on JMGO P5`.
