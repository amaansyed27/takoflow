package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TakoFlowPreferences
import com.example.service.FullKeyboardModeContent
import com.example.speech.LocalSpeechEngine
import com.example.speech.SpeechModels
import com.example.speech.SpeechState
import com.example.ui.components.WaveformMeter
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest

@Composable
fun VoiceDictationScreen(
    preferences: TakoFlowPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val speechEngine = remember { LocalSpeechEngine(context.applicationContext) }
    val speechState by speechEngine.speechState.collectAsState()
    val rmsDb by speechEngine.rmsDb.collectAsState()

    val model by preferences.inferenceModel.collectAsState(initial = SpeechModels.VOSK)
    val language by preferences.language.collectAsState(initial = "English (US)")
    val punctuation by preferences.punctuation.collectAsState(initial = true)
    val autoCaps by preferences.autoCapitalization.collectAsState(initial = true)
    val sound by preferences.soundFeedback.collectAsState(initial = true)
    val vibration by preferences.vibrationFeedback.collectAsState(initial = false)
    val profileId by preferences.activeProfileId.collectAsState(initial = "default")
    val savedKeyboardMode by preferences.keyboardMode.collectAsState(initial = "Voice Only Mode")

    var textInput by remember { mutableStateOf("") }
    var keyboardMode by remember(savedKeyboardMode) { mutableStateOf(savedKeyboardMode) }

    LaunchedEffect(model, language, punctuation, autoCaps, sound, vibration, profileId) {
        speechEngine.activeModel = model
        speechEngine.activeLanguage = language
        speechEngine.autoPunctuation = punctuation
        speechEngine.autoCapitalization = autoCaps
        speechEngine.soundFeedbackEnabled = sound
        speechEngine.vibrationFeedbackEnabled = vibration
        speechEngine.activeProfile = profileId
    }

    LaunchedEffect(speechState) {
        when (val state = speechState) {
            is SpeechState.Success -> {
                textInput = listOf(textInput.trim(), state.recognizedText)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                speechEngine.acknowledgeResult()
            }
            else -> Unit
        }
    }

    DisposableEffect(Unit) {
        onDispose { speechEngine.destroy() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryAmber)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Test dictation", color = OnSurfaceDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(model, color = OnSurfaceVariantDark, fontSize = 12.sp)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Your transcription appears here…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceContainerLow,
                    unfocusedContainerColor = SurfaceContainerLow,
                    focusedBorderColor = PrimaryAmber,
                    unfocusedBorderColor = PrimaryAmber.copy(alpha = 0.2f),
                    focusedTextColor = OnSurfaceDark,
                    unfocusedTextColor = OnSurfaceDark
                )
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when (val state = speechState) {
                        is SpeechState.Listening -> "Listening…"
                        is SpeechState.Processing -> state.partialText
                        is SpeechState.Error -> state.message
                        else -> "Tap the microphone to begin"
                    },
                    color = if (speechState is SpeechState.Error) {
                        androidx.compose.material3.MaterialTheme.colorScheme.error
                    } else {
                        OnSurfaceVariantDark
                    },
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                WaveformMeter(
                    isListening = speechState is SpeechState.Listening,
                    rmsLevel = rmsDb,
                    barCount = 7,
                    maxHeight = 22.dp
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(SurfaceContainerLowest)
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainer)
                            .clickable {
                                keyboardMode = if (keyboardMode == "Voice Only Mode") {
                                    "Full Keyboard"
                                } else {
                                    "Voice Only Mode"
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Keyboard, contentDescription = "Change keyboard mode", tint = PrimaryAmber)
                    }
                    Text(keyboardMode, color = OnSurfaceVariantDark, fontSize = 12.sp)
                    Spacer(Modifier.size(42.dp))
                }

                Spacer(Modifier.height(14.dp))

                if (keyboardMode == "Full Keyboard") {
                    FullKeyboardModeContent(
                        onKey = { textInput += it },
                        onDelete = { if (textInput.isNotEmpty()) textInput = textInput.dropLast(1) },
                        onEnter = { textInput += "\n" },
                        onMic = {
                            if (speechState is SpeechState.Listening) speechEngine.stopListening()
                            else speechEngine.startListening()
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(
                                if (speechState is SpeechState.Listening) PrimaryAmber
                                else SurfaceContainerHigh
                            )
                            .clickable {
                                if (speechState is SpeechState.Listening) speechEngine.stopListening()
                                else speechEngine.startListening()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Dictate",
                            tint = if (speechState is SpeechState.Listening) DarkBackground else PrimaryAmber,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TestKey(Modifier.width(62.dp), onClick = {
                            if (textInput.isNotEmpty()) textInput = textInput.dropLast(1)
                        }) {
                            Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = OnSurfaceDark)
                        }
                        TestKey(Modifier.weight(1f), onClick = { textInput += " " }) {
                            Text("Space", color = OnSurfaceVariantDark)
                        }
                        TestKey(Modifier.width(62.dp), onClick = { textInput += "\n" }, highlighted = true) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardReturn,
                                contentDescription = "Enter",
                                tint = DarkBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TestKey(
    modifier: Modifier,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (highlighted) PrimaryAmber else SurfaceContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
