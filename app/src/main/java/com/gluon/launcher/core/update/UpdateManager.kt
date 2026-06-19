// app/src/main/java/com/gluon/launcher/core/update/UpdateManager.kt
package com.gluon.launcher.core.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.gluon.launcher.core.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val changelog: String
)

class UpdateManager(private val context: Context) {
    private val client = OkHttpClient()

    private val updateJsonUrl = "https://gluoncore.ddns.net/update.json"

    fun shouldCheckForUpdatesAutomated(): Boolean {
        val prefs = context.getSharedPreferences("gluon_updates", Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong("last_check", 0)
        val now = System.currentTimeMillis()
        if (now - lastCheck > 24 * 60 * 60 * 1000) { // Раз в 24 часа
            prefs.edit { putLong("last_check", now) }
            return true
        }
        return false
    }

    suspend fun checkForUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(updateJsonUrl).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val serverVersionCode = json.getInt("versionCode")

                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode
                }

                if (serverVersionCode > currentVersionCode) {
                    return@withContext UpdateInfo(
                        versionCode = serverVersionCode,
                        versionName = json.getString("versionName"),
                        downloadUrl = json.getString("downloadUrl"),
                        changelog = json.optString("changelog", "Доступна новая версия!")
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    fun downloadAndInstall(updateInfo: UpdateInfo) {
        context.toast("Началось скачивание обновления...")
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = updateInfo.downloadUrl.toUri()

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        try {
            downloadsDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("gluon_update_") && file.name.endsWith(".apk")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val fileName = "gluon_update_${updateInfo.versionName}.apk"

        val request = DownloadManager.Request(uri)
            .setTitle("Обновление Gluon Launcher")
            .setDescription("Скачивание версии ${updateInfo.versionName}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    val installUri = downloadManager.getUriForDownloadedFile(downloadId)
                    if (installUri != null) {
                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(installUri, "application/vnd.android.package-archive")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                        }
                        try {
                            context.startActivity(installIntent)
                        } catch (_: Exception) {
                            context.toast("Разрешите установку неизвестных приложений в настройках и откройте APK в Загрузках", 1)
                        }
                    }
                    context.unregisterReceiver(this)
                }
            }
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.RECEIVER_EXPORTED else 0
        ContextCompat.registerReceiver(context, onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), flags)
    }
}