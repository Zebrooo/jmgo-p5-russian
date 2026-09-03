package me.efesser.flauncher

import android.provider.Settings

object SettingsIntentPolicy {
    /** The dedicated Wi-Fi screen is not reliably reachable on JMGO firmware; open the root screen. */
    fun wifiAction(): String = Settings.ACTION_SETTINGS

    fun accessibilityAction(): String = Settings.ACTION_ACCESSIBILITY_SETTINGS

    fun homeAction(): String = Settings.ACTION_HOME_SETTINGS

    /** Used when a dedicated settings screen is missing on the firmware. */
    fun fallbackAction(): String = Settings.ACTION_SETTINGS
}
