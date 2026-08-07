package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FormattingProfileRepository
import com.example.data.repository.SettingsRepository
import com.example.speech.FormattingProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfilesUiState(
    val profiles: List<FormattingProfile> = emptyList(),
    val activeId: String = "default"
)

class ProfilesViewModel(
    private val settings: SettingsRepository,
    private val profilesRepository: FormattingProfileRepository
) : ViewModel() {
    val uiState: StateFlow<ProfilesUiState> = combine(
        profilesRepository.profiles,
        settings.activeProfileId
    ) { profiles, activeId -> ProfilesUiState(profiles, activeId) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ProfilesUiState()
        )

    fun select(profileId: String) {
        viewModelScope.launch { settings.setActiveProfileId(profileId) }
    }

    fun create(): Result<FormattingProfile> = runCatching { profilesRepository.createProfile() }

    fun save(profile: FormattingProfile) {
        profilesRepository.save(profile)
    }

    fun resetOrDelete(profileId: String) {
        if (profilesRepository.isBuiltIn(profileId)) {
            profilesRepository.reset(profileId)
            return
        }

        profilesRepository.delete(profileId)
        if (uiState.value.activeId == profileId) {
            viewModelScope.launch { settings.setActiveProfileId("default") }
        }
    }

    fun isBuiltIn(profileId: String): Boolean = profilesRepository.isBuiltIn(profileId)
}
