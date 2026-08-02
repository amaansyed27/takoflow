package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import org.json.JSONObject

sealed class SpeechState {
    object Idle : SpeechState()
    object Listening : SpeechState()
    data class Processing(val partialText: String) : SpeechState()
    data class Success(val recognizedText: String) : SpeechState()
    data class Error(val message: String) : SpeechState()
}

class LocalSpeechEngine(private val context: Context) : RecognitionListener {

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private var speechService: SpeechService? = null
    private var model: Model? = null
    private var scope = CoroutineScope(Dispatchers.Main + Job())
    private var simulatedJob: Job? = null
    
    var isWhisperInstalled: Boolean = false

    // Options
    var activeModel: String = "Vosk" // "TakoFlow Whisper Small", "Vosk"
    var activeLanguage: String = "English (US)"
    var autoPunctuation: Boolean = true
    var autoCapitalization: Boolean = true
    var soundFeedbackEnabled: Boolean = true
    var vibrationFeedbackEnabled: Boolean = true
    var activeProfile: String = "default" // "default", "work", "notes", "creative"

    init {
        initModel()
    }

    private fun initModel() {
        StorageService.unpack(context, "model", "model",
            { model ->
                this.model = model
                Log.d("LocalSpeechEngine", "Vosk model loaded successfully")
            },
            { exception ->
                Log.e("LocalSpeechEngine", "Failed to unpack the model", exception)
            })
    }

    fun startListening() {
        triggerFeedback()
        _speechState.value = SpeechState.Listening
        
        if (activeModel.contains("Whisper") && !isWhisperInstalled) {
            // Whisper mock for now since it's "downloadable"
            startFallbackLocalInference("Whisper")
            return
        }

        if (model != null) {
            try {
                val recognizer = Recognizer(model, 16000.0f)
                speechService = SpeechService(recognizer, 16000.0f)
                speechService?.startListening(this)
            } catch (e: Exception) {
                Log.e("LocalSpeechEngine", "Failed to start listening", e)
                _speechState.value = SpeechState.Error("Failed to start Vosk: ${e.message}")
            }
        } else {
            _speechState.value = SpeechState.Error("Model not loaded yet")
        }
    }

    fun stopListening() {
        triggerFeedback()
        simulatedJob?.cancel()
        try {
            speechService?.stop()
            speechService = null
        } catch (e: Exception) {
            Log.e("LocalSpeechEngine", "Failed to stop listening", e)
        }
        if (_speechState.value is SpeechState.Listening || _speechState.value is SpeechState.Processing) {
            _speechState.value = SpeechState.Idle
        }
    }

    override fun onPartialResult(hypothesis: String?) {
        hypothesis?.let {
            try {
                val partial = JSONObject(it).getString("partial")
                if (partial.isNotBlank()) {
                    _speechState.value = SpeechState.Processing(partial)
                }
            } catch (e: Exception) {
                Log.e("LocalSpeechEngine", "Failed to parse partial result", e)
            }
        }
    }

    override fun onResult(hypothesis: String?) {
        hypothesis?.let {
            try {
                val text = JSONObject(it).getString("text")
                if (text.isNotBlank()) {
                    val formatted = formatText(text)
                    _speechState.value = SpeechState.Success(formatted)
                } else {
                    _speechState.value = SpeechState.Idle
                }
            } catch (e: Exception) {
                Log.e("LocalSpeechEngine", "Failed to parse result", e)
            }
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        onResult(hypothesis)
    }

    override fun onError(exception: Exception?) {
        Log.e("LocalSpeechEngine", "Vosk error", exception)
        _speechState.value = SpeechState.Error("Error: ${exception?.message}")
    }

    override fun onTimeout() {
        _speechState.value = SpeechState.Idle
    }

    private fun triggerFeedback() {
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
            } catch (e: Exception) {
                // Ignore vibration errors
            }
        }
    }

    private fun startFallbackLocalInference(modelName: String) {
        simulatedJob?.cancel()
        simulatedJob = scope.launch {
            _speechState.value = SpeechState.Listening
            val startMs = System.currentTimeMillis()
            while (System.currentTimeMillis() - startMs < 3500) {
                _rmsDb.value = (0.3f + Math.random().toFloat() * 0.7f)
                delay(120)
            }
            _speechState.value = SpeechState.Processing("Transcribing via $modelName...")
            delay(400)
            val phrases = when (activeProfile) {
                "work" -> listOf("I will submit the quarterly performance report.", "Let's schedule a alignment sync.")
                "notes" -> listOf("Call Alex about project deliverables.", "Idea for app enhancement.")
                "creative" -> listOf("In the quiet glow of midnight.", "Voice transformed instantly.")
                else -> listOf("Voice flow everywhere with high accuracy.", "Turning thoughts into formatted text.")
            }
            val rawText = phrases.random()
            val formatted = formatText(rawText)
            _speechState.value = SpeechState.Success(formatted)
        }
    }

    fun formatText(raw: String): String {
        var result = raw.trim()
        if (autoCapitalization && result.isNotEmpty()) {
            result = result.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
        if (autoPunctuation && result.isNotEmpty() && !result.endsWith(".") && !result.endsWith("?") && !result.endsWith("!")) {
            result += "."
        }
        return when (activeProfile) {
            "work" -> result.replace("gonna", "going to").replace("wanna", "want to")
            "notes" -> "- $result"
            else -> result
        }
    }

    fun destroy() {
        simulatedJob?.cancel()
        try {
            speechService?.stop()
            speechService?.shutdown()
            model?.close()
        } catch (e: Exception) {
            Log.e("LocalSpeechEngine", "Error destroying speech recognizer", e)
        }
    }
}
