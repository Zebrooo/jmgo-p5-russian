import 'package:flauncher/flauncher_channel.dart';
import 'package:flauncher/providers/settings_service.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

/// One-time setup state reported by the native side. Plain booleans only.
class VoiceSetupStatus {
  const VoiceSetupStatus({
    required this.recognizerInstalled,
    required this.microphoneGranted,
    required this.accessibilityEnabled,
    required this.defaultLauncher,
  });

  final bool recognizerInstalled;
  final bool microphoneGranted;
  final bool accessibilityEnabled;
  final bool defaultLauncher;

  bool get complete => recognizerInstalled && microphoneGranted && accessibilityEnabled && defaultLauncher;

  int get missingSteps =>
      [recognizerInstalled, microphoneGranted, accessibilityEnabled, defaultLauncher].where((done) => !done).length;

  factory VoiceSetupStatus.fromMap(Map<dynamic, dynamic> value) => VoiceSetupStatus(
        recognizerInstalled: value['recognizerInstalled'] == true,
        microphoneGranted: value['microphoneGranted'] == true,
        accessibilityEnabled: value['accessibilityEnabled'] == true,
        defaultLauncher: value['defaultLauncher'] == true,
      );

  static Future<VoiceSetupStatus> load() async =>
      VoiceSetupStatus.fromMap(await FLauncherChannel().getVoiceSetupStatus());
}

/// Text-free diagnostics: timestamps, counters and an outcome name. Never speech or field text.
class VoiceDiagnostics {
  const VoiceDiagnostics({
    required this.serviceConnectedAtMs,
    required this.keyEventsSeenByService,
    required this.microphoneKeysSeenByService,
    required this.lastMicrophoneKeyInServiceAtMs,
    required this.lastMicrophoneKeyInLauncherAtMs,
    required this.lastOutcome,
    required this.lastOutcomeAtMs,
  });

  final int? serviceConnectedAtMs;
  final int keyEventsSeenByService;
  final int microphoneKeysSeenByService;
  final int? lastMicrophoneKeyInServiceAtMs;
  final int? lastMicrophoneKeyInLauncherAtMs;
  final String? lastOutcome;
  final int? lastOutcomeAtMs;

  factory VoiceDiagnostics.fromMap(Map<dynamic, dynamic> value) => VoiceDiagnostics(
        serviceConnectedAtMs: (value['serviceConnectedAtMs'] as num?)?.toInt(),
        keyEventsSeenByService: (value['keyEventsSeenByService'] as num?)?.toInt() ?? 0,
        microphoneKeysSeenByService: (value['microphoneKeysSeenByService'] as num?)?.toInt() ?? 0,
        lastMicrophoneKeyInServiceAtMs: (value['lastMicrophoneKeyInServiceAtMs'] as num?)?.toInt(),
        lastMicrophoneKeyInLauncherAtMs: (value['lastMicrophoneKeyInLauncherAtMs'] as num?)?.toInt(),
        lastOutcome: value['lastOutcome'] as String?,
        lastOutcomeAtMs: (value['lastOutcomeAtMs'] as num?)?.toInt(),
      );

  static Future<VoiceDiagnostics> load() async =>
      VoiceDiagnostics.fromMap(await FLauncherChannel().getVoiceDiagnostics());

  static String describeOutcome(String? outcome) {
    switch (outcome) {
      case null:
        return 'ещё не было';
      case 'SESSION_STARTED':
        return 'распознавание запущено';
      case 'ROUTED_TO_WEB_HOST':
        return 'передано WebView-приложению';
      case 'NO_FOREGROUND_WINDOW':
        return 'нет активного окна';
      case 'RECOGNIZER_MISSING':
        return 'распознавание недоступно';
      case 'RESULT_EMPTY_OR_STALE':
        return 'пустой или устаревший результат';
      case 'INSERTED':
        return 'текст вставлен';
      case 'FIELD_OR_WINDOW_LOST':
        return 'поле или окно потеряно';
      default:
        return outcome ?? 'ещё не было';
    }
  }
}

class VoiceSetupPanelPage extends StatefulWidget {
  static const String routeName = "voice_setup_panel";

