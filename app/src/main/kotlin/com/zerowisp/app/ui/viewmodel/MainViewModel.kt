package com.zerowisp.app.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerowisp.app.data.AppSettings
import com.zerowisp.app.data.DownloadManager
import com.zerowisp.app.data.HistoryRepository
import com.zerowisp.app.data.ModelCatalog
import com.zerowisp.app.data.ModelInfo
import com.zerowisp.app.data.ZeroWispSettings
import com.zerowisp.app.service.FloatingBubbleService
import com.zerowisp.app.service.ModelDownloadService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class ModelUiState(
    val model: ModelInfo,
    val isDownloaded: Boolean,
    val isActive: Boolean,
    val downloadProgress: Int = -1 // -1 = not downloading
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appSettings = AppSettings(application)
    private val historyRepo = HistoryRepository(application)

    val settings: StateFlow<ZeroWispSettings> = appSettings.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ZeroWispSettings())

    val history = historyRepo.entriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadProgress: StateFlow<Map<String, Int>> = ModelDownloadService.downloadProgress.asStateFlow()

    fun getModelStates(): List<ModelUiState> {
        val ctx = getApplication<Application>()
        val modelsDir = File(ctx.filesDir, "models")
        val activeId = settings.value.activeModelId
        return ModelCatalog.models.map { model ->
            val file = File(modelsDir, model.filename)
            ModelUiState(
                model = model,
                isDownloaded = file.exists() && file.length() > 1024,
                isActive = model.id == activeId,
                downloadProgress = downloadProgress.value[model.id] ?: -1
            )
        }
    }

    fun startDownload(model: ModelInfo) {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, ModelDownloadService::class.java).apply {
            action = ModelDownloadService.ACTION_DOWNLOAD
            putExtra(ModelDownloadService.EXTRA_MODEL_ID, model.id)
            putExtra(ModelDownloadService.EXTRA_MODEL_URL, model.url)
            putExtra(ModelDownloadService.EXTRA_MODEL_FILENAME, model.filename)
        }
        ctx.startForegroundService(intent)
    }

    fun cancelDownload(modelId: String) {
        val ctx = getApplication<Application>()
        ModelDownloadService.downloadProgress.value = emptyMap()
        ctx.startService(Intent(ctx, ModelDownloadService::class.java).apply {
            action = ModelDownloadService.ACTION_CANCEL
        })
    }

    fun deleteModel(model: ModelInfo) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val file = File(File(ctx.filesDir, "models"), model.filename)
            file.delete()
            if (settings.value.activeModelId == model.id) {
                appSettings.clearActiveModel()
            }
        }
    }

    fun selectModel(model: ModelInfo) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val path = File(File(ctx.filesDir, "models"), model.filename).absolutePath
            appSettings.setActiveModel(model.id, path)
        }
    }

    fun setAlwaysOutputEnglish(value: Boolean) {
        viewModelScope.launch { appSettings.setAlwaysOutputEnglish(value) }
    }

    fun setCleanFillerWords(value: Boolean) {
        viewModelScope.launch { appSettings.setCleanFillerWords(value) }
    }

    fun setAutoChunk(value: Boolean) {
        viewModelScope.launch { appSettings.setAutoChunk(value) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepo.clearAll() }
    }

    fun startBubbleService() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, FloatingBubbleService::class.java)
        ctx.startForegroundService(intent)
    }

    fun stopBubbleService() {
        val ctx = getApplication<Application>()
        ctx.stopService(Intent(ctx, FloatingBubbleService::class.java))
    }
}
