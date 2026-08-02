package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TakoFlowPreferences
import com.example.speech.LocalSpeechEngine
import com.example.speech.SpeechState
import com.example.service.FullKeyboardModeContent
import com.example.ui.components.WaveformMeter
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.OutlineVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import kotlinx.coroutines.launch

@Composable
fun VoiceDictationScreen(
    preferences: TakoFlowPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val speechEngine = remember { LocalSpeechEngine(context) }
    val speechState by speechEngine.speechState.collectAsState()
    val rmsDb by speechEngine.rmsDb.collectAsState()

    val model by preferences.inferenceModel.collectAsState(initial = "TakoFlow Whisper Small")
    val language by preferences.language.collectAsState(initial = "English (US)")
    val punctuation by preferences.punctuation.collectAsState(initial = true)
    val autoCaps by preferences.autoCapitalization.collectAsState(initial = true)
    val sound by preferences.soundFeedback.collectAsState(initial = true)
    val vibration by preferences.vibrationFeedback.collectAsState(initial = false)
    val isWhisperInstalled by preferences.isWhisperInstalled.collectAsState(initial = false)
    val profileId by preferences.activeProfileId.collectAsState(initial = "default")

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

    var textInput by remember { mutableStateOf("") }
    var keyboardMode by remember { mutableStateOf("Voice Only Mode") } // "Voice Only Mode" or "Full Keyboard"

    // When speech recognition produces result, append to text input!
    LaunchedEffect(speechState) {
        val state = speechState
        if (state is SpeechState.Success) {
            textInput = if (textInput.isBlank()) {
                state.recognizedText
            } else {
                "$textInput ${state.recognizedText}"
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "micPulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top App Bar
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
                    .clickable {
                        speechEngine.destroy()
                        onBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OnSurfaceDark
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Search or type",
                color = OnSurfaceDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Live Dictation Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Text Field
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Search or type...", color = OnSurfaceVariantDark) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceContainerLow,
                    unfocusedContainerColor = SurfaceContainerLow,
                    focusedBorderColor = PrimaryAmber,
                    unfocusedBorderColor = PrimaryAmber.copy(alpha = 0.15f),
                    focusedTextColor = OnSurfaceDark,
                    unfocusedTextColor = OnSurfaceDark
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        if (speechState is SpeechState.Listening) {
                            speechEngine.stopListening()
                        } else {
                            speechEngine.startListening()
                        }
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = "Dictate", tint = PrimaryAmber)
                    }
                }
            )

            // Dynamic Listening Status Banner
            when (val state = speechState) {
                is SpeechState.Listening -> {
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceContainerHigh)
                            .border(1.dp, PrimaryAmber.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Listening...", color = PrimaryAmber, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        WaveformMeter(isListening = true, rmsLevel = rmsDb, barCount = 5, maxHeight = 18.dp)
                    }
                }
                is SpeechState.Processing -> {
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceContainerHigh)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(state.partialText, color = OnSurfaceDark, fontSize = 14.sp)
                    }
                }
                else -> {}
            }
        }

        // Voice Mode / Keyboard Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(SurfaceContainerLowest)
                .border(1.dp, PrimaryAmber.copy(alpha = 0.15f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header / Mode Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainer)
                            .clickable {
                                keyboardMode = if (keyboardMode == "Voice Only Mode") "Full Keyboard" else "Voice Only Mode"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Toggle Keyboard Mode",
                            tint = OnSurfaceVariantDark
                        )
                    }

                    WaveformMeter(
                        isListening = speechState is SpeechState.Listening,
                        rmsLevel = rmsDb,
                        barCount = 7,
                        maxHeight = 20.dp
                    )

                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Options",
                        tint = OnSurfaceVariantDark
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (keyboardMode == "Voice Only Mode") {
                    // Large Central Pulsing Mic Button
                    Box(
                        modifier = Modifier
                            .size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (speechState is SpeechState.Listening) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp * pulseScale)
                                    .border(1.dp, PrimaryAmber, CircleShape)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerHigh)
                                .border(1.dp, PrimaryAmber.copy(alpha = 0.3f), CircleShape)
                                .clickable {
                                    if (speechState is SpeechState.Listening) {
                                        speechEngine.stopListening()
                                    } else {
                                        speechEngine.startListening()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Mic",
                                tint = PrimaryAmber,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Utility Keys Row (Backspace, Space, Return)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceContainer)
                                .clickable {
                                    if (textInput.isNotEmpty()) {
                                        textInput = textInput.dropLast(1)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = OnSurfaceDark)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceContainer)
                                .clickable { textInput += " " },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Space", color = OnSurfaceVariantDark, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }

                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryAmber)
                                .clickable { textInput += "\n" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = "Enter", tint = DarkBackground)
                        }
                    }
                } else {
                    // Full QWERTY Keyboard
                    FullKeyboardModeContent(
                        onKey = { key -> textInput += key },
                        onDelete = {
                            if (textInput.isNotEmpty()) {
                                textInput = textInput.dropLast(1)
                            }
                        },
                        onEnter = { textInput += "\n" },
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
