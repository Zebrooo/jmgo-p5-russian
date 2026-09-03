package me.efesser.flauncher.setup

/**
 * Snapshot of everything the user has to configure once so that the microphone key works
 * everywhere. All values are plain booleans: the launcher only reports state and opens the
 * relevant system screen. It never changes secure settings itself.
 */
data class VoiceSetupSnapshot(
    val recognizerInstalled: Boolean,
    val microphoneGranted: Boolean,
    val accessibilityEnabled: Boolean,
    val defaultLauncher: Boolean,
) {
    val complete: Boolean
        get() = recognizerInstalled && microphoneGranted && accessibilityEnabled && defaultLauncher

    fun toMap(): Map<String, Boolean> = mapOf(
        "recognizerInstalled" to recognizerInstalled,
        "microphoneGranted" to microphoneGranted,
        "accessibilityEnabled" to accessibilityEnabled,
        "defaultLauncher" to defaultLauncher,
        "complete" to complete,
    )
}

/** Parses the colon-separated `enabled_accessibility_services` secure setting. */
object AccessibilityServiceSetting {
    fun isEnabled(enabledServices: String?, packageName: String, className: String): Boolean {
        if (enabledServices.isNullOrBlank()) return false
        val expected = flatten(packageName, className) ?: return false
        return enabledServices
            .split(':')
            .mapNotNull(::normalize)
            .any { it.equals(expected, ignoreCase = true) }
    }

    /** Expands `pkg/.Cls` into `pkg/pkg.Cls`; returns null for malformed entries. */
    fun normalize(component: String): String? {
        val trimmed = component.trim()
        val slash = trimmed.indexOf('/')
        if (slash <= 0 || slash == trimmed.length - 1) return null
        val pkg = trimmed.substring(0, slash)
        val cls = trimmed.substring(slash + 1)
        return flatten(pkg, if (cls.startsWith(".")) pkg + cls else cls)
    }

    private fun flatten(packageName: String, className: String): String? =
        if (packageName.isBlank() || className.isBlank()) null else "$packageName/$className"
}

/** What the launcher's own microphone-key handler should do while the launcher is foreground. */
object LauncherVoiceFallbackPolicy {
    enum class Action {
        /** The accessibility service is on: start capture, the service inserts the result. */
        CAPTURE,

        /** The service is off, so a result could never be inserted: show the setup screen. */
        SETUP,
    }

    fun select(accessibilityEnabled: Boolean): Action =
        if (accessibilityEnabled) Action.CAPTURE else Action.SETUP
}
