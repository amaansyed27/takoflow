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

data class DashboardUiState(
    val model: String = SpeechModels.VOSK,
    val profileName: String = "Default",
    val imeEnabled: Boolean = false,
    val imeSelected: Boolean = false,
    val microphoneGranted: Boolean = false,
    val selectedModelReady: Boolean = false
) {
    val ready: Boolean
        get() = imeEnabled && imeSelected && microphoneGranted && selectedModelReady
}

class DashboardViewModel(container: TakoFlowAppContainer) : ViewModel() {
    private val settings = container.settings
    private val models = container.models
    private val profiles = container.profiles
    private val systemStatus = container.systemStatus

    private val selection = combine(
        settings.inferenceModel,
        settings.activeProfileId
    ) { model, profileId -> model to profileId }

    private val modelStates = combine(models.voskState, models.whisperState) { vosk, whisper ->
        vosk to whisper
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        selection,
        modelStates,
        profiles.profiles,
        systemStatus.status
    ) { (model, profileId), (vosk, whisper), profileList, status ->
        DashboardUiState(
            model = model,
            profileName = profileList.firstOrNull { it.id == profileId }?.name ?: "Default",
            imeEnabled = status.imeEnabled,
            imeSelected = status.imeSelected,
            microphoneGranted = status.microphoneGranted,
            selectedModelReady = if (model == SpeechModels.WHISPER_TINY) {
                whisper.installed
            } else {
                vosk.installed
            }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DashboardUiState()
    )

    init {
        viewModelScope.launch {
            while (isActive) {
                systemStatus.refresh()
                models.refresh()
                delay(1_000)
            }
        }
    }
}
