package com.example.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.service.ImeStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SystemStatus(
    val imeEnabled: Boolean = false,
    val imeSelected: Boolean = false,
    val microphoneGranted: Boolean = false
)

class SystemStatusRepository(private val context: Context) {
    private val _status = MutableStateFlow(snapshot())
    val status: StateFlow<SystemStatus> = _status.asStateFlow()

    fun refresh() {
        _status.value = snapshot()
    }

    private fun snapshot(): SystemStatus = SystemStatus(
        imeEnabled = ImeStatus.isEnabled(context),
        imeSelected = ImeStatus.isSelected(context),
        microphoneGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    )
}
