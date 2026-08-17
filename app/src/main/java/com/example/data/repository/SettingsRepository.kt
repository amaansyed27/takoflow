package com.example.data.repository

import com.example.data.TakoFlowPreferences
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val preferences: TakoFlowPreferences) {
    val autoStartListening: Flow<Boolean> = preferences.autoStartListening
    val inferenceModel: Flow<String> = preferences.inferenceModel
    val whisperMode: Flow<String> = preferences.whisperMode
    val punctuation: Flow<Boolean> = preferences.punctuation
    val autoCapitalization: Flow<Boolean> = preferences.autoCapitalization
    val grammarCorrection: Flow<Boolean> = preferences.grammarCorrection
    val spellCorrection: Flow<Boolean> = preferences.spellCorrection
    val soundFeedback: Flow<Boolean> = preferences.soundFeedback
    val vibrationFeedback: Flow<Boolean> = preferences.vibrationFeedback
    val activeProfileId: Flow<String> = preferences.activeProfileId
    val setupCompleted: Flow<Boolean> = preferences.setupCompleted

    suspend fun setAutoStartListening(value: Boolean) = preferences.setAutoStartListening(value)
    suspend fun setInferenceModel(value: String) = preferences.setInferenceModel(value)
    suspend fun setWhisperMode(value: String) = preferences.setWhisperMode(value)
    suspend fun setPunctuation(value: Boolean) = preferences.setPunctuation(value)
    suspend fun setAutoCapitalization(value: Boolean) = preferences.setAutoCapitalization(value)
    suspend fun setGrammarCorrection(value: Boolean) = preferences.setGrammarCorrection(value)
    suspend fun setSpellCorrection(value: Boolean) = preferences.setSpellCorrection(value)
    suspend fun setSoundFeedback(value: Boolean) = preferences.setSoundFeedback(value)
    suspend fun setVibrationFeedback(value: Boolean) = preferences.setVibrationFeedback(value)
    suspend fun setActiveProfileId(value: String) = preferences.setActiveProfileId(value)
    suspend fun setSetupCompleted(value: Boolean) = preferences.setSetupCompleted(value)
}
