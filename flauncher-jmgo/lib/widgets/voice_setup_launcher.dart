import 'package:flauncher/flauncher_channel.dart';
import 'package:flauncher/providers/settings_service.dart';
import 'package:flauncher/widgets/settings/settings_panel.dart';
import 'package:flauncher/widgets/settings/voice_setup_panel_page.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

/// Opens the voice-input setup screen once at start-up while a setup step is missing,
/// and whenever the native side asks for it (microphone key pressed with the service off).
class VoiceSetupLauncher extends StatefulWidget {
  const VoiceSetupLauncher({
    Key? key,
    required this.child,
    this.loadStatus,
    this.setRequestedListener,
  }) : super(key: key);

  final Widget child;
  final Future<VoiceSetupStatus> Function()? loadStatus;
  final void Function(void Function()? listener)? setRequestedListener;

  @override
  State<VoiceSetupLauncher> createState() => _VoiceSetupLauncherState();
}

class _VoiceSetupLauncherState extends State<VoiceSetupLauncher> {
  bool _open = false;

  @override
  void initState() {
    super.initState();
    (widget.setRequestedListener ?? FLauncherChannel().setVoiceSetupRequestedListener)(_show);
    WidgetsBinding.instance.addPostFrameCallback((_) => _checkOnStart());
  }

  @override
  void dispose() {
    (widget.setRequestedListener ?? FLauncherChannel().setVoiceSetupRequestedListener)(null);
    super.dispose();
  }

  Future<void> _checkOnStart() async {
    if (!mounted || !context.read<SettingsService>().voiceSetupCheckOnStart) return;
    try {
      final status = await (widget.loadStatus ?? VoiceSetupStatus.load)();
      if (mounted && !status.complete) _show();
    } catch (error) {
      debugPrint("Could not check voice setup status: $error");
    }
  }

  Future<void> _show() async {
    if (_open || !mounted) return;
    _open = true;
    try {
      await showDialog(
        context: context,
        builder: (_) => SettingsPanel(initialRoute: VoiceSetupPanelPage.routeName),
      );
    } finally {
      _open = false;
    }
  }

  @override
  Widget build(BuildContext context) => widget.child;
}
