package me.efesser.flauncher.voice

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import me.efesser.flauncher.R
import org.jmgo.input.core.InputContract
import org.jmgo.input.core.VoiceRoute
import org.jmgo.input.core.VoiceSessionGate
import java.util.UUID

class NativeVoiceAccessibilityService : AccessibilityService() {
    private val sessionGate = VoiceSessionGate(InputContract.DEFAULT_SESSION_TIMEOUT_MS)
    private lateinit var capabilityResolver: WebCapabilityResolver
    private var originPackage: String? = null
    private var originWindowId: Int? = null
    private var resultReceiverRegistered = false

    private val resultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != InputContract.ACTION_NATIVE_VOICE_RESULT) return
            applyNativeResult(intent)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        capabilityResolver = WebCapabilityResolver(this)
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        val filter = IntentFilter(InputContract.ACTION_NATIVE_VOICE_RESULT)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(resultReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(resultReceiver, filter)
        }
        resultReceiverRegistered = true
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!NativeVoicePolicy.shouldConsume(event.keyCode, event.action, event.repeatCount)) return false
        val root = rootInActiveWindow
        val currentPackage = root?.packageName?.toString()

        if (currentPackage == InputContract.FUTO_PACKAGE || sessionGate.isActive(SystemClock.elapsedRealtime())) {
            sendBroadcast(Intent(InputContract.ACTION_FINISH_RECOGNITION).setPackage(InputContract.FUTO_PACKAGE))
            return true
        }

        val route = NativeVoicePolicy.route(
            currentPackage,
            currentPackage?.let { capabilityResolver.resolve(it) } != null,
        )
        if (route == VoiceRoute.NONE || currentPackage == null) {
            Toast.makeText(this, R.string.native_voice_no_field, Toast.LENGTH_SHORT).show()
            return true
        }

        val sessionId = UUID.randomUUID().toString()
        if (!sessionGate.start(sessionId, SystemClock.elapsedRealtime())) return true
        originPackage = currentPackage
        originWindowId = root.windowId

        try {
            if (route == VoiceRoute.WEB) {
                val component = capabilityResolver.resolve(currentPackage)
                    ?: throw IllegalStateException("Web capability disappeared")
                startActivity(
                    Intent(InputContract.ACTION_WEB_VOICE)
                        .setComponent(component)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(InputContract.EXTRA_SESSION_ID, sessionId)
                        .putExtra(InputContract.EXTRA_ORIGIN_PACKAGE, currentPackage),
                )
                clearSession()
            } else {
                startActivity(
                    NativeVoiceCaptureActivity.intent(this, sessionId, currentPackage)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        } catch (_: RuntimeException) {
            clearSession()
            Toast.makeText(this, R.string.native_voice_unavailable, Toast.LENGTH_SHORT).show()
        }
        return true
    }

    private fun applyNativeResult(intent: Intent) {
        val expectedOrigin = originPackage
        val root = rootInActiveWindow
        val currentPackage = root?.packageName?.toString()
        val result = intent.getStringExtra(InputContract.EXTRA_RESULT)
        val sessionId = intent.getStringExtra(InputContract.EXTRA_SESSION_ID)
        val declaredOrigin = intent.getStringExtra(InputContract.EXTRA_ORIGIN_PACKAGE)
        val sameWindow = originWindowId == null || root?.windowId == originWindowId
        val accepted = sameWindow && expectedOrigin == declaredOrigin && NativeVoicePolicy.canApplyResult(
            sessionGate,
            sessionId,
            SystemClock.elapsedRealtime(),
            expectedOrigin,
            currentPackage,
            result,
        )
        if (!accepted || root == null || expectedOrigin == null) {
            clearSession()
            if (!result.isNullOrBlank()) {
                Toast.makeText(this, R.string.native_voice_field_lost, Toast.LENGTH_SHORT).show()
            }
            return
        }

        val target = AndroidEditableTarget.find(root, expectedOrigin)
        val applied = target != null && AndroidEditableTarget.setTextAndSubmit(target, result!!.trim())
        clearSession()
        if (!applied) Toast.makeText(this, R.string.native_voice_field_lost, Toast.LENGTH_SHORT).show()
    }

    private fun clearSession() {
        sessionGate.clear()
        originPackage = null
        originWindowId = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        clearSession()
    }

    override fun onDestroy() {
        if (resultReceiverRegistered) {
            unregisterReceiver(resultReceiver)
            resultReceiverRegistered = false
        }
        clearSession()
        super.onDestroy()
    }
}
