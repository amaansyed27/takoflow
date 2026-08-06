package com.example.service

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.TakoFlowPreferences
import com.example.speech.AdaptiveLanguageModel
import com.example.speech.FormattingProfile
import com.example.speech.FormattingProfileStore
import com.example.speech.LocalSpeechEngine
import com.example.speech.SpeechModels
import com.example.speech.SpeechState
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLowest
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

class TakoFlowFixedInputMethodService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    private lateinit var preferences: TakoFlowPreferences
    private lateinit var speechEngine: LocalSpeechEngine
    private lateinit var profileStore: FormattingProfileStore
    private lateinit var adaptiveModel: AdaptiveLanguageModel

    private val composingWord = MutableStateFlow("")
    private val suggestions = MutableStateFlow<List<String>>(emptyList())

    private var previousWord: String = ""
    private var activeProfile: FormattingProfile = FormattingProfileStore.builtInProfile("default")
    private var autoCorrectEnabled: Boolean = true
    private var predictionsEnabled: Boolean = true
    private var learningEnabled: Boolean = true
    private var sensitiveField: Boolean = false

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        preferences = TakoFlowPreferences(applicationContext)
        profileStore = FormattingProfileStore.get(applicationContext)
        adaptiveModel = AdaptiveLanguageModel.get(applicationContext)
        speechEngine = LocalSpeechEngine(applicationContext)
        installWindowTreeOwners()
    }

    private fun installWindowTreeOwners() {
        val decorView = window.window?.decorView ?: return
        decorView.setViewTreeLifecycleOwner(this)
        decorView.setViewTreeSavedStateRegistryOwner(this)
    }

    override fun onCreateInputView(): View {
        installWindowTreeOwners()

        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@TakoFlowFixedInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@TakoFlowFixedInputMethodService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)

            setContent {
                MyApplicationTheme {
                    val mode by preferences.keyboardMode.collectAsState(initial = "Voice Only Mode")
                    val model by preferences.inferenceModel.collectAsState(initial = SpeechModels.VOSK)
                    val language by preferences.language.collectAsState(initial = "English (US)")
                    val punctuation by preferences.punctuation.collectAsState(initial = true)
                    val autoCaps by preferences.autoCapitalization.collectAsState(initial = true)
                    val sound by preferences.soundFeedback.collectAsState(initial = true)
                    val vibration by preferences.vibrationFeedback.collectAsState(initial = false)
                    val profileId by preferences.activeProfileId.collectAsState(initial = "default")
                    val autoStart by preferences.autoStartListening.collectAsState(initial = false)
                    val autoCorrect by preferences.autoCorrect.collectAsState(initial = true)
                    val showPredictions by preferences.wordPredictions.collectAsState(initial = true)
                    val learnHistory by preferences.learnFromTyping.collectAsState(initial = true)
                    val profiles by profileStore.profiles.collectAsState()
                    val speechState by speechEngine.speechState.collectAsState()
                    val rmsDb by speechEngine.rmsDb.collectAsState()
                    val suggestionItems by suggestions.collectAsState()

                    LaunchedEffect(
                        model,
                        language,
                        punctuation,
                        autoCaps,
                        sound,
                        vibration,
                        profileId,
                        autoCorrect,
                        showPredictions,
                        learnHistory,
                        profiles
                    ) {
                        activeProfile = profiles.firstOrNull { it.id == profileId }
                            ?: FormattingProfileStore.builtInProfile(profileId)
                        autoCorrectEnabled = autoCorrect
                        predictionsEnabled = showPredictions
                        learningEnabled = learnHistory

                        speechEngine.activeModel = model
                        speechEngine.activeLanguage = language
                        speechEngine.autoPunctuation = punctuation
                        speechEngine.autoCapitalization = autoCaps
                        speechEngine.soundFeedbackEnabled = sound
                        speechEngine.vibrationFeedbackEnabled = vibration
                        speechEngine.activeProfile = profileId
                        refreshSuggestions()
                    }

                    LaunchedEffect(autoStart, model) {
                        if (autoStart && speechState is SpeechState.Idle) {
                            speechEngine.startListening()
                        }
                    }

                    LaunchedEffect(speechState) {
                        val state = speechState
                        if (state is SpeechState.Success) {
                            if (composingWord.value.isNotBlank()) commitCurrentWord(" ")
                            currentInputConnection?.commitText(state.recognizedText + " ", 1)
                            if (learningEnabled && !sensitiveField) {
                                adaptiveModel.learnText(state.recognizedText, activeProfile.id)
                            }
                            previousWord = lastWord(state.recognizedText)
                            refreshSuggestions()
                            speechEngine.acknowledgeResult()
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SurfaceContainerLowest
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceContainerLowest)
                                .padding(vertical = 8.dp, horizontal = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TAKOFLOW",
                                    color = PrimaryAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = when (val state = speechState) {
                                        is SpeechState.Listening -> "Listening…"
                                        is SpeechState.Processing -> state.partialText
                                        is SpeechState.Success -> "Done"
                                        is SpeechState.Error -> state.message
                                        else -> "$model · ${activeProfile.name}"
                                    },
                                    color = when (speechState) {
                                        is SpeechState.Error -> MaterialTheme.colorScheme.error
                                        is SpeechState.Listening -> PrimaryAmber
                                        else -> OnSurfaceVariantDark
                                    },
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            if (mode == "Voice Only Mode") {
                                VoiceOnlyModeContent(
                                    speechState = speechState,
                                    rmsDb = rmsDb,
                                    onMicClick = {
                                        if (speechState is SpeechState.Listening) {
                                            speechEngine.stopListening()
                                        } else {
                                            speechEngine.startListening()
                                        }
                                    },
                                    onSpace = { commitCurrentWord(" ") },
                                    onDelete = ::handleDelete,
                                    onEnter = ::sendEnter
                                )
                            } else {
                                if (predictionsEnabled && !sensitiveField) {
                                    SuggestionStrip(
                                        suggestions = suggestionItems,
                                        onSuggestion = ::selectSuggestion
                                    )
                                    Spacer(Modifier.height(6.dp))
                                }

                                FullKeyboardModeContent(
                                    onKey = ::handleTypedKey,
                                    onDelete = ::handleDelete,
                                    onEnter = ::sendEnter,
                                    onMic = {
                                        if (speechState is SpeechState.Listening) {
                                            speechEngine.stopListening()
                                        } else {
                                            speechEngine.startListening()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleTypedKey(value: String) {
        if (value == " ") {
            commitCurrentWord(" ")
            return
        }

        if (value.length == 1 && (value[0].isLetter() || value[0] == '\'')) {
            val next = composingWord.value + value
            composingWord.value = next
            currentInputConnection?.setComposingText(next, 1)
            refreshSuggestions()
            return
        }

        commitCurrentWord("")
        currentInputConnection?.commitText(value, 1)
    }

    private fun commitCurrentWord(separator: String) {
        val rawWord = composingWord.value
        if (rawWord.isBlank()) {
            if (separator.isNotEmpty()) currentInputConnection?.commitText(separator, 1)
            refreshSuggestions()
            return
        }

        val committed = if (autoCorrectEnabled && !sensitiveField) {
            adaptiveModel.correct(rawWord, previousWord, activeProfile)
        } else {
            rawWord
        }

        currentInputConnection?.setComposingText(committed, 1)
        currentInputConnection?.finishComposingText()
        if (separator.isNotEmpty()) currentInputConnection?.commitText(separator, 1)

        if (learningEnabled && !sensitiveField) {
            adaptiveModel.learnWord(committed, previousWord, activeProfile.id)
        }

        previousWord = committed.lowercase(Locale.getDefault())
        composingWord.value = ""
        refreshSuggestions()
    }

    private fun selectSuggestion(suggestion: String) {
        if (suggestion.isBlank()) return

        currentInputConnection?.setComposingText(suggestion, 1)
        currentInputConnection?.finishComposingText()
        currentInputConnection?.commitText(" ", 1)

        if (learningEnabled && !sensitiveField) {
            adaptiveModel.learnWord(suggestion, previousWord, activeProfile.id)
        }

        previousWord = suggestion.lowercase(Locale.getDefault())
        composingWord.value = ""
        refreshSuggestions()
    }

    private fun handleDelete() {
        val current = composingWord.value
        if (current.isNotEmpty()) {
            val next = current.dropLast(1)
            composingWord.value = next
            if (next.isEmpty()) {
                currentInputConnection?.setComposingText("", 1)
                currentInputConnection?.finishComposingText()
            } else {
                currentInputConnection?.setComposingText(next, 1)
            }
            refreshSuggestions()
        } else {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
    }

    private fun refreshSuggestions() {
        suggestions.value = if (!predictionsEnabled || sensitiveField) {
            emptyList()
        } else {
            adaptiveModel.suggestions(
                prefix = composingWord.value,
                previousWord = previousWord,
                profile = activeProfile
            )
        }
    }

    private fun sendEnter() {
        commitCurrentWord("")
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    override fun onStartInputView(attribute: EditorInfo?, restarting: Boolean) {
        installWindowTreeOwners()
        sensitiveField = isSensitiveField(attribute)
        composingWord.value = ""
        previousWord = if (sensitiveField) {
            ""
        } else {
            lastWord(currentInputConnection?.getTextBeforeCursor(100, 0)?.toString().orEmpty())
        }
        refreshSuggestions()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        super.onStartInputView(attribute, restarting)
    }

    override fun onWindowShown() {
        installWindowTreeOwners()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        super.onWindowShown()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        speechEngine.stopListening()
        currentInputConnection?.finishComposingText()
        composingWord.value = ""
        suggestions.value = emptyList()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
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

    private fun lastWord(text: String): String =
        Regex("[\\p{L}\\p{N}']+")
            .findAll(text)
            .lastOrNull()
            ?.value
            ?.lowercase(Locale.getDefault())
            .orEmpty()
}

@androidx.compose.runtime.Composable
private fun SuggestionStrip(
    suggestions: List<String>,
    onSuggestion: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        suggestions.take(3).forEach { suggestion ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainerHigh)
                    .clickable { onSuggestion(suggestion) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    suggestion,
                    color = OnSurfaceDark,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }
    }
}
