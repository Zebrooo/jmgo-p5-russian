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
}
