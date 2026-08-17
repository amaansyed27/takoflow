package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SpeechModelRepository
import com.example.speech.ModelDownloadState
import com.example.speech.SpeechModels
import com.example.speech.WhisperModes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class VoiceSettingsUiState(
    val selectedModel: String = SpeechModels.VOSK,
    val whisperMode: String = WhisperModes.BATCH,
    val punctuation: Boolean = true,
    val autoCapitalization: Boolean = true,
    val grammarCorrection: Boolean = true,
    val spellCorrection: Boolean = true,
    val vosk: ModelDownloadState = ModelDownloadState(),
    val whisper: ModelDownloadState = ModelDownloadState()
)

private data class VoiceEngineSettings(
    val model: String,
    val whisperMode: String
)

private data class TextQualitySettings(
    val punctuation: Boolean,
    val capitalization: Boolean,
    val grammar: Boolean,
    val spelling: Boolean
)

class VoiceSettingsViewModel(
    private val settings: SettingsRepository,
    private val models: SpeechModelRepository
) : ViewModel() {
    private val engineSettings = combine(
        settings.inferenceModel,
        settings.whisperMode
    ) { model, whisperMode -> VoiceEngineSettings(model, whisperMode) }

    private val textQuality = combine(
        settings.punctuation,
        settings.autoCapitalization,
        settings.grammarCorrection,
        settings.spellCorrection
    ) { punctuation, caps, grammar, spelling ->
        TextQualitySettings(punctuation, caps, grammar, spelling)
    }

    val uiState: StateFlow<VoiceSettingsUiState> = combine(
        engineSettings,
        textQuality,
        models.voskState,
        models.whisperState
    ) { engine, quality, vosk, whisper ->
        VoiceSettingsUiState(
            selectedModel = engine.model,
            whisperMode = engine.whisperMode,
            punctuation = quality.punctuation,
            autoCapitalization = quality.capitalization,
            grammarCorrection = quality.grammar,
            spellCorrection = quality.spelling,
            vosk = vosk,
            whisper = whisper
        )
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

    fun setWhisperMode(mode: String) {
        viewModelScope.launch { settings.setWhisperMode(mode) }
    }

    fun setPunctuation(enabled: Boolean) {
        viewModelScope.launch { settings.setPunctuation(enabled) }
    }

    fun setAutoCapitalization(enabled: Boolean) {
        viewModelScope.launch { settings.setAutoCapitalization(enabled) }
    }

    fun setGrammarCorrection(enabled: Boolean) {
        viewModelScope.launch { settings.setGrammarCorrection(enabled) }
    }

    fun setSpellCorrection(enabled: Boolean) {
        viewModelScope.launch { settings.setSpellCorrection(enabled) }
    }

    fun downloadVosk() = models.downloadVosk()
    fun cancelVoskDownload() = models.cancelVoskDownload()
    fun deleteVosk() = models.deleteVosk()
    fun downloadWhisper() = models.downloadWhisper()
    fun cancelWhisperDownload() = models.cancelWhisperDownload()
    fun deleteWhisper() = models.deleteWhisper()
}
