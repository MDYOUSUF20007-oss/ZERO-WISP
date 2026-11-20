package com.zerowisp.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class DownloadManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var cancelled = false

    fun cancelDownload() {
        cancelled = true
        client.dispatcher.cancelAll()
    }

    suspend fun download(
        url: String,
        filename: String,
        onProgress: (Int) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        cancelled = false
        val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
        val tmpFile = File(modelsDir, "$filename.tmp")
        val finalFile = File(modelsDir, filename)

        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                onError("HTTP ${response.code}")
                return@withContext
            }

            val body = response.body ?: run {
                onError("Empty response body")
                return@withContext
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            FileOutputStream(tmpFile).use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (cancelled) {
                            tmpFile.delete()
                            return@withContext
                        }
                        out.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                            onProgress(progress)
                        }
                    }
                }
            }

            // Atomic rename
            if (tmpFile.renameTo(finalFile)) {
                onComplete()
            } else {
                tmpFile.delete()
                onError("Failed to save file")
            }

        } catch (e: Exception) {
            tmpFile.delete()
            if (!cancelled) {
                onError(e.message ?: "Unknown error")
            }
        }
    }
}
