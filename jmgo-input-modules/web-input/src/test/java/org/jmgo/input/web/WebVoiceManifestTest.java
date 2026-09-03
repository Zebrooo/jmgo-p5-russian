package org.jmgo.input.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import org.jmgo.input.core.InputContract;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** The manifest contribution of web-input is what turns a host into a WebView voice host. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.TIRAMISU)
public final class WebVoiceManifestTest {
    @Test
    @SuppressWarnings("deprecation")
    public void declaresTheExportedWebVoiceCapabilityActivity() {
        Context context = RuntimeEnvironment.getApplication();
        PackageManager packageManager = context.getPackageManager();

        ResolveInfo info = packageManager.resolveActivity(
                new Intent(InputContract.ACTION_WEB_VOICE).setPackage(context.getPackageName()), 0);

        assertNotNull("WEB_VOICE must resolve inside the host package", info);
        assertEquals(WebVoiceActivity.class.getName(), info.activityInfo.name);
        assertEquals(context.getPackageName(), info.activityInfo.packageName);
        assertTrue("the accessibility service starts it from another package", info.activityInfo.exported);
    }
}
