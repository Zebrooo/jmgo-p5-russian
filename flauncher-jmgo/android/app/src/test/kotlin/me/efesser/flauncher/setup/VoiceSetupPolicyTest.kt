package me.efesser.flauncher.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSetupPolicyTest {
    private val pkg = "me.efesser.flauncher"
    private val cls = "me.efesser.flauncher.voice.NativeVoiceAccessibilityService"

    @Test fun snapshotIsCompleteOnlyWhenEveryStepIsDone() {
        assertTrue(VoiceSetupSnapshot(true, true, true, true).complete)
        assertFalse(VoiceSetupSnapshot(false, true, true, true).complete)
        assertFalse(VoiceSetupSnapshot(true, false, true, true).complete)
        assertFalse(VoiceSetupSnapshot(true, true, false, true).complete)
        assertFalse(VoiceSetupSnapshot(true, true, true, false).complete)
    }

    @Test fun snapshotMapExposesEveryStepForTheFlutterSide() {
        val map = VoiceSetupSnapshot(true, false, true, false).toMap()
        assertEquals(
            mapOf(
                "recognizerInstalled" to true,
                "microphoneGranted" to false,
                "accessibilityEnabled" to true,
                "defaultLauncher" to false,
                "complete" to false,
            ),
            map,
        )
    }

    @Test fun recognisesTheServiceInLongAndShortComponentForms() {
        assertTrue(AccessibilityServiceSetting.isEnabled("$pkg/$cls", pkg, cls))
        assertTrue(AccessibilityServiceSetting.isEnabled("$pkg/.voice.NativeVoiceAccessibilityService", pkg, cls))
        assertTrue(AccessibilityServiceSetting.isEnabled(
            "com.other/.Service:$pkg/$cls:com.jmgo.voice/.KeyService", pkg, cls,
        ))
        assertTrue(AccessibilityServiceSetting.isEnabled(" $pkg/$cls ", pkg, cls))
    }

    @Test fun rejectsOtherServicesBlankSettingsAndLookalikes() {
        assertFalse(AccessibilityServiceSetting.isEnabled(null, pkg, cls))
        assertFalse(AccessibilityServiceSetting.isEnabled("", pkg, cls))
        assertFalse(AccessibilityServiceSetting.isEnabled("com.other/.Service", pkg, cls))
        assertFalse(AccessibilityServiceSetting.isEnabled("$pkg.evil/$cls", pkg, cls))
        assertFalse(AccessibilityServiceSetting.isEnabled("$pkg/${cls}Extra", pkg, cls))
        assertFalse(AccessibilityServiceSetting.isEnabled("$pkg/$cls", "", cls))
    }

    @Test fun normaliseHandlesMalformedEntries() {
        assertNull(AccessibilityServiceSetting.normalize(""))
        assertNull(AccessibilityServiceSetting.normalize("nopackage"))
        assertNull(AccessibilityServiceSetting.normalize("/onlyclass"))
        assertNull(AccessibilityServiceSetting.normalize("pkg/"))
        assertEquals("a.b/a.b.C", AccessibilityServiceSetting.normalize("a.b/.C"))
        assertEquals("a.b/x.y.C", AccessibilityServiceSetting.normalize("a.b/x.y.C"))
    }

    @Test fun launcherFallbackStartsCaptureOnlyWhenTheServiceCanInsertTheResult() {
        assertEquals(LauncherVoiceFallbackPolicy.Action.CAPTURE, LauncherVoiceFallbackPolicy.select(true))
        assertEquals(LauncherVoiceFallbackPolicy.Action.SETUP, LauncherVoiceFallbackPolicy.select(false))
    }
}
