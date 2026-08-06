package com.example.speech

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

object SpeechModels {
    const val VOSK = "Vosk"
    const val WHISPER_TINY = "Whisper Tiny"
}

data class ModelDownloadState(
    val installed: Boolean = false,
    val downloading: Boolean = false,
    val progressPercent: Int = 0,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val error: String? = null
)

class SpeechModelManager private constructor(private val context: Context) {
    companion object {
        private const val VOSK_URL =
            "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
        private const val WHISPER_URL =
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin"
        private const val MAX_VOSK_EXPANDED_BYTES = 250L * 1024L * 1024L

        @Volatile private var instance: SpeechModelManager? = null

        fun get(context: Context): SpeechModelManager =
            instance ?: synchronized(this) {
                instance ?: SpeechModelManager(context.applicationContext).also { instance = it }
            }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    private val modelsDir = File(context.filesDir, "speech-models").apply { mkdirs() }

    val voskModelDir: File = File(modelsDir, "vosk-model-small-en-us-0.15")
    val whisperModelFile: File = File(modelsDir, "ggml-tiny.en.bin")
    private val voskArchive = File(context.cacheDir, "vosk-model.zip.part")
    private val whisperPartial = File(modelsDir, "ggml-tiny.en.bin.part")

    private val _voskState = MutableStateFlow(ModelDownloadState())
    val voskState: StateFlow<ModelDownloadState> = _voskState.asStateFlow()
    private val _whisperState = MutableStateFlow(ModelDownloadState())
    val whisperState: StateFlow<ModelDownloadState> = _whisperState.asStateFlow()

    private var voskJob: Job? = null
    private var whisperJob: Job? = null
    @Volatile private var voskCall: Call? = null
    @Volatile private var whisperCall: Call? = null

    init {
        if (!isVoskInstalled()) voskArchive.delete()
        if (!isWhisperInstalled()) whisperPartial.delete()
        refresh()
    }

    fun refresh() {
        if (!_voskState.value.downloading) {
            val installed = isVoskInstalled()
            _voskState.value = ModelDownloadState(
                installed = installed,
                progressPercent = if (installed) 100 else 0,
                downloadedBytes = if (installed) directorySize(voskModelDir) else 0
            )
        }
        if (!_whisperState.value.downloading) {
            val installed = isWhisperInstalled()
            _whisperState.value = ModelDownloadState(
                installed = installed,
                progressPercent = if (installed) 100 else 0,
                downloadedBytes = if (installed) whisperModelFile.length() else 0,
                totalBytes = if (installed) whisperModelFile.length() else 0
            )
        }
    }

    fun isVoskInstalled(): Boolean =
        voskModelDir.isDirectory && File(voskModelDir, "am/final.mdl").isFile &&
            File(voskModelDir, "conf/model.conf").isFile

    fun isWhisperInstalled(): Boolean =
        whisperModelFile.isFile && whisperModelFile.length() > 70L * 1024L * 1024L

    fun downloadVosk() {
        if (_voskState.value.downloading || isVoskInstalled()) return
        voskJob = scope.launch {
            try {
                voskArchive.delete()
                updateVosk(true, 0, null)
                downloadFile(VOSK_URL, voskArchive, { voskCall = it }) { copied, total ->
                    updateVosk(true, progress(copied, total), null, downloadedBytes = copied, totalBytes = total)
                }
                currentCoroutineContext().ensureActive()
                voskModelDir.deleteRecursively()
                unzipSafely(voskArchive, modelsDir)
                check(isVoskInstalled()) { "Downloaded Vosk archive did not contain a valid model." }
                updateVosk(false, 100, null, true, directorySize(voskModelDir))
            } catch (error: Throwable) {
                voskModelDir.deleteRecursively()
                val cancelled = error is CancellationException || !currentCoroutineContext().isActive
                updateVosk(false, 0, if (cancelled) null else readableDownloadError(error), false)
            } finally {
                voskCall = null
                voskArchive.delete()
            }
        }
    }

    fun cancelVoskDownload() {
        voskCall?.cancel()
        voskJob?.cancel()
        voskArchive.delete()
        updateVosk(false, 0, null, isVoskInstalled())
    }

    fun deleteVosk() {
        cancelVoskDownload()
        voskModelDir.deleteRecursively()
        updateVosk(false, 0, null, false)
    }

    fun downloadWhisper() {
        if (_whisperState.value.downloading || isWhisperInstalled()) return
        whisperJob = scope.launch {
            try {
                whisperPartial.delete()
                updateWhisper(true, 0, null)
                downloadFile(WHISPER_URL, whisperPartial, { whisperCall = it }) { copied, total ->
                    updateWhisper(true, progress(copied, total), null, downloadedBytes = copied, totalBytes = total)
                }
                currentCoroutineContext().ensureActive()
                check(whisperPartial.length() > 70L * 1024L * 1024L) { "Downloaded Whisper model is incomplete." }
                if (whisperModelFile.exists()) whisperModelFile.delete()
                if (!whisperPartial.renameTo(whisperModelFile)) {
                    whisperPartial.copyTo(whisperModelFile, overwrite = true)
                    whisperPartial.delete()
                }
                check(isWhisperInstalled()) { "Could not install Whisper model." }
                updateWhisper(false, 100, null, true, whisperModelFile.length(), whisperModelFile.length())
            } catch (error: Throwable) {
                val cancelled = error is CancellationException || !currentCoroutineContext().isActive
                updateWhisper(false, 0, if (cancelled) null else readableDownloadError(error), false)
            } finally {
                whisperCall = null
                whisperPartial.delete()
            }
        }
    }

    fun cancelWhisperDownload() {
        whisperCall?.cancel()
        whisperJob?.cancel()
        whisperPartial.delete()
        updateWhisper(false, 0, null, isWhisperInstalled())
    }

    fun deleteWhisper() {
        cancelWhisperDownload()
        whisperModelFile.delete()
        updateWhisper(false, 0, null, false)
    }

    private suspend fun downloadFile(
        url: String,
        destination: File,
        assignCall: (Call?) -> Unit,
        onProgress: (Long, Long) -> Unit
    ) {
        destination.parentFile?.mkdirs()
        val call = client.newCall(Request.Builder().url(url).build())
        assignCall(call)
        call.execute().use { response ->
            check(response.isSuccessful) { "Download failed with HTTP ${response.code}." }
            val body = response.body ?: error("Download returned an empty response.")
            val total = body.contentLength().coerceAtLeast(0)
            var copied = 0L
            body.byteStream().use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        onProgress(copied, total)
                    }
                    output.fd.sync()
                }
            }
        }
        assignCall(null)
    }

    private suspend fun unzipSafely(archive: File, destination: File) {
        val destinationPath = destination.canonicalPath + File.separator
        var expandedBytes = 0L
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            while (true) {
                currentCoroutineContext().ensureActive()
                val entry = zip.nextEntry ?: break
                val output = File(destination, entry.name)
                check(output.canonicalPath.startsWith(destinationPath)) { "Unsafe path in downloaded model archive." }
                if (entry.isDirectory) output.mkdirs() else {
                    output.parentFile?.mkdirs()
                    output.outputStream().buffered().use { fileOutput ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = zip.read(buffer)
                            if (read < 0) break
                            expandedBytes += read
                            check(expandedBytes <= MAX_VOSK_EXPANDED_BYTES) { "Downloaded Vosk archive is unexpectedly large." }
                            fileOutput.write(buffer, 0, read)
                        }
                    }
                }
                zip.closeEntry()
            }
        }
    }

    private fun progress(copied: Long, total: Long): Int =
        if (total > 0) ((copied * 100L) / total).toInt().coerceIn(0, 99) else 0

    private fun readableDownloadError(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("Unable to resolve host", true) -> "No internet connection. Check your network and retry."
            message.contains("timeout", true) -> "The download timed out. Check your connection and retry."
            message.isNotBlank() -> message
            else -> "Model download failed."
        }
    }

    private fun directorySize(directory: File): Long =
        if (!directory.exists()) 0 else directory.walkTopDown().filter(File::isFile).sumOf(File::length)

    private fun updateVosk(
        downloading: Boolean,
        progress: Int,
        error: String?,
        installed: Boolean = isVoskInstalled(),
        downloadedBytes: Long = 0,
        totalBytes: Long = 0
    ) {
        _voskState.value = ModelDownloadState(installed, downloading, progress, downloadedBytes, totalBytes, error)
    }

    private fun updateWhisper(
        downloading: Boolean,
        progress: Int,
        error: String?,
        installed: Boolean = isWhisperInstalled(),
        downloadedBytes: Long = 0,
        totalBytes: Long = 0
    ) {
        _whisperState.value = ModelDownloadState(installed, downloading, progress, downloadedBytes, totalBytes, error)
    }
}
