// app/src/main/java/com/gluon/launcher/core/predictive/PredictiveDockManager.kt
package com.gluon.launcher.core.predictive

import android.Manifest
import android.app.usage.UsageStatsManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class PredictiveDockManager(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("gluon_predictive_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val hasUsageStatsPermission: Boolean
        get() = try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            usageStatsManager?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, System.currentTimeMillis() - 1000, System.currentTimeMillis())
            true
        } catch (_: Exception) { false }

    private val _isPredictiveEnabled = MutableStateFlow(sharedPrefs.getBoolean("enabled", false) && hasUsageStatsPermission)
    val isPredictiveEnabled: StateFlow<Boolean> = _isPredictiveEnabled

    private val _predictedApps = MutableStateFlow<List<String>>(emptyList())
    val predictedApps: StateFlow<List<String>> = _predictedApps

    private var connectedBluetoothDeviceName: String? = null

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return

            when (intent?.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    connectedBluetoothDeviceName = device?.name
                    updatePredictions()
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    connectedBluetoothDeviceName = null
                    updatePredictions()
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        // ИСПРАВЛЕНИЕ: Изменен RECEIVER_NOT_EXPORTED на RECEIVER_EXPORTED
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.RECEIVER_EXPORTED else 0
        ContextCompat.registerReceiver(context, bluetoothReceiver, filter, flags)

        updatePredictions()
    }

    fun setPredictiveEnabled(enabled: Boolean) {
        val actualEnabled = enabled && hasUsageStatsPermission
        sharedPrefs.edit { putBoolean("enabled", actualEnabled) }
        _isPredictiveEnabled.value = actualEnabled
        updatePredictions()
    }

    fun updatePredictions() {
        scope.launch {
            if (!_isPredictiveEnabled.value || !hasUsageStatsPermission) {
                _predictedApps.value = emptyList()
                return@launch
            }

            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return@launch
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -7)

            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, calendar.timeInMillis, endTime)

            val sortedApps = stats.asSequence()
                .filter { it.totalTimeInForeground > 0 }
                .sortedByDescending { it.totalTimeInForeground }
                .map { it.packageName }
                .distinct()
                .take(5)
                .toList()

            _predictedApps.value = sortedApps
        }
    }

    fun unregister() {
        try { context.unregisterReceiver(bluetoothReceiver) } catch (_: Exception) {}
        scope.cancel()
    }
}