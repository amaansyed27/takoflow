package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SpeechModelRepository
import com.example.speech.ModelDownloadState
import com.example.speech.SpeechModels
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class VoiceSettingsUiState(
    val selectedModel: String = SpeechModels.VOSK,
    val punctuation: Boolean = true,
    val autoCapitalization: Boolean = true,
    val vosk: ModelDownloadState = ModelDownloadState(),
    val whisper: ModelDownloadState = ModelDownloadState()
)

class VoiceSettingsViewModel(
    private val settings: SettingsRepository,
    private val models: SpeechModelRepository
) : ViewModel() {
    private val formatting = combine(
        settings.inferenceModel,
        settings.punctuation,
        settings.autoCapitalization
    ) { model, punctuation, caps -> Triple(model, punctuation, caps) }

    val uiState: StateFlow<VoiceSettingsUiState> = combine(
        formatting,
        models.voskState,
        models.whisperState
    ) { (model, punctuation, caps), vosk, whisper ->
        VoiceSettingsUiState(model, punctuation, caps, vosk, whisper)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        VoiceSettingsUiState()
    )

    init {
        viewModelScope.launch {
            while (isActive) {
                models.refresh()
                delay(1_000)
            }
        }
    }

    fun selectModel(model: String) {
        val installed = when (model) {
            SpeechModels.WHISPER_TINY -> models.whisperState.value.installed
            else -> models.voskState.value.installed
        }
        if (!installed) return
        viewModelScope.launch { settings.setInferenceModel(model) }
    }

    fun setPunctuation(enabled: Boolean) {
        viewModelScope.launch { settings.setPunctuation(enabled) }
    }

    fun setAutoCapitalization(enabled: Boolean) {
        viewModelScope.launch { settings.setAutoCapitalization(enabled) }
    }

    fun downloadVosk() = models.downloadVosk()
    fun cancelVoskDownload() = models.cancelVoskDownload()
    fun deleteVosk() = models.deleteVosk()
    fun downloadWhisper() = models.downloadWhisper()
    fun cancelWhisperDownload() = models.cancelWhisperDownload()
    fun deleteWhisper() = models.deleteWhisper()
}
