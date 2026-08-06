package com.example.speech

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.max

sealed class SpeechState {
    data object Idle : SpeechState()
    data class Preparing(val message: String) : SpeechState()
    data class Listening(val partialText: String = "") : SpeechState()
    data class Processing(val partialText: String) : SpeechState()
    data class Success(val recognizedText: String) : SpeechState()
    data class Error(val message: String) : SpeechState()
}

class LocalSpeechEngine(private val context: Context) : RecognitionListener {
    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val engineJob = SupervisorJob()
    private val scope = CoroutineScope(engineJob + Dispatchers.IO)
    private val modelManager = SpeechModelManager.get(context)
    private val resourceLock = Any()
    private val whisperLock = Any()
    private val sessionCounter = AtomicLong(0L)

    private var voskModel: Model? = null
    private var speechService: SpeechService? = null
    private var voskStartJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var transcriptionJob: Job? = null
    private var whisperPreparationJob: Job? = null
    private var recordedPcm = ByteArrayOutputStream()

    @Volatile private var whisperBridge: WhisperBridge? = null
    @Volatile private var destroyed = false
    @Volatile private var activeSession = 0L
    @Volatile private var voskAcceptCallbacks = false
    @Volatile private var voskStreaming = false

    var activeModel: String = SpeechModels.VOSK
        set(value) {
            if (field != value && _speechState.value !is SpeechState.Idle) {
                cancelListening()
            }
            field = value
            if (value == SpeechModels.WHISPER_TINY) prepareWhisper()
        }

    var activeLanguage: String = "English (US)"
    var autoPunctuation: Boolean = true
    var autoCapitalization: Boolean = true
    var soundFeedbackEnabled: Boolean = true
    var vibrationFeedbackEnabled: Boolean = false
    var activeProfile: String = "default"

    fun startListening() {
        if (destroyed) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            _speechState.value = SpeechState.Error("Microphone permission is required.")
            return
        }
        if (
            _speechState.value is SpeechState.Preparing ||
            _speechState.value is SpeechState.Listening ||
            _speechState.value is SpeechState.Processing
        ) return

