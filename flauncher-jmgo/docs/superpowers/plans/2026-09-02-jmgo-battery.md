# JMGO Battery Status Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Display the JMGO MCU battery percentage and charging state in FLauncher's top panel.

**Architecture:** A pure Kotlin selector validates MCU sysfs strings and falls back to Android broadcast values. The method channel adds `batteryPercent`; the existing Flutter widget renders the percentage without changing panel structure.

**Tech Stack:** Kotlin, Android `BatteryManager`, Flutter/Dart, JUnit 4, flutter_test.

**Spec:** `docs/superpowers/specs/2026-09-02-jmgo-battery-design.md`

## Global Constraints

- Preserve the existing top-panel layout and 10-second refresh interval.
- Accept battery percentages only in the inclusive range `0..100`.
- Preserve the standard Android battery fallback.
- Do not clear launcher data during installation.

---

### Task 1: Select real battery values on Android

**Files:**
- Create: `android/app/src/main/kotlin/me/efesser/flauncher/BatteryStatus.kt`
- Test: `android/app/src/test/kotlin/me/efesser/flauncher/BatteryStatusTest.kt`
- Modify: `android/app/src/main/kotlin/me/efesser/flauncher/MainActivity.kt`

**Interfaces:**
- Produces: `BatteryStatus.select(mcuCapacity: String?, mcuStatus: String?, broadcastLevel: Int?, broadcastScale: Int?, plugged: Boolean): BatterySnapshot`.
- Produces: `BatterySnapshot(percent: Int?, charging: Boolean)`.

- [ ] **Step 1: Write failing selector tests**

```kotlin
assertEquals(BatterySnapshot(48, false), BatteryStatus.select("48\n", "Discharging\n", 100, 100, true))
assertEquals(BatterySnapshot(50, true), BatteryStatus.select(null, null, 25, 50, true))
assertEquals(BatterySnapshot(null, false), BatteryStatus.select("-1", "Discharging", null, null, false))
```

- [ ] **Step 2: Verify RED**

Run `cd android && ./gradlew app:testDebugUnitTest --tests '*BatteryStatusTest'`. Expect compilation failure because the selector is missing.

- [ ] **Step 3: Implement selector and native bridge**

Read the two MCU files with `File.readText()` inside `runCatching`, read broadcast level/scale/plugged, call the selector, and add `batteryPercent` plus the selector's charging value under existing key `pluggedIn`.

- [ ] **Step 4: Verify GREEN**

Run the focused Android test and require zero failures.

### Task 2: Render percentage in the top panel

**Files:**
- Modify: `lib/widgets/system_status_widget.dart`
- Modify: `test/widgets/system_status_widget_test.dart`

**Interfaces:**
- Consumes: nullable method-channel field `batteryPercent`.
- Produces: visible text `<percent>% · Батарея` or `<percent>% · Зарядка`.

- [ ] **Step 1: Change the widget test first**

Supply `batteryPercent: 48` and `pluggedIn: false`, then assert `find.text('48% · Батарея')`. Add a separate charging case asserting `find.text('48% · Зарядка')`.

- [ ] **Step 2: Verify RED**

Run `flutter test test/widgets/system_status_widget_test.dart`; expect compilation or assertion failure because `batteryPercent` is absent.

- [ ] **Step 3: Implement minimal Dart mapping and label**

Add nullable `int batteryPercent` to `SystemStatus`, parse it from the map, and render the combined Russian label while retaining the existing icon and controls.

- [ ] **Step 4: Verify GREEN and regression suite**

Run the focused widget test, `flutter test`, and `flutter build apk --profile --target-platform android-arm`.

### Task 3: Install and verify the launcher

**Files:**
- Build artifact: `build/app/outputs/flutter-apk/app-profile.apk`

**Interfaces:**
- Consumes: current launcher database and category ordering.
- Produces: updated default HOME launcher with preserved tiles.

- [ ] **Step 1: Install without clearing data**

Run `adb install -r build/app/outputs/flutter-apk/app-profile.apk` and reassert FLauncher as HOME only if Android changed the role.

- [ ] **Step 2: Compare device values**

Read MCU capacity/status with `run-as me.efesser.flauncher`, open HOME, and confirm the panel shows the same percentage and charging label.

- [ ] **Step 3: Regression-check controls and tiles**

Open Wi-Fi and system settings from the top panel, then verify SmartTube, Zagonka, VK Video, Kinopoisk, Alice, AirScreen, and HolaCast are visible launchable applications.

