package me.efesser.flauncher

import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SettingsIntentPolicyTest {
    @Test
    fun wifiShortcutUsesSafeGeneralSettingsOnJmgo() {
        assertEquals(Settings.ACTION_SETTINGS, SettingsIntentPolicy.wifiAction())
        assertNotEquals(Settings.ACTION_WIFI_SETTINGS, SettingsIntentPolicy.wifiAction())
    }

    @Test
    fun setupScreensOpenDedicatedSystemPagesWithAGeneralFallback() {
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, SettingsIntentPolicy.accessibilityAction())
        assertEquals(Settings.ACTION_HOME_SETTINGS, SettingsIntentPolicy.homeAction())
        assertEquals(Settings.ACTION_SETTINGS, SettingsIntentPolicy.fallbackAction())
    }
}
