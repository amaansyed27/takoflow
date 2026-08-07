package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FormattingProfileRepository
import com.example.data.repository.SettingsRepository
import com.example.speech.LocalSpeechEngine
import com.example.speech.SpeechModels
import com.example.speech.SpeechState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DictationUiState(
    val speechState: SpeechState = SpeechState.Idle,
    val rmsDb: Float = 0f,
    val model: String = SpeechModels.VOSK,
    val profileName: String = "Default",
    val text: String = ""
)

private data class EngineSettings(
    val model: String,
    val punctuation: Boolean,
    val autoCapitalization: Boolean,
    val sound: Boolean,
    val vibration: Boolean
)

private data class DictationConfiguration(
    val engine: EngineSettings,
    val profileId: String,
    val profileName: String
)

class DictationViewModel(
    private val settings: SettingsRepository,
    private val profiles: FormattingProfileRepository,
    private val speechEngine: LocalSpeechEngine
) : ViewModel() {
    private val text = MutableStateFlow("")

    private val engineSettings = combine(
        settings.inferenceModel,
        settings.punctuation,
        settings.autoCapitalization,
        settings.soundFeedback,
        settings.vibrationFeedback
    ) { model, punctuation, caps, sound, vibration ->
        EngineSettings(model, punctuation, caps, sound, vibration)
    }

    private val configuration = combine(
        engineSettings,
        settings.activeProfileId,
        profiles.profiles
    ) { engine, profileId, profileList ->
        DictationConfiguration(
            engine = engine,
            profileId = profileId,
            profileName = profileList.firstOrNull { it.id == profileId }?.name ?: "Default"
        )
    }

    val uiState: StateFlow<DictationUiState> = combine(
        speechEngine.speechState,
        speechEngine.rmsDb,
        configuration,
        text
    ) { speechState, rmsDb, config, currentText ->
        DictationUiState(
            speechState = speechState,
            rmsDb = rmsDb,
            model = config.engine.model,
            profileName = config.profileName,
            text = currentText
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DictationUiState()
    )

    init {
        viewModelScope.launch {
            configuration.collect { config ->
                speechEngine.activeModel = config.engine.model
                speechEngine.autoPunctuation = config.engine.punctuation
                speechEngine.autoCapitalization = config.engine.autoCapitalization
                speechEngine.soundFeedbackEnabled = config.engine.sound
                speechEngine.vibrationFeedbackEnabled = config.engine.vibration
                speechEngine.activeProfile = config.profileId
            }
        }

        viewModelScope.launch {
            speechEngine.speechState.collect { state ->
                if (state is SpeechState.Success) {
                    text.value = listOf(text.value.trim(), state.recognizedText)
                        .filter(String::isNotBlank)
                        .joinToString(" ")
                    speechEngine.acknowledgeResult()
                }
            }
        }
    }

    fun updateText(value: String) {
        text.value = value
    }

    fun onMicClick() {
        when (speechEngine.speechState.value) {
            is SpeechState.Listening -> speechEngine.stopListening()
            is SpeechState.Idle, is SpeechState.Error -> speechEngine.startListening()
            else -> Unit
        }
    }

    fun addSpace() {
        text.value += " "
    }

    fun deleteCharacter() {
        if (text.value.isNotEmpty()) text.value = text.value.dropLast(1)
    }

    fun addLineBreak() {
        text.value += "\n"
    }

    fun clear() {
        text.value = ""
    }

    override fun onCleared() {
        speechEngine.destroy()
        super.onCleared()
    }
}
