package me.efesser.flauncher

import android.provider.Settings

object SettingsIntentPolicy {
    fun wifiAction(): String = Settings.ACTION_SETTINGS
}
