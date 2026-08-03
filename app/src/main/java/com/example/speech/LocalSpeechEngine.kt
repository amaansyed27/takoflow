package com.example.speech

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.max

sealed class SpeechState {
    data object Idle : SpeechState()
    data object Listening : SpeechState()
    data class Processing(val partialText: String) : SpeechState()
    data class Success(val recognizedText: String) : SpeechState()
    data class Error(val message: String) : SpeechState()
}

class LocalSpeechEngine(private val context: Context) : RecognitionListener {
    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val modelManager = SpeechModelManager.get(context)

    private var voskModel: Model? = null
    private var speechService: SpeechService? = null
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var recordedPcm = ByteArrayOutputStream()
    private var whisperBridge: WhisperBridge? = null

    var activeModel: String = SpeechModels.VOSK
    var activeLanguage: String = "English (US)"
    var autoPunctuation: Boolean = true
    var autoCapitalization: Boolean = true
    var soundFeedbackEnabled: Boolean = true
    var vibrationFeedbackEnabled: Boolean = false
    var activeProfile: String = "default"

    fun startListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            _speechState.value = SpeechState.Error("Microphone permission is required.")
            return
        }

        if (_speechState.value is SpeechState.Listening || _speechState.value is SpeechState.Processing) {
            return
        }

        triggerFeedback()
        when (activeModel) {
            SpeechModels.WHISPER_TINY -> startWhisperRecording()
            else -> startVoskListening()
        }
    }

    fun stopListening() {
        if (_speechState.value !is SpeechState.Listening && _speechState.value !is SpeechState.Processing) {
            return
        }

        triggerFeedback()
        when (activeModel) {
            SpeechModels.WHISPER_TINY -> stopWhisperAndTranscribe()
            else -> stopVoskListening()
        }
    }

    fun acknowledgeResult() {
        _speechState.value = when (_speechState.value) {
            is SpeechState.Success -> if (speechService != null) SpeechState.Listening else SpeechState.Idle
            is SpeechState.Error -> SpeechState.Idle
            else -> _speechState.value
        }
    }

    private fun startVoskListening() {
        if (!modelManager.isVoskInstalled()) {
            _speechState.value = SpeechState.Error("Download the Vosk model from TakoFlow settings first.")
            return
        }

        try {
            val loadedModel = voskModel ?: Model(modelManager.voskModelDir.absolutePath).also {
                voskModel = it
            }
            val recognizer = Recognizer(loadedModel, SAMPLE_RATE.toFloat())
            speechService = SpeechService(recognizer, SAMPLE_RATE.toFloat()).also {
                it.startListening(this)
            }
            _speechState.value = SpeechState.Listening
        } catch (error: Exception) {
            Log.e(TAG, "Could not start Vosk", error)
            _speechState.value = SpeechState.Error(error.message ?: "Could not start Vosk.")
        }
    }

    private fun stopVoskListening() {
        val activeService = speechService
        speechService = null
        try {
            activeService?.stop()
            _speechState.value = SpeechState.Processing("Finishing transcription…")
            scope.launch {
                delay(800)
                if (_speechState.value is SpeechState.Processing) {
                    _speechState.value = SpeechState.Idle
                }
            }
        } catch (error: Exception) {
            Log.e(TAG, "Could not stop Vosk", error)
            _speechState.value = SpeechState.Idle
        }
    }

    private fun startWhisperRecording() {
        if (!modelManager.isWhisperInstalled()) {
            _speechState.value = SpeechState.Error("Download Whisper Tiny before selecting it.")
            return
        }

        try {
            val minimum = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            check(minimum > 0) { "Android could not create an audio input buffer." }

            recordedPcm = ByteArrayOutputStream()
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                max(minimum * 2, 8192)
            ).also { recorder ->
                check(recorder.state == AudioRecord.STATE_INITIALIZED) {
                    "Microphone initialization failed."
                }
                recorder.startRecording()
            }

            _speechState.value = SpeechState.Listening
            recordingJob = scope.launch {
                val buffer = ShortArray(2048)
                while (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
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
        } catch (error: Exception) {
            releaseAudioRecord()
            Log.e(TAG, "Could not start Whisper recording", error)
            _speechState.value = SpeechState.Error(error.message ?: "Could not start recording.")
        }
    }

    private fun stopWhisperAndTranscribe() {
        if (_speechState.value !is SpeechState.Listening) return
        _speechState.value = SpeechState.Processing("Transcribing on device…")

        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }

        scope.launch {
            recordingJob?.join()
            releaseAudioRecord()
            try {
                val samples = pcm16ToFloat(recordedPcm.toByteArray())
                if (samples.size < SAMPLE_RATE / 4) {
                    _speechState.value = SpeechState.Error("Recording was too short.")
                    return@launch
                }

                val bridge = whisperBridge ?: WhisperBridge(
                    modelManager.whisperModelFile.absolutePath
                ).also { whisperBridge = it }
                val text = bridge.transcribe(samples)
                _speechState.value = if (text.isBlank()) {
                    SpeechState.Error("No speech was detected.")
                } else {
                    SpeechState.Success(formatText(text))
                }
            } catch (error: Exception) {
                Log.e(TAG, "Whisper transcription failed", error)
                _speechState.value = SpeechState.Error(error.message ?: "Whisper transcription failed.")
            } finally {
                _rmsDb.value = 0f
            }
        }
    }

    override fun onPartialResult(hypothesis: String?) {
        parseResult(hypothesis, "partial")?.takeIf { it.isNotBlank() }?.let {
            _speechState.value = SpeechState.Processing(it)
        }
    }

    override fun onResult(hypothesis: String?) {
        parseResult(hypothesis, "text")?.takeIf { it.isNotBlank() }?.let {
            _speechState.value = SpeechState.Success(formatText(it))
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        val text = parseResult(hypothesis, "text")
        _speechState.value = if (!text.isNullOrBlank()) {
            SpeechState.Success(formatText(text))
        } else {
            SpeechState.Idle
        }
    }

    override fun onError(exception: Exception?) {
        speechService = null
        Log.e(TAG, "Vosk recognition failed", exception)
        _speechState.value = SpeechState.Error(exception?.message ?: "Vosk recognition failed.")
    }

    override fun onTimeout() {
        speechService = null
        _speechState.value = SpeechState.Idle
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
            } catch (_: Exception) {
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
            } catch (_: Exception) {
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

    private fun releaseAudioRecord() {
        try {
            audioRecord?.release()
        } catch (_: Exception) {
        }
        audioRecord = null
        recordingJob = null
    }

    fun destroy() {
        val service = speechService
        speechService = null
        try {
            service?.stop()
            service?.shutdown()
        } catch (_: Exception) {
        }
        releaseAudioRecord()
        whisperBridge?.close()
        whisperBridge = null
        voskModel?.close()
        voskModel = null
    }

    private companion object {
        const val TAG = "LocalSpeechEngine"
        const val SAMPLE_RATE = 16_000
    }
}
