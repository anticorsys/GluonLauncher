// --- NotificationPanelAccessibilityService.kt ---
// app/src/main/java/com/gluon/launcher/core/accessibility/NotificationPanelAccessibilityService.kt
package com.gluon.launcher.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

// Использование Accessibility API обосновано и предназначено исключительно
// для вызова системной панели уведомлений по жесту пользователя в лаунчере.
@SuppressLint("DiscouragedApi", "AccessibilityPolicy", "AccessibilityService")
@Suppress("AccessibilityService", "AccessibilityUsage")
class NotificationPanelAccessibilityService : AccessibilityService() {

    companion object {
        private var instanceRef: WeakReference<NotificationPanelAccessibilityService>? = null

        fun isServiceEnabled(): Boolean = instanceRef?.get() != null

        fun openNotificationPanel() {
            instanceRef?.get()?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                ?: Log.w("Accessibility", "Service not running, cannot open notifications")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
        Log.d("Accessibility", "NotificationPanelAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // не требуется
    }

    override fun onInterrupt() {
        Log.d("Accessibility", "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instanceRef?.get() == this) {
            instanceRef?.clear()
            instanceRef = null
        }
    }
}