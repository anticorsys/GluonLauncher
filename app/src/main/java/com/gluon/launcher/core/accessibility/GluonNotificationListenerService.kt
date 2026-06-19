// app/src/main/java/com/gluon/launcher/core/accessibility/GluonNotificationListenerService.kt
package com.gluon.launcher.core.accessibility

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.gluon.launcher.core.data.states.NotificationState

class GluonNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        updateCounts()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        updateCounts()
    }

    override fun onListenerConnected() {
        updateCounts()
    }

    override fun onListenerDisconnected() {
        NotificationState.notifications.value = emptyMap()
    }

    private fun updateCounts() {
        try {
            val active = activeNotifications ?: return
            val counts = mutableMapOf<String, Int>()
            for (n in active) {
                // Игнорируем не очищаемые системные фоновые уведомления
                if (!n.isOngoing) {
                    val pkg = n.packageName
                    counts[pkg] = (counts[pkg] ?: 0) + 1
                }
            }
            NotificationState.notifications.value = counts
        } catch (_: Exception) {
            // Игнорируем DeadObjectException или SecurityException при блокировке ОС
        }
    }
}