#!/usr/bin/env bash
# Builds and tests every component from the repository root.
#
# Requirements: JDK 17, Android SDK (platforms 33 and 35, build-tools, cmake 3.22.1, NDK),
# Node.js, Flutter 3.7.5, flauncher-jmgo/android/local.properties with sdk.dir and
# flutter.sdk, and flauncher-jmgo/android/app/google-services.json (see README).
#
# Usage:
#   scripts/build-all.sh            # tests + debug builds of everything
#   scripts/build-all.sh --tests    # tests only, no APK/AAR assembly
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
TESTS_ONLY=0
[ "${1:-}" = "--tests" ] && TESTS_ONLY=1

GRADLEW_FLAUNCHER="$ROOT/flauncher-jmgo/android/gradlew"
GRADLEW_FUTO="$ROOT/futo-voice-jmgo/gradlew"
chmod +x "$GRADLEW_FLAUNCHER" "$GRADLEW_FUTO"

step() { printf '\n\033[1m==> %s\033[0m\n' "$1"; }

step "web-input DOM tests (Node)"
node --test jmgo-input-modules/web-input/src/test/js/jmgo-web-input.test.js

step "input-core, web-input, Zagonka"
ZAGONKA_TASKS=(:input-core:test :web-input:test :app:testDebugUnitTest)
[ "$TESTS_ONLY" = 0 ] && ZAGONKA_TASKS+=(:input-core:assemble :web-input:assemble :app:assembleDebug)
"$GRADLEW_FLAUNCHER" -p zagonka-tv-wrapper "${ZAGONKA_TASKS[@]}"

step "FLauncher (Flutter)"
(cd flauncher-jmgo && flutter pub get && flutter analyze && flutter test)
FLAUNCHER_TASKS=(app:testDebugUnitTest)
[ "$TESTS_ONLY" = 0 ] && FLAUNCHER_TASKS+=(app:assembleDebug)
"$GRADLEW_FLAUNCHER" -p flauncher-jmgo/android "${FLAUNCHER_TASKS[@]}"

step "FUTO Voice Input (JMGO flavour)"
FUTO_TASKS=(testPlayStoreDebugUnitTest)
[ "$TESTS_ONLY" = 0 ] && FUTO_TASKS+=(assemblePlayStoreDebug)
"$GRADLEW_FUTO" -p futo-voice-jmgo "${FUTO_TASKS[@]}"

if [ "$TESTS_ONLY" = 0 ]; then
  step "Artifacts"
  ls -1 zagonka-tv-wrapper/app/build/outputs/apk/debug/*.apk \
        flauncher-jmgo/build/app/outputs/apk/debug/*.apk \
        futo-voice-jmgo/app/build/outputs/apk/playStore/debug/*.apk 2>/dev/null || true
fi

step "Done"
