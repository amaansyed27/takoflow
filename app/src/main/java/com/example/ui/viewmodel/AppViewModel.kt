package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    private val settings: SettingsRepository
) : ViewModel() {
    val setupCompleted: StateFlow<Boolean?> = settings.setupCompleted
        .map<Boolean, Boolean?> { it }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    fun setSetupCompleted(value: Boolean) {
        viewModelScope.launch { settings.setSetupCompleted(value) }
    }

    fun setInferenceModel(model: String) {
        viewModelScope.launch { settings.setInferenceModel(model) }
    }
}
