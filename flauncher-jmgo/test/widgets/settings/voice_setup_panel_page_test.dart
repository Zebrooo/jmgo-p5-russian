import 'package:flauncher/providers/settings_service.dart';
import 'package:flauncher/widgets/settings/voice_setup_panel_page.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/mockito.dart';
import 'package:provider/provider.dart';

import '../../mocks.mocks.dart';

void main() {
  setUpAll(() async {
    final binding = TestWidgetsFlutterBinding.ensureInitialized();
    binding.window.physicalSizeTestValue = Size(1280, 720);
    binding.window.devicePixelRatioTestValue = 1.0;
    binding.platformDispatcher.textScaleFactorTestValue = 0.8;
  });

  const incomplete = VoiceSetupStatus(
    recognizerInstalled: true,
    microphoneGranted: false,
    accessibilityEnabled: false,
    defaultLauncher: true,
  );
  const complete = VoiceSetupStatus(
    recognizerInstalled: true,
    microphoneGranted: true,
    accessibilityEnabled: true,
    defaultLauncher: true,
  );
  final now = DateTime(2026, 9, 3, 12, 0, 0);
  final diagnostics = VoiceDiagnostics(
    serviceConnectedAtMs: now.millisecondsSinceEpoch - 90 * 1000,
    keyEventsSeenByService: 12,
    microphoneKeysSeenByService: 2,
    lastMicrophoneKeyInServiceAtMs: now.millisecondsSinceEpoch - 5 * 1000,
    lastMicrophoneKeyInLauncherAtMs: null,
    lastOutcome: 'INSERTED',
    lastOutcomeAtMs: now.millisecondsSinceEpoch - 4 * 1000,
  );

  testWidgets("shows missing steps with their system-settings actions", (tester) async {
    var accessibilityOpened = 0;
    var recognizerOpened = 0;
    var homeOpened = 0;
    await _pump(
      tester,
      loadStatus: () async => incomplete,
      loadDiagnostics: () async => diagnostics,
      openAccessibilitySettings: () async => accessibilityOpened += 1,
      openRecognizerSettings: () async => recognizerOpened += 1,
      openHomeSettings: () async => homeOpened += 1,
      now: () => now,
    );

    expect(find.text("Осталось шагов: 2. Выполните их по порядку."), findsOneWidget);
    expect(find.byKey(Key("step_recognizer_done")), findsOneWidget);
    expect(find.byKey(Key("step_microphone_todo")), findsOneWidget);
    expect(find.byKey(Key("step_accessibility_todo")), findsOneWidget);
    expect(find.byKey(Key("step_home_done")), findsOneWidget);
    expect(find.byKey(Key("step_home_action")), findsNothing);
    expect(find.byKey(Key("step_recognizer_action")), findsNothing);

    await tester.tap(find.byKey(Key("step_microphone_action")));
    await tester.tap(find.byKey(Key("step_accessibility_action")));
    await tester.pump();
    expect(recognizerOpened, 1);
    expect(accessibilityOpened, 1);
    expect(homeOpened, 0);

    expect(find.text("Событий клавиш через сервис: 12"), findsOneWidget);
    expect(find.text("Кнопка микрофона в сервисе: 2 раз, последний: 5 с назад"), findsOneWidget);
    expect(find.text("Кнопка микрофона в лаунчере: последний: ещё не было"), findsOneWidget);
    expect(find.text("Последний результат: текст вставлен (4 с назад)"), findsOneWidget);
    expect(find.text("Сервис подключён: да"), findsOneWidget);
  });

  testWidgets("shows a finished setup without any action buttons", (tester) async {
    await _pump(
      tester,
      loadStatus: () async => complete,
      loadDiagnostics: () async => diagnostics,
      now: () => now,
    );

    expect(find.textContaining("Всё настроено"), findsOneWidget);
    expect(find.byKey(Key("step_microphone_action")), findsNothing);
    expect(find.byKey(Key("step_accessibility_action")), findsNothing);
    expect(find.byKey(Key("step_home_action")), findsNothing);
  });

  testWidgets("'Check again' reloads the status", (tester) async {
    var loads = 0;
    await _pump(
      tester,
      loadStatus: () async {
        loads += 1;
        return loads == 1 ? incomplete : complete;
      },
      loadDiagnostics: () async => diagnostics,
      now: () => now,
    );
    expect(find.byKey(Key("step_accessibility_todo")), findsOneWidget);

    await tester.tap(find.byKey(Key("voice_setup_refresh")));
    await tester.pumpAndSettle();

    expect(loads, 2);
    expect(find.byKey(Key("step_accessibility_done")), findsOneWidget);
  });

  testWidgets("'Show on start' switch calls SettingsService", (tester) async {
    final settingsService = await _pump(
      tester,
      loadStatus: () async => complete,
      loadDiagnostics: () async => diagnostics,
      now: () => now,
    );

    await tester.tap(find.byKey(Key("voice_setup_check_on_start")));
    await tester.pump();

    verify(settingsService.setVoiceSetupCheckOnStart(false));
  });

  test("status parses native maps and counts missing steps", () {
    final status = VoiceSetupStatus.fromMap({
      'recognizerInstalled': true,
      'microphoneGranted': null,
      'accessibilityEnabled': false,
    });
    expect(status.recognizerInstalled, isTrue);
    expect(status.microphoneGranted, isFalse);
    expect(status.defaultLauncher, isFalse);
    expect(status.complete, isFalse);
    expect(status.missingSteps, 3);
    expect(VoiceDiagnostics.describeOutcome(null), "ещё не было");
    expect(VoiceDiagnostics.describeOutcome("FIELD_OR_WINDOW_LOST"), "поле или окно потеряно");
    expect(VoiceDiagnostics.describeOutcome("SOMETHING_NEW"), "SOMETHING_NEW");
  });
}

Future<MockSettingsService> _pump(
  WidgetTester tester, {
  required Future<VoiceSetupStatus> Function() loadStatus,
  required Future<VoiceDiagnostics> Function() loadDiagnostics,
  Future<void> Function()? openAccessibilitySettings,
  Future<void> Function()? openRecognizerSettings,
  Future<void> Function()? openHomeSettings,
  required DateTime Function() now,
}) async {
  final settingsService = MockSettingsService();
  when(settingsService.voiceSetupCheckOnStart).thenReturn(true);
  await tester.pumpWidget(
    ChangeNotifierProvider<SettingsService>.value(
      value: settingsService,
      builder: (_, __) => MaterialApp(
        home: Material(
          child: VoiceSetupPanelPage(
            loadStatus: loadStatus,
            loadDiagnostics: loadDiagnostics,
            openAccessibilitySettings: openAccessibilitySettings ?? () async {},
            openRecognizerSettings: openRecognizerSettings ?? () async {},
            openHomeSettings: openHomeSettings ?? () async {},
            now: now,
          ),
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
  return settingsService;
}
