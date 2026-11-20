package com.zerowisp.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zerowisp.app.data.DownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ModelDownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "zerowisp_download_channel"
        const val NOTIFICATION_ID = 1002
        const val ACTION_DOWNLOAD = "com.zerowisp.app.DOWNLOAD"
        const val ACTION_CANCEL = "com.zerowisp.app.CANCEL_DOWNLOAD"
        const val EXTRA_MODEL_ID = "model_id"
        const val EXTRA_MODEL_URL = "model_url"
        const val EXTRA_MODEL_FILENAME = "model_filename"

        val downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var downloadManager: DownloadManager

    override fun onCreate() {
        super.onCreate()
        downloadManager = DownloadManager(applicationContext)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Preparing download…", 0))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DOWNLOAD -> {
                val modelId = intent.getStringExtra(EXTRA_MODEL_ID) ?: return START_NOT_STICKY
                val url = intent.getStringExtra(EXTRA_MODEL_URL) ?: return START_NOT_STICKY
                val filename = intent.getStringExtra(EXTRA_MODEL_FILENAME) ?: return START_NOT_STICKY
                startDownload(modelId, url, filename)
            }
            ACTION_CANCEL -> {
                downloadProgress.value = emptyMap()
                downloadManager.cancelDownload()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startDownload(modelId: String, url: String, filename: String) {
        scope.launch {
            downloadProgress.value = mapOf(modelId to 0)
            downloadManager.download(
                url = url,
                filename = filename,
                onProgress = { progress ->
                    downloadProgress.value = mapOf(modelId to progress)
                    updateNotification("Downloading $filename…", progress)
                },
                onComplete = {
                    downloadProgress.value = emptyMap()
                    updateNotification("Download complete!", 100)
                    stopSelf()
                },
                onError = { error ->
                    downloadProgress.value = emptyMap()
                    updateNotification("Download failed: $error", 0)
                    stopSelf()
                }
            )
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Model Downloads", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String, progress: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Zero Wisp")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String, progress: Int) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text, progress))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
