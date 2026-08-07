package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.TakoFlowAppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(container: TakoFlowAppContainer) : ViewModel() {
    private val settings = container.settings

    val setupCompleted: StateFlow<Boolean> = settings.setupCompleted.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false
    )

    fun setSetupCompleted(value: Boolean) {
        viewModelScope.launch { settings.setSetupCompleted(value) }
    }

    fun setInferenceModel(model: String) {
        viewModelScope.launch { settings.setInferenceModel(model) }
    }
}
