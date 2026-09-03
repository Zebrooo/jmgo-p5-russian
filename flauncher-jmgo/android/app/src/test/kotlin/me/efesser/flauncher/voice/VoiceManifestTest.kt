package me.efesser.flauncher.voice

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import org.jmgo.input.core.InputContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Merged-manifest guarantees for the accessibility bridge. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class VoiceManifestTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    @Suppress("DEPRECATION")
    fun accessibilityServiceIsExportedForTheSystemOnly() {
        val info = context.packageManager.getServiceInfo(
            ComponentName(context, NativeVoiceAccessibilityService::class.java),
            PackageManager.GET_META_DATA,
        )

        assertTrue("Android binds the service, so it must be exported", info.exported)
        assertEquals("android.permission.BIND_ACCESSIBILITY_SERVICE", info.permission)
        assertTrue(
            "service configuration resource must be declared",
            info.metaData.containsKey("android.accessibilityservice"),
        )
    }

    @Test
    @Suppress("DEPRECATION")
    fun accessibilityServiceAnswersTheSystemIntentFilter() {
        val services = context.packageManager.queryIntentServices(
            Intent("android.accessibilityservice.AccessibilityService").setPackage(context.packageName),
            0,
        )

        assertTrue(services.any { it.serviceInfo.name == NativeVoiceAccessibilityService::class.java.name })
    }

    @Test
    @Suppress("DEPRECATION")
    fun captureActivityStaysPrivateToTheLauncher() {
        val info = context.packageManager.getActivityInfo(
            ComponentName(context, NativeVoiceCaptureActivity::class.java),
            0,
        )

        assertFalse(info.exported)
    }

    @Test
    @Suppress("DEPRECATION")
    fun launcherItselfIsANativeHostNotAWebVoiceHost() {
        val resolved = context.packageManager.resolveActivity(
            Intent(InputContract.ACTION_WEB_VOICE).setPackage(context.packageName),
            0,
        )

        assertNull("the launcher must not embed web-input", resolved)
    }
}
