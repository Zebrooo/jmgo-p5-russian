import 'dart:async';

import 'package:flauncher/flauncher_channel.dart';
import 'package:flauncher/widgets/settings/settings_panel.dart';
import 'package:flauncher/widgets/time_widget.dart';
import 'package:flutter/material.dart';

class SystemStatus {
  const SystemStatus({
    required this.wifiConnected,
    required this.wifiSsid,
    required this.wifiLevel,
    required this.pluggedIn,
    required this.batteryPercent,
  });

  final bool wifiConnected;
  final String wifiSsid;
  final int wifiLevel;
  final bool pluggedIn;
  final int? batteryPercent;

  factory SystemStatus.fromMap(Map<dynamic, dynamic> value) => SystemStatus(
        wifiConnected: value['wifiConnected'] == true,
        wifiSsid: value['wifiSsid'] as String? ?? '',
        wifiLevel: value['wifiLevel'] as int? ?? 0,
        pluggedIn: value['pluggedIn'] == true,
        batteryPercent: value['batteryPercent'] as int?,
      );
}

class SystemStatusWidget extends StatefulWidget {
  SystemStatusWidget({
    Key? key,
    Future<SystemStatus> Function()? loadStatus,
    Future<void> Function()? openWifiSettings,
    Future<void> Function(BuildContext)? openLauncherSettings,
    Widget? timeWidget,
  })  : loadStatus = loadStatus ??
            (() async => SystemStatus.fromMap(
                await FLauncherChannel().getSystemStatus())),
        openWifiSettings =
            openWifiSettings ?? (() => FLauncherChannel().openWifiSettings()),
        openLauncherSettings = openLauncherSettings ?? _showSettingsPanel,
        timeWidget = timeWidget ?? TimeWidget(),
        super(key: key);

  final Future<SystemStatus> Function() loadStatus;
  final Future<void> Function() openWifiSettings;

  /// The launcher's own settings panel; it contains the Android settings shortcut,
  /// voice-input setup, categories, wallpaper and the about dialog.
  final Future<void> Function(BuildContext) openLauncherSettings;
  final Widget timeWidget;

  static Future<void> _showSettingsPanel(BuildContext context) =>
      showDialog(context: context, builder: (_) => SettingsPanel());

  @override
  State<SystemStatusWidget> createState() => _SystemStatusWidgetState();
}

class _SystemStatusWidgetState extends State<SystemStatusWidget> {
  SystemStatus? _status;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _refresh();
    _timer = Timer.periodic(const Duration(seconds: 10), (_) => _refresh());
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Future<void> _refresh() async {
    final status = await widget.loadStatus();
    if (mounted) setState(() => _status = status);
  }

  @override
  Widget build(BuildContext context) {
    final status = _status;
    final color = Theme.of(context).colorScheme.onSurface;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        TextButton.icon(
          key: const Key('wifi_status'),
          onPressed: widget.openWifiSettings,
          icon: Icon(
              status?.wifiConnected == true
                  ? _wifiIcon(status!.wifiLevel)
                  : Icons.wifi_off,
              color: color),
          label: Text(
              status?.wifiSsid.isNotEmpty == true ? status!.wifiSsid : 'Wi-Fi',
              style: TextStyle(color: color)),
        ),
        const SizedBox(width: 8),
        Icon(status?.pluggedIn == true ? Icons.power : Icons.battery_std,
            color: color),
        const SizedBox(width: 5),
        Text(_batteryLabel(status)),
        const SizedBox(width: 20),
        widget.timeWidget,
        const SizedBox(width: 16),
        IconButton(
          key: const Key('launcher_settings'),
          onPressed: () => widget.openLauncherSettings(context),
          icon: const Icon(Icons.settings_outlined),
          tooltip: 'Настройки',
        ),
        const SizedBox(width: 20),
      ],
    );
  }

  String _batteryLabel(SystemStatus? status) {
    final power = status?.pluggedIn == true ? 'Зарядка' : 'Батарея';
    final percent = status?.batteryPercent;
    return percent == null ? power : '$percent% · $power';
  }

  IconData _wifiIcon(int level) {
    if (level <= 0) return Icons.signal_wifi_0_bar;
    if (level <= 2) return Icons.network_wifi_1_bar;
    if (level == 3) return Icons.network_wifi_2_bar;
    return Icons.wifi;
  }
}
