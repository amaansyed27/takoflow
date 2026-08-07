package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.TakoFlowAppContainer
import com.example.speech.ModelDownloadState
import com.example.speech.SpeechModels
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SetupUiState(
    val imeEnabled: Boolean = false,
    val imeSelected: Boolean = false,
    val microphoneGranted: Boolean = false,
    val vosk: ModelDownloadState = ModelDownloadState()
) {
    val ready: Boolean
        get() = imeEnabled && imeSelected && microphoneGranted && vosk.installed
}

class SetupViewModel(container: TakoFlowAppContainer) : ViewModel() {
    private val settings = container.settings
    private val models = container.models
    private val systemStatus = container.systemStatus

    val uiState: StateFlow<SetupUiState> = combine(
        systemStatus.status,
        models.voskState
    ) { status, vosk ->
        SetupUiState(
            imeEnabled = status.imeEnabled,
            imeSelected = status.imeSelected,
            microphoneGranted = status.microphoneGranted,
            vosk = vosk
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SetupUiState()
    )

    init {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(800)
            }
        }
    }

    fun refresh() {
        systemStatus.refresh()
        models.refresh()
    }

    fun downloadVosk() = models.downloadVosk()
    fun cancelVoskDownload() = models.cancelVoskDownload()

    fun selectDefaultModel() {
        viewModelScope.launch { settings.setInferenceModel(SpeechModels.VOSK) }
    }
}
