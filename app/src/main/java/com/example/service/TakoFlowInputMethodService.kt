package com.example.service

import android.inputmethodservice.InputMethodService
import android.view.View
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
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TakoFlowPreferences
import com.example.speech.LocalSpeechEngine
import com.example.speech.SpeechState
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.OutlineVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.SurfaceContainerHighest

class TakoFlowInputMethodService : InputMethodService() {

    private lateinit var preferences: TakoFlowPreferences
    private lateinit var speechEngine: LocalSpeechEngine

    override fun onCreate() {
        super.onCreate()
        preferences = TakoFlowPreferences(applicationContext)
        speechEngine = LocalSpeechEngine(applicationContext)
    }

    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
            setContent {
                MyApplicationTheme {
                    val mode by preferences.keyboardMode.collectAsState(initial = "Voice Only Mode")
                    val model by preferences.inferenceModel.collectAsState(initial = "Vosk")
                    val language by preferences.language.collectAsState(initial = "English (US)")
                    val punctuation by preferences.punctuation.collectAsState(initial = true)
                    val autoCaps by preferences.autoCapitalization.collectAsState(initial = true)
                    val sound by preferences.soundFeedback.collectAsState(initial = true)
                    val vibration by preferences.vibrationFeedback.collectAsState(initial = false)
                    val profileId by preferences.activeProfileId.collectAsState(initial = "default")
                    val isWhisperInstalled by preferences.isWhisperInstalled.collectAsState(initial = false)
                    
                    LaunchedEffect(model, language, punctuation, autoCaps, sound, vibration, profileId, isWhisperInstalled) {
                        speechEngine.activeModel = model
                        speechEngine.activeLanguage = language
                        speechEngine.autoPunctuation = punctuation
                        speechEngine.autoCapitalization = autoCaps
                        speechEngine.soundFeedbackEnabled = sound
                        speechEngine.vibrationFeedbackEnabled = vibration
                        speechEngine.activeProfile = profileId
                        speechEngine.isWhisperInstalled = isWhisperInstalled
                    }
                    val speechState by speechEngine.speechState.collectAsState()
                    val rmsDb by speechEngine.rmsDb.collectAsState()

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
                            // Top Bar with Waveform and Mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TAKOFLOW IME",
                                    color = PrimaryAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                when (val state = speechState) {
                                    is SpeechState.Listening -> {
                                        Text(
                                            text = "Listening...",
                                            color = PrimaryAmber,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    is SpeechState.Processing -> {
                                        Text(
                                            text = state.partialText,
                                            color = OnSurfaceDark,
                                            fontSize = 12.sp
                                        )
                                    }
                                    is SpeechState.Success -> {
                                        currentInputConnection?.commitText(state.recognizedText + " ", 1)
                                        Text(
                                            text = "Transcribed ✓",
                                            color = PrimaryAmber,
                                            fontSize = 12.sp
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = "Tap Mic to Dictate",
                                            color = OnSurfaceVariantDark,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

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
                                    onSpace = { currentInputConnection?.commitText(" ", 1) },
                                    onDelete = { currentInputConnection?.deleteSurroundingText(1, 0) },
                                    onEnter = { currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER)) }
                                )
                            } else {
                                FullKeyboardModeContent(
                                    onKey = { text -> currentInputConnection?.commitText(text, 1) },
                                    onDelete = { currentInputConnection?.deleteSurroundingText(1, 0) },
                                    onEnter = { currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER)) },
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

    override fun onDestroy() {
        super.onDestroy()
        speechEngine.destroy()
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
        // Center Mic Button
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(PrimaryAmber)
                    .clickable { onMicClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Dictate",
                    tint = DarkBackground,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Action Keys Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainerHigh)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Backspace",
                    tint = OnSurfaceDark
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainerHigh)
                    .clickable { onSpace() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Space",
                    color = OnSurfaceVariantDark,
                    fontSize = 14.sp
                )
            }

            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryAmber)
                    .clickable { onEnter() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardReturn,
                    contentDescription = "Return",
                    tint = DarkBackground
                )
            }
        }
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
                        Text(
                            text = key,
                            color = OnSurfaceDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Bottom Row with Mic, Space, Backspace, Return
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
                    .clickable { onMic() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Dictation",
                    tint = DarkBackground
                )
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
                    .clickable { onDelete() },
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
                    .clickable { onEnter() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardReturn, contentDescription = "Enter", tint = DarkBackground)
            }
        }
    }
}
