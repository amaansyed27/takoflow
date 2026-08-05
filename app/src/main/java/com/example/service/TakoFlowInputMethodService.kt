package com.example.service

import android.inputmethodservice.InputMethodService
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.speech.LocalSpeechEngine
import com.example.speech.SpeechModels
import com.example.speech.SpeechState
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.SurfaceContainerLowest

class TakoFlowInputMethodService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    private lateinit var preferences: TakoFlowPreferences
    private lateinit var speechEngine: LocalSpeechEngine

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        preferences = TakoFlowPreferences(applicationContext)
        speechEngine = LocalSpeechEngine(applicationContext)
    }

    override fun onCreateInputView(): View = ComposeView(this).apply {
        setViewTreeLifecycleOwner(this@TakoFlowInputMethodService)
        setViewTreeSavedStateRegistryOwner(this@TakoFlowInputMethodService)
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
                val speechState by speechEngine.speechState.collectAsState()
                val rmsDb by speechEngine.rmsDb.collectAsState()

                LaunchedEffect(model, language, punctuation, autoCaps, sound, vibration, profileId) {
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
                    when (val state = speechState) {
                        is SpeechState.Success -> {
                            currentInputConnection?.commitText(state.recognizedText + " ", 1)
                            speechEngine.acknowledgeResult()
                        }
                        else -> Unit
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), color = SurfaceContainerLowest) {
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
                                    else -> "$model · Tap mic"
                                },
                                color = when (speechState) {
                                    is SpeechState.Error -> androidx.compose.material3.MaterialTheme.colorScheme.error
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
                                    if (speechState is SpeechState.Listening) speechEngine.stopListening()
                                    else speechEngine.startListening()
                                },
                                onSpace = { currentInputConnection?.commitText(" ", 1) },
                                onDelete = { currentInputConnection?.deleteSurroundingText(1, 0) },
                                onEnter = {
                                    currentInputConnection?.sendKeyEvent(
                                        KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
                                    )
                                    currentInputConnection?.sendKeyEvent(
                                        KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
                                    )
                                }
                            )
                        } else {
                            FullKeyboardModeContent(
                                onKey = { currentInputConnection?.commitText(it, 1) },
                                onDelete = { currentInputConnection?.deleteSurroundingText(1, 0) },
                                onEnter = {
                                    currentInputConnection?.sendKeyEvent(
                                        KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
                                    )
                                    currentInputConnection?.sendKeyEvent(
                                        KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
                                    )
                                },
                                onMic = {
                                    if (speechState is SpeechState.Listening) speechEngine.stopListening()
                                    else speechEngine.startListening()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStartInputView(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
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

@Composable
fun VoiceOnlyModeContent(
    speechState: SpeechState,
    rmsDb: Float,
    onMicClick: () -> Unit,
    onSpace: () -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size((80 + (rmsDb * 10)).dp)
                    .clip(CircleShape)
                    .background(
                        if (speechState is SpeechState.Listening) PrimaryAmber
                        else SurfaceContainerHigh
                    )
                    .clickable(onClick = onMicClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Dictate",
                    tint = if (speechState is SpeechState.Listening) DarkBackground else PrimaryAmber,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UtilityKey(modifier = Modifier.width(60.dp), onClick = onDelete) {
                Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = OnSurfaceDark)
            }
            UtilityKey(modifier = Modifier.weight(1f), onClick = onSpace) {
                Text("Space", color = OnSurfaceVariantDark, fontSize = 14.sp)
            }
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryAmber)
                    .clickable(onClick = onEnter),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardReturn, contentDescription = "Return", tint = DarkBackground)
            }
        }
    }
}

@Composable
private fun UtilityKey(
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun FullKeyboardModeContent(
    onKey: (String) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    onMic: () -> Unit
) {
    val rows = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("Z", "X", "C", "V", "B", "N", "M")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
            ) {
                rowKeys.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceContainerHighest)
                            .clickable { onKey(key.lowercase()) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(key, color = OnSurfaceDark, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(PrimaryAmber)
                    .clickable(onClick = onMic),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Voice dictation", tint = DarkBackground)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceContainerHighest)
                    .clickable { onKey(" ") },
                contentAlignment = Alignment.Center
            ) {
                Text("English", color = OnSurfaceVariantDark, fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceContainerHigh)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = OnSurfaceDark)
            }
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(PrimaryAmber)
                    .clickable(onClick = onEnter),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardReturn, contentDescription = "Enter", tint = DarkBackground)
            }
        }
    }
}