        val session = sessionCounter.incrementAndGet()
        activeSession = session
        triggerFeedback()
        when (activeModel) {
            SpeechModels.WHISPER_TINY -> startWhisperRecording(session)
            else -> startVoskListening(session)
        }
    }

    fun stopListening() {
        if (_speechState.value !is SpeechState.Listening) return
        triggerFeedback()
        when (activeModel) {
            SpeechModels.WHISPER_TINY -> stopWhisperAndTranscribe(activeSession)
            else -> stopVoskListening(activeSession)
        }
    }

    fun cancelListening() {
        activeSession = sessionCounter.incrementAndGet()
        voskAcceptCallbacks = false
        voskStreaming = false
        voskStartJob?.cancel()
        transcriptionJob?.cancel()
        recordingJob?.cancel()

        val service = synchronized(resourceLock) {
            speechService.also { speechService = null }
        }
        try {
            service?.stop()
            service?.shutdown()
        } catch (_: Throwable) {
        }

        try {
            audioRecord?.stop()
        } catch (_: Throwable) {
        }
        releaseAudioRecord()
        _rmsDb.value = 0f
        if (!destroyed) _speechState.value = SpeechState.Idle
    }

    fun acknowledgeResult() {
        _speechState.value = when (_speechState.value) {
            is SpeechState.Success -> if (voskStreaming) SpeechState.Listening() else SpeechState.Idle
            is SpeechState.Error -> SpeechState.Idle
            else -> _speechState.value
        }
    }

    private fun startVoskListening(session: Long) {
        if (!modelManager.isVoskInstalled()) {
            _speechState.value = SpeechState.Error("Download the Vosk model from TakoFlow settings first.")
            return
        }

        _speechState.value = SpeechState.Preparing("Loading Vosk…")
        voskStartJob?.cancel()
        voskStartJob = scope.launch {
            try {
                val existing = synchronized(resourceLock) { voskModel }
                val loadedModel = existing ?: Model(modelManager.voskModelDir.absolutePath)
                val selectedModel = synchronized(resourceLock) {
                    if (!isCurrent(session)) {
                        if (existing == null) loadedModel.close()
                        return@synchronized null
                    }
                    voskModel ?: loadedModel.also { voskModel = it }
                } ?: return@launch

                currentCoroutineContext().ensureActive()
                val recognizer = Recognizer(selectedModel, SAMPLE_RATE.toFloat())
                val service = SpeechService(recognizer, SAMPLE_RATE.toFloat())

                synchronized(resourceLock) {
                    if (!isCurrent(session)) {
                        service.shutdown()
                        return@synchronized
                    }
                    speechService = service
                    voskAcceptCallbacks = true
                    voskStreaming = true
                    service.startListening(this@LocalSpeechEngine)
                    _speechState.value = SpeechState.Listening()
                }
            } catch (_: CancellationException) {
            } catch (error: Throwable) {
                Log.e(TAG, "Could not start Vosk", error)
                if (isCurrent(session)) {
                    voskAcceptCallbacks = false
                    voskStreaming = false
                    _speechState.value = SpeechState.Error(
                        nativeFailureMessage(error, "Could not start Vosk.")
                    )
                }
            }
        }
    }

    private fun stopVoskListening(session: Long) {
        voskStreaming = false
        val service = synchronized(resourceLock) {
            speechService.also { speechService = null }
        }
        try {
            service?.stop()
            _speechState.value = SpeechState.Processing("Finishing transcription…")
            scope.launch {
                delay(1_000)
                if (isCurrent(session) && _speechState.value is SpeechState.Processing) {
                    voskAcceptCallbacks = false
                    _speechState.value = SpeechState.Idle
                }
                try {
                    service?.shutdown()
                } catch (_: Throwable) {
                }
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Could not stop Vosk", error)
            voskAcceptCallbacks = false
            if (isCurrent(session)) _speechState.value = SpeechState.Idle
        }
    }

    private fun prepareWhisper() {
        if (destroyed || !modelManager.isWhisperInstalled()) return
        synchronized(whisperLock) {
            if (whisperBridge != null || whisperPreparationJob?.isActive == true) return
            whisperPreparationJob = scope.launch {
                val prepared = try {
                    val started = SystemClock.elapsedRealtime()
                    WhisperBridge(modelManager.whisperModelFile.absolutePath).also {
                        Log.i(TAG, "Whisper model prepared in ${SystemClock.elapsedRealtime() - started} ms")
                    }
                } catch (error: Throwable) {
                    Log.e(TAG, "Could not prepare Whisper", error)
                    null
                }
                if (prepared != null) {
                    synchronized(whisperLock) {
                        if (destroyed || whisperBridge != null) prepared.close()
                        else whisperBridge = prepared
                    }
                }
            }
        }
    }

    private suspend fun getOrCreateWhisperBridge(): WhisperBridge {
        prepareWhisper()
        whisperPreparationJob?.join()
        synchronized(whisperLock) { whisperBridge?.let { return it } }

        val created = WhisperBridge(modelManager.whisperModelFile.absolutePath)
        synchronized(whisperLock) {
            if (destroyed) {
                created.close()
                error("Speech engine was closed.")
            }
            val existing = whisperBridge
            return if (existing != null) {
                created.close()
                existing
            } else {
                whisperBridge = created
                created
            }
        }
    }

    private fun startWhisperRecording(session: Long) {
        if (!modelManager.isWhisperInstalled()) {
            _speechState.value = SpeechState.Error("Download Whisper Tiny before selecting it.")
            return
        }
        prepareWhisper()

        try {
            val minimum = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            check(minimum > 0) { "Android could not create an audio input buffer." }

            recordedPcm = ByteArrayOutputStream()
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                max(minimum * 2, 8192)
            )
            check(recorder.state == AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                "Microphone initialization failed."
            }

            synchronized(resourceLock) {
                if (!isCurrent(session)) {
                    recorder.release()
                    return
                }
                audioRecord = recorder
                recorder.startRecording()
            }

            _speechState.value = SpeechState.Listening()
            recordingJob = scope.launch {
                val buffer = ShortArray(2048)
                while (isCurrent(session) && recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        var peak = 0
                        for (index in 0 until read) {
                            val sample = buffer[index].toInt()
                            recordedPcm.write(sample and 0xFF)
                            recordedPcm.write((sample shr 8) and 0xFF)
                            peak = max(peak, abs(sample))
                        }
                        _rmsDb.value = (peak / Short.MAX_VALUE.toFloat()).coerceIn(0f, 1f)
                    }
                }
            }
        } catch (error: Throwable) {
            releaseAudioRecord()
            Log.e(TAG, "Could not start Whisper recording", error)
            if (isCurrent(session)) {
                _speechState.value = SpeechState.Error(
                    nativeFailureMessage(error, "Could not start recording.")
                )
            }
        }
    }

    private fun stopWhisperAndTranscribe(session: Long) {
        if (_speechState.value !is SpeechState.Listening) return
        _speechState.value = SpeechState.Processing("Transcribing on device…")
        try {
            audioRecord?.stop()
        } catch (_: Throwable) {
        }

        transcriptionJob = scope.launch {
            try {
                recordingJob?.join()
                val samples = pcm16ToFloat(recordedPcm.toByteArray())
                releaseAudioRecord()
                currentCoroutineContext().ensureActive()
                if (!isCurrent(session)) return@launch
                if (samples.size < SAMPLE_RATE / 4) {
                    _speechState.value = SpeechState.Error("Recording was too short.")
                    return@launch
                }

                val bridge = getOrCreateWhisperBridge()
                val started = SystemClock.elapsedRealtime()
                val text = bridge.transcribe(samples)
                currentCoroutineContext().ensureActive()
                if (!isCurrent(session)) return@launch

                Log.i(
                    TAG,
                    "Whisper processed ${samples.size / SAMPLE_RATE.toFloat()} seconds in " +
                        "${SystemClock.elapsedRealtime() - started} ms"
                )
                _speechState.value = if (text.isBlank()) {
                    SpeechState.Error("No speech was detected.")
                } else {
                    SpeechState.Success(formatText(text))
                }
            } catch (_: CancellationException) {
            } catch (error: Throwable) {
                Log.e(TAG, "Whisper transcription failed", error)
                if (isCurrent(session)) {
                    _speechState.value = SpeechState.Error(
                        nativeFailureMessage(error, "Whisper transcription failed.")
                    )
                }
            } finally {
                _rmsDb.value = 0f
                if (destroyed) closeWhisperBridge()
            }
        }
    }

    override fun onPartialResult(hypothesis: String?) {
        if (!voskAcceptCallbacks) return
        val partial = parseResult(hypothesis, "partial").orEmpty()
        if (partial.isNotBlank()) _speechState.value = SpeechState.Listening(partial)
    }

    override fun onResult(hypothesis: String?) {
        if (!voskAcceptCallbacks) return
        parseResult(hypothesis, "text")?.takeIf(String::isNotBlank)?.let {
            _speechState.value = SpeechState.Success(formatText(it))
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        if (!voskAcceptCallbacks) return
        voskAcceptCallbacks = false
        voskStreaming = false
        val text = parseResult(hypothesis, "text")
        _speechState.value = if (!text.isNullOrBlank()) {
            SpeechState.Success(formatText(text))
        } else {
            SpeechState.Idle
        }
    }

    override fun onError(exception: Exception?) {
        if (!voskAcceptCallbacks) return
        voskAcceptCallbacks = false
        voskStreaming = false
        synchronized(resourceLock) { speechService = null }
        Log.e(TAG, "Vosk recognition failed", exception)
        _speechState.value = SpeechState.Error(exception?.message ?: "Vosk recognition failed.")
    }

    override fun onTimeout() {
        if (!voskAcceptCallbacks) return
        voskAcceptCallbacks = false
        voskStreaming = false
        synchronized(resourceLock) { speechService = null }
        _speechState.value = SpeechState.Idle
    }

    private fun isCurrent(session: Long): Boolean =
        !destroyed && activeSession == session && currentCoroutineContextSafe()

    private fun currentCoroutineContextSafe(): Boolean =
        try {
            engineJob.isActive
        } catch (_: Throwable) {
            false
        }

    private fun parseResult(json: String?, key: String): String? = try {
        json?.let { JSONObject(it).optString(key) }
    } catch (error: Exception) {
        Log.e(TAG, "Could not parse speech result", error)
        null
    }

    fun formatText(raw: String): String = DictationTextFormatter.format(
        raw = raw,
        autoCapitalization = autoCapitalization,
        autoPunctuation = autoPunctuation,
        profile = activeProfile
    )

    private fun triggerFeedback() {
        if (soundFeedbackEnabled) {
            try {
                val tone = ToneGenerator(AudioManager.STREAM_SYSTEM, 35)
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
                scope.launch {
                    delay(100)
                    tone.release()
                }
            } catch (_: Throwable) {
            }
        }
        if (vibrationFeedbackEnabled) {
            try {
                val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    manager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                if (vibrator.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(40)
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun pcm16ToFloat(bytes: ByteArray): FloatArray {
        val output = FloatArray(bytes.size / 2)
        for (index in output.indices) {
            val low = bytes[index * 2].toInt() and 0xFF
            val high = bytes[index * 2 + 1].toInt()
            val sample = ((high shl 8) or low).toShort()
            output[index] = sample / 32768f
        }
        return output
    }

    private fun nativeFailureMessage(error: Throwable, fallback: String): String =
        error.message?.takeIf(String::isNotBlank) ?: fallback

    private fun releaseAudioRecord() {
        val recorder = synchronized(resourceLock) {
            audioRecord.also { audioRecord = null }
        }
        try {
            recorder?.release()
        } catch (_: Throwable) {
        }
        recordingJob = null
    }

    private fun closeWhisperBridge() {
        val bridge = synchronized(whisperLock) {
            whisperBridge.also { whisperBridge = null }
        }
        try {
            bridge?.close()
        } catch (_: Throwable) {
        }
    }

    private fun closeVoskModel() {
        val model = synchronized(resourceLock) {
            voskModel.also { voskModel = null }
        }
        try {
            model?.close()
        } catch (_: Throwable) {
        }
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        activeSession = sessionCounter.incrementAndGet()
        cancelListening()
        whisperPreparationJob?.cancel()
        engineJob.cancel()

        val transcription = transcriptionJob
        if (transcription?.isActive == true) {
            transcription.invokeOnCompletion {
                closeWhisperBridge()
                closeVoskModel()
            }
        } else {
            closeWhisperBridge()
            closeVoskModel()
        }
    }

    private companion object {
        const val TAG = "LocalSpeechEngine"
        const val SAMPLE_RATE = 16_000
    }
}
