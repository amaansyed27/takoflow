package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.TakoFlowAppContainer
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

class GeneralSettingsViewModel(container: TakoFlowAppContainer) : ViewModel() {
    private val settings = container.settings
    private val systemStatus = container.systemStatus

    private val panelSettings = combine(
        settings.autoStartListening,
        settings.inferenceModel,
        settings.soundFeedback,
        settings.vibrationFeedback
    ) { autoStart, model, sound, vibration ->
        listOf(autoStart.toString(), model, sound.toString(), vibration.toString())
    }

    val uiState: StateFlow<GeneralSettingsUiState> = combine(
        panelSettings,
        systemStatus.status
    ) { values, status ->
        GeneralSettingsUiState(
            autoStart = values[0].toBoolean(),
            model = values[1],
            sound = values[2].toBoolean(),
            vibration = values[3].toBoolean(),
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

    fun resetSetup() {
        viewModelScope.launch { settings.setSetupCompleted(false) }
    }
}
