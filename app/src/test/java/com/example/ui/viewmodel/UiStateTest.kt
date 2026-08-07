package com.example.ui.viewmodel

import com.example.speech.ModelDownloadState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiStateTest {
    @Test
    fun dashboardIsReadyOnlyWhenEveryRequirementIsReady() {
        assertTrue(
            DashboardUiState(
                imeEnabled = true,
                imeSelected = true,
                microphoneGranted = true,
                selectedModelReady = true
            ).ready
        )

        assertFalse(
            DashboardUiState(
                imeEnabled = true,
                imeSelected = true,
                microphoneGranted = false,
                selectedModelReady = true
            ).ready
        )
    }

    @Test
    fun setupRequiresInstalledVoskAndAndroidPermissions() {
        val installed = ModelDownloadState(installed = true)
        assertTrue(
            SetupUiState(
                imeEnabled = true,
                imeSelected = true,
                microphoneGranted = true,
                vosk = installed
            ).ready
        )

        assertFalse(
            SetupUiState(
                imeEnabled = true,
                imeSelected = true,
                microphoneGranted = true,
                vosk = ModelDownloadState(installed = false)
            ).ready
        )
    }
}
