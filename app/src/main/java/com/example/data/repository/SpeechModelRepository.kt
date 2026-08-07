package com.example.data.repository

import com.example.speech.ModelDownloadState
import com.example.speech.SpeechModelManager
import kotlinx.coroutines.flow.StateFlow

class SpeechModelRepository(private val manager: SpeechModelManager) {
    val voskState: StateFlow<ModelDownloadState> = manager.voskState
    val whisperState: StateFlow<ModelDownloadState> = manager.whisperState

    fun refresh() = manager.refresh()
    fun downloadVosk() = manager.downloadVosk()
    fun cancelVoskDownload() = manager.cancelVoskDownload()
    fun deleteVosk() = manager.deleteVosk()
    fun downloadWhisper() = manager.downloadWhisper()
    fun cancelWhisperDownload() = manager.cancelWhisperDownload()
    fun deleteWhisper() = manager.deleteWhisper()
    fun isVoskInstalled(): Boolean = manager.isVoskInstalled()
    fun isWhisperInstalled(): Boolean = manager.isWhisperInstalled()
}
