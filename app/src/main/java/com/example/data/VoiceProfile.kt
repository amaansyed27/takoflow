package com.example.data

data class VoiceProfile(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconName: String,
    val isActive: Boolean = false,
    val isDefault: Boolean = false
)
