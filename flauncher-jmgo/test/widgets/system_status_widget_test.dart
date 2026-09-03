import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flauncher/widgets/system_status_widget.dart';

void main() {
  testWidgets('shows wifi and battery percentage and opens their settings',
      (tester) async {
    var wifiOpened = false;
    var settingsOpened = false;

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: SystemStatusWidget(
          loadStatus: () async => const SystemStatus(
            wifiConnected: true,
            wifiSsid: 'Nini-5G',
            wifiLevel: 3,
            pluggedIn: false,
            batteryPercent: 48,
          ),
          openWifiSettings: () async => wifiOpened = true,
          openSystemSettings: () async => settingsOpened = true,
          timeWidget: const Text('12:00'),
        ),
      ),
    ));
    await tester.pump();

    expect(find.text('Nini-5G'), findsOneWidget);
    expect(find.text('48% · Батарея'), findsOneWidget);

    await tester.tap(find.byKey(const Key('wifi_status')));
    await tester.tap(find.byKey(const Key('system_settings')));
    expect(wifiOpened, isTrue);
    expect(settingsOpened, isTrue);
  });

  testWidgets('shows charging state next to battery percentage',
      (tester) async {
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: SystemStatusWidget(
          loadStatus: () async => const SystemStatus(
            wifiConnected: false,
            wifiSsid: '',
            wifiLevel: 0,
            pluggedIn: true,
            batteryPercent: 48,
          ),
          openWifiSettings: () async {},
          openSystemSettings: () async {},
          timeWidget: const Text('12:00'),
        ),
      ),
    ));
    await tester.pump();

    expect(find.text('48% · Зарядка'), findsOneWidget);
  });
}