  VoiceSetupPanelPage({
    Key? key,
    Future<VoiceSetupStatus> Function()? loadStatus,
    Future<VoiceDiagnostics> Function()? loadDiagnostics,
    Future<void> Function()? openAccessibilitySettings,
    Future<void> Function()? openRecognizerSettings,
    Future<void> Function()? openHomeSettings,
    DateTime Function()? now,
  })  : loadStatus = loadStatus ?? VoiceSetupStatus.load,
        loadDiagnostics = loadDiagnostics ?? VoiceDiagnostics.load,
        openAccessibilitySettings = openAccessibilitySettings ?? (() => FLauncherChannel().openAccessibilitySettings()),
        openRecognizerSettings = openRecognizerSettings ?? (() => FLauncherChannel().openRecognizerSettings()),
        openHomeSettings = openHomeSettings ?? (() => FLauncherChannel().openHomeSettings()),
        now = now ?? (() => DateTime.now()),
        super(key: key);

  final Future<VoiceSetupStatus> Function() loadStatus;
  final Future<VoiceDiagnostics> Function() loadDiagnostics;
  final Future<void> Function() openAccessibilitySettings;
  final Future<void> Function() openRecognizerSettings;
  final Future<void> Function() openHomeSettings;
  final DateTime Function() now;

  @override
  State<VoiceSetupPanelPage> createState() => _VoiceSetupPanelPageState();
}

