import 'package:flauncher/providers/settings_service.dart';
import 'package:flauncher/widgets/settings/voice_setup_panel_page.dart';
import 'package:flauncher/widgets/voice_setup_launcher.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/mockito.dart';
import 'package:provider/provider.dart';

import '../mocks.mocks.dart';

void main() {
  setUpAll(() async {
    final binding = TestWidgetsFlutterBinding.ensureInitialized();
    binding.window.physicalSizeTestValue = Size(1280, 720);
    binding.window.devicePixelRatioTestValue = 1.0;
    binding.platformDispatcher.textScaleFactorTestValue = 0.8;
  });

  const incomplete = VoiceSetupStatus(
    recognizerInstalled: false,
    microphoneGranted: false,
    accessibilityEnabled: false,
    defaultLauncher: false,
  );
  const complete = VoiceSetupStatus(
    recognizerInstalled: true,
    microphoneGranted: true,
    accessibilityEnabled: true,
    defaultLauncher: true,
  );

  testWidgets("opens the setup panel on start while setup is incomplete", (tester) async {
    await _pump(tester, checkOnStart: true, status: incomplete);

    expect(find.byKey(Key("VoiceSetupPanelPage")), findsOneWidget);
  });

  testWidgets("stays quiet when setup is complete", (tester) async {
    await _pump(tester, checkOnStart: true, status: complete);

    expect(find.byKey(Key("VoiceSetupPanelPage")), findsNothing);
  });

  testWidgets("stays quiet when the start-up check is disabled", (tester) async {
    var loads = 0;
    await _pump(tester, checkOnStart: false, status: incomplete, onLoad: () => loads += 1);

    expect(find.byKey(Key("VoiceSetupPanelPage")), findsNothing);
    expect(loads, 0);
  });

  testWidgets("opens the setup panel once when native asks for it", (tester) async {
    void Function()? listener;
    await _pump(
      tester,
      checkOnStart: false,
      status: complete,
      setRequestedListener: (value) => listener = value,
    );
    expect(listener, isNotNull);

    listener!();
    listener!();
    await tester.pumpAndSettle();

    expect(find.byKey(Key("VoiceSetupPanelPage")), findsOneWidget);
  });
}

Future<void> _pump(
  WidgetTester tester, {
  required bool checkOnStart,
  required VoiceSetupStatus status,
  void Function()? onLoad,
  void Function(void Function()? listener)? setRequestedListener,
}) async {
  final settingsService = MockSettingsService();
  when(settingsService.voiceSetupCheckOnStart).thenReturn(checkOnStart);
  await tester.pumpWidget(
    ChangeNotifierProvider<SettingsService>.value(
      value: settingsService,
      builder: (_, __) => MaterialApp(
        home: Builder(
          builder: (context) => VoiceSetupLauncher(
            loadStatus: () async {
              onLoad?.call();
              return status;
            },
            setRequestedListener: setRequestedListener ?? (_) {},
            child: Container(key: Key("home")),
          ),
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
}
