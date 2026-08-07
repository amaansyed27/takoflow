package com.example.core

import android.content.Context
import com.example.data.TakoFlowPreferences
import com.example.data.repository.FormattingProfileRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SpeechModelRepository
import com.example.data.repository.SystemStatusRepository
import com.example.speech.FormattingProfileStore
import com.example.speech.LocalSpeechEngine
import com.example.speech.SpeechModelManager

class TakoFlowAppContainer private constructor(private val appContext: Context) {
    val settings = SettingsRepository(TakoFlowPreferences(appContext))
    val models = SpeechModelRepository(SpeechModelManager.get(appContext))
    val profiles = FormattingProfileRepository(FormattingProfileStore.get(appContext))
    val systemStatus = SystemStatusRepository(appContext)

    fun createSpeechEngine(): LocalSpeechEngine = LocalSpeechEngine(appContext)

    companion object {
        @Volatile private var instance: TakoFlowAppContainer? = null

        fun get(context: Context): TakoFlowAppContainer =
            instance ?: synchronized(this) {
                instance ?: TakoFlowAppContainer(context.applicationContext).also { instance = it }
            }
    }
}
