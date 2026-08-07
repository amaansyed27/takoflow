package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    private val settings: SettingsRepository
) : ViewModel() {
    val setupCompleted: StateFlow<Boolean> = settings.setupCompleted.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false
    )

    fun setSetupCompleted(value: Boolean) {
        viewModelScope.launch { settings.setSetupCompleted(value) }
    }
}
