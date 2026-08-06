package com.example.service

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
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

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        preferences = TakoFlowPreferences(applicationContext)
        profileStore = FormattingProfileStore.get(applicationContext)
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
                    val model by preferences.inferenceModel.collectAsState(initial = SpeechModels.VOSK)
                    val language by preferences.language.collectAsState(initial = "English (US)")
                    val punctuation by preferences.punctuation.collectAsState(initial = true)
                    val autoCaps by preferences.autoCapitalization.collectAsState(initial = true)
                    val sound by preferences.soundFeedback.collectAsState(initial = true)
                    val vibration by preferences.vibrationFeedback.collectAsState(initial = false)
                    val profileId by preferences.activeProfileId.collectAsState(initial = "default")
                    val autoStart by preferences.autoStartListening.collectAsState(initial = false)
                    val profiles by profileStore.profiles.collectAsState()
                    val speechState by speechEngine.speechState.collectAsState()
                    val rmsDb by speechEngine.rmsDb.collectAsState()
                    val profileName = profiles.firstOrNull { it.id == profileId }?.name
                        ?: FormattingProfileStore.builtInProfile(profileId).name

                    LaunchedEffect(
                        model,
                        language,
                        punctuation,
                        autoCaps,
                        sound,
                        vibration,
                        profileId
                    ) {
                        speechEngine.activeModel = model
                        speechEngine.activeLanguage = language
                        speechEngine.autoPunctuation = punctuation
                        speechEngine.autoCapitalization = autoCaps
                        speechEngine.soundFeedbackEnabled = sound
                        speechEngine.vibrationFeedbackEnabled = vibration
                        speechEngine.activeProfile = profileId
                    }

                    LaunchedEffect(autoStart, model) {
                        if (autoStart && speechState is SpeechState.Idle) {
                            speechEngine.startListening()
                        }
                    }

                    LaunchedEffect(speechState) {
                        val state = speechState
                        if (state is SpeechState.Success) {
                            currentInputConnection?.commitText(state.recognizedText + " ", 1)
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
                                .padding(vertical = 8.dp, horizontal = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
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
                                        else -> "$model · $profileName"
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

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceContainerHigh)
                                    .clickable(onClick = ::switchBackToTypingKeyboard),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Keyboard,
                                        contentDescription = null,
                                        tint = PrimaryAmber,
                                        modifier = Modifier.size(21.dp)
                                    )
                                    Spacer(Modifier.width(9.dp))
                                    Column {
                                        Text(
                                            "Use keyboard",
                                            color = OnSurfaceDark,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Return to Samsung Keyboard or Gboard",
                                            color = OnSurfaceVariantDark,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))

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
                                onSpace = { currentInputConnection?.commitText(" ", 1) },
                                onDelete = { currentInputConnection?.deleteSurroundingText(1, 0) },
                                onEnter = ::sendEnter
                            )
                        }
                    }
                }
            }
        }
    }

    private fun sendEnter() {
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    @Suppress("DEPRECATION")
    private fun switchBackToTypingKeyboard() {
        speechEngine.stopListening()

        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val switched = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchToPreviousInputMethod() || switchToNextInputMethod(false)
        } else {
            val token = window.window?.attributes?.token
            token != null && manager.switchToLastInputMethod(token)
        }

        if (!switched) {
            manager.showInputMethodPicker()
        }
    }

    override fun onStartInputView(attribute: EditorInfo?, restarting: Boolean) {
        installWindowTreeOwners()
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
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        speechEngine.destroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }
}
