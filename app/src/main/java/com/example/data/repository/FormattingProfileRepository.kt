package com.example.data.repository

import com.example.speech.FormattingProfile
import com.example.speech.FormattingProfileStore
import kotlinx.coroutines.flow.StateFlow

class FormattingProfileRepository(private val store: FormattingProfileStore) {
    val profiles: StateFlow<List<FormattingProfile>> = store.profiles

    fun getProfile(id: String): FormattingProfile = store.getProfile(id)
    fun createProfile(): FormattingProfile = store.createProfile()
    fun save(profile: FormattingProfile) = store.save(profile)
    fun reset(profileId: String) = store.reset(profileId)
    fun delete(profileId: String): Boolean = store.delete(profileId)
    fun isBuiltIn(profileId: String): Boolean = FormattingProfileStore.isBuiltIn(profileId)
    fun builtInProfile(profileId: String): FormattingProfile = FormattingProfileStore.builtInProfile(profileId)
}
