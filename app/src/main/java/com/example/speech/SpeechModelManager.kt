package com.example.speech

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object SpeechModels {
    const val VOSK = "Vosk"
    const val WHISPER_TINY = "Whisper Tiny"
}

data class ModelDownloadState(
    val installed: Boolean = false,
    val downloading: Boolean = false,
    val progressPercent: Int = 0,
    val error: String? = null
)

class SpeechModelManager private constructor(private val context: Context) {
    companion object {
        private const val VOSK_URL =
            "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
        private const val WHISPER_URL =
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin"

        @Volatile
        private var instance: SpeechModelManager? = null

        fun get(context: Context): SpeechModelManager =
            instance ?: synchronized(this) {
                instance ?: SpeechModelManager(context.applicationContext).also { instance = it }
            }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder().build()
    private val modelsDir = File(context.filesDir, "speech-models").apply { mkdirs() }

    val voskModelDir: File = File(modelsDir, "vosk-model-small-en-us-0.15")
    val whisperModelFile: File = File(modelsDir, "ggml-tiny.en.bin")

    private val _voskState = MutableStateFlow(ModelDownloadState())
    val voskState: StateFlow<ModelDownloadState> = _voskState.asStateFlow()

    private val _whisperState = MutableStateFlow(ModelDownloadState())
    val whisperState: StateFlow<ModelDownloadState> = _whisperState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (!_voskState.value.downloading) {
            val installed = isVoskInstalled()
            _voskState.value = ModelDownloadState(
                installed = installed,
                progressPercent = if (installed) 100 else 0
            )
        }
        if (!_whisperState.value.downloading) {
            val installed = isWhisperInstalled()
            _whisperState.value = ModelDownloadState(
                installed = installed,
                progressPercent = if (installed) 100 else 0
            )
        }
    }

    fun isVoskInstalled(): Boolean =
        voskModelDir.isDirectory &&
            File(voskModelDir, "am/final.mdl").isFile &&
            File(voskModelDir, "conf/model.conf").isFile

    fun isWhisperInstalled(): Boolean =
        whisperModelFile.isFile && whisperModelFile.length() > 70L * 1024L * 1024L

    fun downloadVosk() {
        if (_voskState.value.downloading || isVoskInstalled()) return
        scope.launch {
            val archive = File(context.cacheDir, "vosk-model.zip.part")
            try {
                archive.delete()
                updateVosk(downloading = true, progress = 0, error = null)
                downloadFile(VOSK_URL, archive) { updateVosk(true, it, null) }
                voskModelDir.deleteRecursively()
                unzipSafely(archive, modelsDir)
                check(isVoskInstalled()) { "Downloaded Vosk archive did not contain a valid model." }
                updateVosk(downloading = false, progress = 100, error = null, installed = true)
            } catch (error: Exception) {
                voskModelDir.deleteRecursively()
                updateVosk(false, 0, error.message ?: "Vosk download failed", false)
            } finally {
                archive.delete()
            }
        }
    }

    fun downloadWhisper() {
        if (_whisperState.value.downloading || isWhisperInstalled()) return
        scope.launch {
            val partial = File(modelsDir, "ggml-tiny.en.bin.part")
            try {
                partial.delete()
                updateWhisper(downloading = true, progress = 0, error = null)
                downloadFile(WHISPER_URL, partial) { updateWhisper(true, it, null) }
                check(partial.length() > 70L * 1024L * 1024L) {
                    "Downloaded Whisper model is incomplete."
                }
                if (whisperModelFile.exists()) whisperModelFile.delete()
                check(partial.renameTo(whisperModelFile)) { "Could not install Whisper model." }
                updateWhisper(false, 100, null, true)
            } catch (error: Exception) {
                partial.delete()
                updateWhisper(false, 0, error.message ?: "Whisper download failed", false)
            }
        }
    }

    fun deleteWhisper() {
        if (_whisperState.value.downloading) return
        whisperModelFile.delete()
        updateWhisper(false, 0, null, false)
    }

    private fun downloadFile(url: String, destination: File, onProgress: (Int) -> Unit) {
        destination.parentFile?.mkdirs()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Download failed with HTTP ${response.code}." }
            val body = response.body ?: error("Download returned an empty response.")
            val total = body.contentLength()
            var copied = 0L
            body.byteStream().use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (total > 0) {
                            onProgress(((copied * 100L) / total).toInt().coerceIn(0, 99))
                        }
                    }
                    output.fd.sync()
                }
            }
        }
    }

    private fun unzipSafely(archive: File, destination: File) {
        val destinationPath = destination.canonicalPath + File.separator
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val output = File(destination, entry.name)
                check(output.canonicalPath.startsWith(destinationPath)) {
                    "Unsafe path in downloaded model archive."
                }
                if (entry.isDirectory) {
                    output.mkdirs()
                } else {
                    output.parentFile?.mkdirs()
                    output.outputStream().buffered().use { zip.copyTo(it) }
                }
                zip.closeEntry()
            }
        }
    }

    private fun updateVosk(
        downloading: Boolean,
        progress: Int,
        error: String?,
        installed: Boolean = isVoskInstalled()
    ) {
        _voskState.value = ModelDownloadState(installed, downloading, progress, error)
    }

    private fun updateWhisper(
        downloading: Boolean,
        progress: Int,
        error: String?,
        installed: Boolean = isWhisperInstalled()
    ) {
        _whisperState.value = ModelDownloadState(installed, downloading, progress, error)
    }
}
