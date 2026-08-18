package com.example.service

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.core.TakoFlowAppContainer
import com.example.data.repository.FormattingProfileRepository
import com.example.data.repository.SettingsRepository
import com.example.speech.LocalSpeechEngine
import com.example.speech.SpeechModels
import com.example.speech.SpeechState
import com.example.speech.WhisperModes
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SurfaceContainerLowest
import kotlinx.coroutines.flow.MutableStateFlow

class TakoFlowFixedInputMethodService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    private lateinit var settings: SettingsRepository
    private lateinit var profiles: FormattingProfileRepository
    private lateinit var speechEngine: LocalSpeechEngine
    private lateinit var editorController: ImeEditorController
    private val sensitiveField = MutableStateFlow(false)
    private var hasSpeechComposition = false

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        val container = TakoFlowAppContainer.get(applicationContext)
        settings = container.settings
        profiles = container.profiles
        speechEngine = container.createSpeechEngine()
        editorController = ImeEditorController(
            connectionProvider = { currentInputConnection },
            editorInfoProvider = { currentInputEditorInfo }
        )
        installWindowTreeOwners()
    }

    private fun installWindowTreeOwners() {
        val decorView = window.window?.decorView ?: return
        decorView.setViewTreeLifecycleOwner(this)
        decorView.setViewTreeSavedStateRegistryOwner(this)
        decorView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
    }

    override fun onCreateInputView(): View {
        installWindowTreeOwners()
        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@TakoFlowFixedInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@TakoFlowFixedInputMethodService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                MyApplicationTheme {
                    val model by settings.inferenceModel.collectAsState(initial = SpeechModels.VOSK)
                    val whisperMode by settings.whisperMode.collectAsState(initial = WhisperModes.BATCH)
                    val punctuation by settings.punctuation.collectAsState(initial = true)
                    val autoCaps by settings.autoCapitalization.collectAsState(initial = true)
                    val grammar by settings.grammarCorrection.collectAsState(initial = true)
                    val spelling by settings.spellCorrection.collectAsState(initial = true)
                    val sound by settings.soundFeedback.collectAsState(initial = true)
                    val vibration by settings.vibrationFeedback.collectAsState(initial = false)
                    val profileId by settings.activeProfileId.collectAsState(initial = "default")
                    val autoStart by settings.autoStartListening.collectAsState(initial = false)
                    val profileList by profiles.profiles.collectAsState()
                    val speechState by speechEngine.speechState.collectAsState()
                    val rmsDb by speechEngine.rmsDb.collectAsState()
                    val isSensitive by sensitiveField.collectAsState()
                    val profileName = profileList.firstOrNull { it.id == profileId }?.name
                        ?: profiles.builtInProfile(profileId).name

                    LaunchedEffect(
                        model,
                        whisperMode,
                        punctuation,
                        autoCaps,
                        grammar,
                        spelling,
                        sound,
                        vibration,
                        profileId
                    ) {
                        speechEngine.activeModel = model
                        speechEngine.whisperMode = whisperMode
                        speechEngine.autoPunctuation = punctuation
                        speechEngine.autoCapitalization = autoCaps
                        speechEngine.grammarCorrectionEnabled = grammar
                        speechEngine.spellCorrectionEnabled = spelling
                        speechEngine.soundFeedbackEnabled = sound
                        speechEngine.vibrationFeedbackEnabled = vibration
                        speechEngine.activeProfile = profileId
                    }

                    LaunchedEffect(autoStart, model, isSensitive) {
                        if (autoStart && !isSensitive && speechState is SpeechState.Idle) {
                            speechEngine.startListening()
                        }
                    }

                    LaunchedEffect(speechState, isSensitive) {
                        when (val state = speechState) {
                            is SpeechState.Listening -> {
                                if (!isSensitive && state.partialText.isNotBlank()) {
                                    currentInputConnection?.setComposingText(state.partialText, 1)
                                    hasSpeechComposition = true
                                }
                            }
                            is SpeechState.Success -> {
                                currentInputConnection?.commitText(state.recognizedText + " ", 1)
                                currentInputConnection?.finishComposingText()
                                hasSpeechComposition = false
                                speechEngine.acknowledgeResult()
                            }
                            is SpeechState.Idle, is SpeechState.Error -> {
                                clearSpeechComposition(removeText = true)
                            }
                            is SpeechState.Preparing, is SpeechState.Processing -> Unit
                        }
                    }

                    Surface(color = SurfaceContainerLowest) {
                        VoiceKeyboardPanel(
                            state = speechState,
                            rmsDb = rmsDb,
                            modelName = model,
                            profileName = profileName,
                            sensitiveField = isSensitive,
                            onMicClick = {
                                when {
                                    isSensitive -> Unit
                                    speechState is SpeechState.Listening -> speechEngine.stopListening()
                                    speechState is SpeechState.Idle || speechState is SpeechState.Error -> {
                                        speechEngine.startListening()
                                    }
                                }
                            },
                            onSwitchKeyboard = ::switchBackToTypingKeyboard,
                            onSpace = {
                                finishSpeechComposition()
                                editorController.insertSpace()
                            },
                            onDelete = {
                                finishSpeechComposition()
                                editorController.deleteBackward()
                            },
                            onEnter = {
                                finishSpeechComposition()
                                editorController.sendEnter()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun finishSpeechComposition() {
        if (!hasSpeechComposition) return
        currentInputConnection?.finishComposingText()
        hasSpeechComposition = false
    }

    private fun clearSpeechComposition(removeText: Boolean) {
        if (!hasSpeechComposition) return
        if (removeText) currentInputConnection?.setComposingText("", 1)
        currentInputConnection?.finishComposingText()
        hasSpeechComposition = false
    }

    @Suppress("DEPRECATION")
    private fun switchBackToTypingKeyboard() {
        speechEngine.cancelListening()
        clearSpeechComposition(removeText = true)
        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val switched = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchToPreviousInputMethod() || switchToNextInputMethod(false)
        } else {
            val token = window.window?.attributes?.token
            token != null && manager.switchToLastInputMethod(token)
        }
        if (!switched) manager.showInputMethodPicker()
    }

    override fun onStartInputView(attribute: EditorInfo?, restarting: Boolean) {
        installWindowTreeOwners()
        clearSpeechComposition(removeText = true)
        val sensitive = isSensitiveField(attribute)
        sensitiveField.value = sensitive
        if (sensitive) speechEngine.cancelListening()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        super.onStartInputView(attribute, restarting)
    }

    override fun onWindowShown() {
        installWindowTreeOwners()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        super.onWindowShown()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        speechEngine.cancelListening()
        clearSpeechComposition(removeText = true)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onFinishInputView(finishingInput)
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onDestroy() {
        clearSpeechComposition(removeText = true)
        speechEngine.destroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    private fun isSensitiveField(info: EditorInfo?): Boolean {
        val inputType = info?.inputType ?: return false
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return when (inputClass) {
            InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
    }
}
