package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.speech.SpeechModels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "takoflow_settings")

class TakoFlowPreferences(private val context: Context) {
    companion object {
        private val KEY_AUTO_START = booleanPreferencesKey("auto_start_listening")
        private val KEY_INFERENCE_MODEL = stringPreferencesKey("inference_model")
        private val KEY_PUNCTUATION = booleanPreferencesKey("punctuation")
        private val KEY_AUTO_CAPS = booleanPreferencesKey("auto_capitalization")
        private val KEY_SOUND_FEEDBACK = booleanPreferencesKey("sound_feedback")
        private val KEY_VIBRATION_FEEDBACK = booleanPreferencesKey("vibration_feedback")
        private val KEY_ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
        private val KEY_SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
    }

    val autoStartListening: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_AUTO_START] ?: false }

    val inferenceModel: Flow<String> =
        context.dataStore.data.map { it[KEY_INFERENCE_MODEL] ?: SpeechModels.VOSK }

    val punctuation: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_PUNCTUATION] ?: true }

    val autoCapitalization: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_AUTO_CAPS] ?: true }

    val soundFeedback: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_SOUND_FEEDBACK] ?: true }

    val vibrationFeedback: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_VIBRATION_FEEDBACK] ?: false }

    val activeProfileId: Flow<String> =
        context.dataStore.data.map { it[KEY_ACTIVE_PROFILE_ID] ?: "default" }

    val setupCompleted: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_SETUP_COMPLETED] ?: false }

    suspend fun setAutoStartListening(value: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_START] = value }
    }

    suspend fun setInferenceModel(value: String) {
        context.dataStore.edit { it[KEY_INFERENCE_MODEL] = value }
    }

    suspend fun setPunctuation(value: Boolean) {
        context.dataStore.edit { it[KEY_PUNCTUATION] = value }
    }

    suspend fun setAutoCapitalization(value: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_CAPS] = value }
    }

    suspend fun setSoundFeedback(value: Boolean) {
        context.dataStore.edit { it[KEY_SOUND_FEEDBACK] = value }
    }

    suspend fun setVibrationFeedback(value: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATION_FEEDBACK] = value }
    }

    suspend fun setActiveProfileId(value: String) {
        context.dataStore.edit { it[KEY_ACTIVE_PROFILE_ID] = value }
    }

    suspend fun setSetupCompleted(value: Boolean) {
        context.dataStore.edit { it[KEY_SETUP_COMPLETED] = value }
    }
}
