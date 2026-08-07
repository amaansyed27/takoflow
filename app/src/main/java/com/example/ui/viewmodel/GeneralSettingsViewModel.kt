package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SystemStatusRepository
import com.example.speech.SpeechModels
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class GeneralSettingsUiState(
    val autoStart: Boolean = false,
    val model: String = SpeechModels.VOSK,
    val sound: Boolean = true,
    val vibration: Boolean = false,
    val imeEnabled: Boolean = false,
    val imeSelected: Boolean = false
)

private data class VoicePanelSettings(
    val autoStart: Boolean,
    val model: String,
    val sound: Boolean,
    val vibration: Boolean
)

class GeneralSettingsViewModel(
    private val settings: SettingsRepository,
    private val systemStatus: SystemStatusRepository
) : ViewModel() {
    private val panelSettings = combine(
        settings.autoStartListening,
        settings.inferenceModel,
        settings.soundFeedback,
        settings.vibrationFeedback
    ) { autoStart, model, sound, vibration ->
        VoicePanelSettings(autoStart, model, sound, vibration)
    }

    val uiState: StateFlow<GeneralSettingsUiState> = combine(
        panelSettings,
        systemStatus.status
    ) { panel, status ->
        GeneralSettingsUiState(
            autoStart = panel.autoStart,
            model = panel.model,
            sound = panel.sound,
            vibration = panel.vibration,
            imeEnabled = status.imeEnabled,
            imeSelected = status.imeSelected
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        GeneralSettingsUiState()
    )

    init {
        viewModelScope.launch {
            while (isActive) {
                systemStatus.refresh()
                delay(1_000)
            }
        }
    }

    fun setAutoStart(enabled: Boolean) {
        viewModelScope.launch { settings.setAutoStartListening(enabled) }
    }

    fun setSound(enabled: Boolean) {
        viewModelScope.launch { settings.setSoundFeedback(enabled) }
    }

    fun setVibration(enabled: Boolean) {
        viewModelScope.launch { settings.setVibrationFeedback(enabled) }
    }
}