class _VoiceSetupPanelPageState extends State<VoiceSetupPanelPage> with WidgetsBindingObserver {
  VoiceSetupStatus? _status;
  VoiceDiagnostics? _diagnostics;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _refresh();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // The user comes back from a system settings screen: re-check every step.
    if (state == AppLifecycleState.resumed) _refresh();
  }

  Future<void> _refresh() async {
    final status = await widget.loadStatus();
    final diagnostics = await widget.loadDiagnostics();
    if (!mounted) return;
    setState(() {
      _status = status;
      _diagnostics = diagnostics;
    });
  }

  @override
  Widget build(BuildContext context) {
    final status = _status;
    final theme = Theme.of(context);
    return SingleChildScrollView(
      child: Column(
        key: Key("VoiceSetupPanelPage"),
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text("Голосовой ввод", style: theme.textTheme.titleLarge, textAlign: TextAlign.center),
          Divider(),
          Padding(
            padding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            child: Text(_summary(status), key: Key("voice_setup_summary"), style: theme.textTheme.bodyMedium),
          ),
          _step(
            context,
            key: "recognizer",
            done: status?.recognizerInstalled,
            title: "Распознавание речи (FUTO)",
            doneHint: "JMGO-сборка FUTO Voice Input установлена.",
            todoHint: "Установите APK FUTO Voice Input (JMGO-сборка). Распознавание работает офлайн.",
            action: null,
          ),
          _step(
            context,
            key: "microphone",
            done: status?.microphoneGranted,
            title: "Микрофон для FUTO",
            doneHint: "Разрешение на микрофон выдано.",
            todoHint: "Откройте настройки FUTO → Разрешения → Микрофон.",
            action:
                status?.recognizerInstalled == true ? _Action("Настройки FUTO", widget.openRecognizerSettings) : null,
          ),
          _step(
            context,
            key: "accessibility",
            done: status?.accessibilityEnabled,
            title: "Сервис «Русский голосовой ввод»",
            doneHint: "Сервис включён: кнопка микрофона работает во всех приложениях.",
            todoHint: "Включите сервис в разделе «Специальные возможности». Лаунчер не может включить его сам.",
            action: _Action("Специальные возможности", widget.openAccessibilitySettings),
          ),
          _step(
            context,
            key: "home",
            done: status?.defaultLauncher,
            title: "Домашний экран",
            doneHint: "«Русский лаунчер» выбран домашним экраном.",
            todoHint: "Выберите «Русский лаунчер» домашним приложением по умолчанию.",
            action: _Action("Выбрать домашний экран", widget.openHomeSettings),
          ),
          Divider(),
          TextButton.icon(
            key: Key("voice_setup_refresh"),
            icon: Icon(Icons.refresh),
            label: Text("Проверить снова"),
            onPressed: _refresh,
          ),
          Consumer<SettingsService>(
            builder: (context, settingsService, _) => SwitchListTile(
              key: Key("voice_setup_check_on_start"),
              contentPadding: EdgeInsets.symmetric(horizontal: 8),
              value: settingsService.voiceSetupCheckOnStart,
              onChanged: (value) => settingsService.setVoiceSetupCheckOnStart(value),
              title: Text("Показывать при запуске"),
              subtitle: Text("Пока настройка не завершена."),
              dense: true,
            ),
          ),
          Divider(),
          Text("Диагностика", style: theme.textTheme.titleMedium),
          Padding(
            padding: EdgeInsets.all(8),
            child: Text(
              "Речь и содержимое полей не записываются. Только отметки времени и счётчики.",
              style: theme.textTheme.bodySmall,
            ),
          ),
          ..._diagnosticsLines(context, _diagnostics),
        ],
      ),
    );
  }

  String _summary(VoiceSetupStatus? status) {
    if (status == null) return "Проверка...";
    if (status.complete) {
      return "Всё настроено. В поле поиска нажмите кнопку микрофона на пульте и произнесите запрос.";
    }
    return "Осталось шагов: ${status.missingSteps}. Выполните их по порядку.";
  }

  Widget _step(
    BuildContext context, {
    required String key,
    required bool? done,
    required String title,
    required String doneHint,
    required String todoHint,
    required _Action? action,
  }) {
    final theme = Theme.of(context);
    final icon = done == null
        ? Icon(Icons.hourglass_empty, color: theme.colorScheme.onSurface)
        : done
            ? Icon(Icons.check_circle, key: Key("step_${key}_done"), color: Colors.lightGreenAccent)
            : Icon(Icons.error_outline, key: Key("step_${key}_todo"), color: Colors.orangeAccent);
    return Padding(
      padding: EdgeInsets.symmetric(horizontal: 8, vertical: 6),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              icon,
              SizedBox(width: 8),
              Expanded(child: Text(title, style: theme.textTheme.bodyLarge)),
            ],
          ),
          Padding(
            padding: EdgeInsets.only(left: 32, top: 2),
            child: Text(done == null ? "Проверка..." : (done ? doneHint : todoHint), style: theme.textTheme.bodySmall),
          ),
          if (action != null && done == false)
            Padding(
              padding: EdgeInsets.only(left: 24),
              child: TextButton.icon(
                key: Key("step_${key}_action"),
                icon: Icon(Icons.open_in_new, size: 18),
                label: Text(action.label),
                onPressed: action.run,
              ),
            ),
        ],
      ),
    );
  }

  List<Widget> _diagnosticsLines(BuildContext context, VoiceDiagnostics? diagnostics) {
    final style = Theme.of(context).textTheme.bodySmall;
    if (diagnostics == null) return [Text("Проверка...", style: style)];
    return [
      _line("Сервис подключён", diagnostics.serviceConnectedAtMs == null ? "нет" : "да", style),
      _line("Событий клавиш через сервис", "${diagnostics.keyEventsSeenByService}", style),
      _line(
        "Кнопка микрофона в сервисе",
        "${diagnostics.microphoneKeysSeenByService} раз, последний: ${_ago(diagnostics.lastMicrophoneKeyInServiceAtMs)}",
        style,
      ),
      _line("Кнопка микрофона в лаунчере", "последний: ${_ago(diagnostics.lastMicrophoneKeyInLauncherAtMs)}", style),
      _line(
        "Последний результат",
        "${VoiceDiagnostics.describeOutcome(diagnostics.lastOutcome)} (${_ago(diagnostics.lastOutcomeAtMs)})",
        style,
      ),
    ];
  }

  Widget _line(String label, String value, TextStyle? style) => Padding(
        padding: EdgeInsets.symmetric(horizontal: 8, vertical: 2),
        child: Text("$label: $value", style: style),
      );

  String _ago(int? atMs) {
    if (atMs == null) return "ещё не было";
    final seconds = widget.now().difference(DateTime.fromMillisecondsSinceEpoch(atMs)).inSeconds;
    if (seconds < 0) return "только что";
    if (seconds < 60) return "$seconds с назад";
    if (seconds < 3600) return "${seconds ~/ 60} мин назад";
    return "${seconds ~/ 3600} ч назад";
  }
}

class _Action {
  const _Action(this.label, this.run);

  final String label;
  final Future<void> Function() run;
}
