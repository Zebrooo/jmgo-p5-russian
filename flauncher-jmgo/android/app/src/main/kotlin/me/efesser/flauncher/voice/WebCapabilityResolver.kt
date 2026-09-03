package me.efesser.flauncher.voice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import org.jmgo.input.core.InputContract

class WebCapabilityResolver(private val context: Context) {
    @Suppress("DEPRECATION")
    fun resolve(packageName: String): ComponentName? {
        if (packageName.isBlank()) return null
        val info = context.packageManager.resolveActivity(
            Intent(InputContract.ACTION_WEB_VOICE).setPackage(packageName),
            0,
        )?.activityInfo ?: return null
        if (info.packageName != packageName) return null
        return ComponentName(info.packageName, info.name)
    }
}
