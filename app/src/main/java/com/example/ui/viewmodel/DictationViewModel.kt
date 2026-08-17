package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FormattingProfileRepository
import com.example.data.repository.SettingsRepository
import com.example.speech.LocalSpeechEngine
import com.example.speech.SpeechModels
import com.example.speech.SpeechState
import com.example.speech.WhisperModes
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
    val whisperMode: String = WhisperModes.BATCH,
    val profileName: String = "Default",
    val text: String = ""
)

private data class RecognitionSettings(
    val model: String,
    val whisperMode: String
)

private data class CorrectionSettings(
    val punctuation: Boolean,
    val autoCapitalization: Boolean,
    val grammar: Boolean,
    val spelling: Boolean
)

private data class FeedbackSettings(
    val sound: Boolean,
    val vibration: Boolean
)

private data class EngineSettings(
    val recognition: RecognitionSettings,
    val corrections: CorrectionSettings,
    val feedback: FeedbackSettings
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

    private val recognitionSettings = combine(
        settings.inferenceModel,
        settings.whisperMode
    ) { model, whisperMode -> RecognitionSettings(model, whisperMode) }

    private val correctionSettings = combine(
        settings.punctuation,
        settings.autoCapitalization,
        settings.grammarCorrection,
        settings.spellCorrection
    ) { punctuation, caps, grammar, spelling ->
        CorrectionSettings(punctuation, caps, grammar, spelling)
    }

    private val feedbackSettings = combine(
        settings.soundFeedback,
        settings.vibrationFeedback
    ) { sound, vibration -> FeedbackSettings(sound, vibration) }

    private val engineSettings = combine(
        recognitionSettings,
        correctionSettings,
        feedbackSettings
    ) { recognition, corrections, feedback ->
        EngineSettings(recognition, corrections, feedback)
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
            model = config.engine.recognition.model,
            whisperMode = config.engine.recognition.whisperMode,
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
                speechEngine.activeModel = config.engine.recognition.model
                speechEngine.whisperMode = config.engine.recognition.whisperMode
                speechEngine.autoPunctuation = config.engine.corrections.punctuation
                speechEngine.autoCapitalization = config.engine.corrections.autoCapitalization
                speechEngine.grammarCorrectionEnabled = config.engine.corrections.grammar
                speechEngine.spellCorrectionEnabled = config.engine.corrections.spelling
                speechEngine.soundFeedbackEnabled = config.engine.feedback.sound
                speechEngine.vibrationFeedbackEnabled = config.engine.feedback.vibration
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
