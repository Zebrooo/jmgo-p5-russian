package me.efesser.flauncher.voice

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import org.jmgo.input.core.InputContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class WebCapabilityResolverTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val webHost = ComponentName("web.host", "web.host.VoiceActivity")

    @Before
    fun installPackages() {
        val shadowPackageManager = shadowOf(context.packageManager)
        for (packageName in listOf("web.host", "video.app")) {
            shadowPackageManager.installPackage(
                PackageInfo().apply {
                    this.packageName = packageName
                    applicationInfo = ApplicationInfo().apply { this.packageName = packageName }
                },
            )
        }
        shadowPackageManager.addActivityIfNotPresent(webHost)
        shadowPackageManager.addIntentFilterForActivity(
            webHost,
            IntentFilter(InputContract.ACTION_WEB_VOICE).apply { addCategory(Intent.CATEGORY_DEFAULT) },
        )
    }

    @Test
    fun resolvesTheWebVoiceActivityOfAWebHost() {
        assertEquals(webHost, WebCapabilityResolver(context).resolve("web.host"))
    }

    @Test
    fun treatsPackagesWithoutTheCapabilityAsNative() {
        assertNull(WebCapabilityResolver(context).resolve("video.app"))
        assertNull(WebCapabilityResolver(context).resolve("missing.app"))
        assertNull(WebCapabilityResolver(context).resolve(""))
    }
}
