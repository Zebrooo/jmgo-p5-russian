package me.efesser.flauncher.voice

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
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
    private val retryHandler = Handler(Looper.getMainLooper())
    private var pendingResult: PendingResult? = null
    private val retryResult = Runnable { tryApplyPendingResult() }

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
        val now = SystemClock.elapsedRealtime()
        val expectedOrigin = originPackage
        val sessionId = intent.getStringExtra(InputContract.EXTRA_SESSION_ID)
        val declaredOrigin = intent.getStringExtra(InputContract.EXTRA_ORIGIN_PACKAGE)
        val result = NativeVoicePolicy.validatedResult(
            sessionGate,
            sessionId,
            now,
            expectedOrigin,
            declaredOrigin,
            intent.getStringExtra(InputContract.EXTRA_RESULT),
        )
        if (result == null || sessionId == null) {
            clearSession()
            return
        }

        pendingResult = PendingResult(sessionId, result, now + WINDOW_RESTORE_TIMEOUT_MS)
        tryApplyPendingResult()
    }

    private fun tryApplyPendingResult() {
        val pending = pendingResult ?: return
        val now = SystemClock.elapsedRealtime()
        val expectedOrigin = originPackage
        if (expectedOrigin == null || !sessionGate.isActive(now) || now > pending.deadlineMs) {
            clearSession()
            Toast.makeText(this, R.string.native_voice_field_lost, Toast.LENGTH_SHORT).show()
            return
        }

        val root = rootInActiveWindow
        val currentPackage = root?.packageName?.toString()
        val sameWindow = originWindowId == null || root?.windowId == originWindowId
        val target = if (root != null && currentPackage == expectedOrigin && sameWindow) {
            AndroidEditableTarget.find(root, expectedOrigin)
        } else {
            null
        }

        if (!NativeVoicePolicy.isWindowReady(
                expectedOrigin,
                currentPackage,
                sameWindow,
                target != null,
            )
        ) {
            scheduleRetry()
            return
        }

        if (AndroidEditableTarget.setTextAndSubmit(target!!, pending.text)) {
            sessionGate.accept(pending.sessionId, now)
            clearSession()
        } else {
            scheduleRetry()
        }
    }

    private fun scheduleRetry() {
        retryHandler.removeCallbacks(retryResult)
        retryHandler.postDelayed(retryResult, WINDOW_RETRY_INTERVAL_MS)
    }

    private fun clearSession() {
        retryHandler.removeCallbacks(retryResult)
        pendingResult = null
        sessionGate.clear()
        originPackage = null
        originWindowId = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (pendingResult != null) tryApplyPendingResult()
    }

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

    private data class PendingResult(
        val sessionId: String,
        val text: String,
        val deadlineMs: Long,
    )

    private companion object {
        const val WINDOW_RESTORE_TIMEOUT_MS = 5_000L
        const val WINDOW_RETRY_INTERVAL_MS = 100L
    }
}
