package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.service.VoiceOnlyModeContent
import com.example.speech.LocalSpeechEngine
import com.example.speech.SpeechModels
import com.example.speech.SpeechState
import com.example.ui.components.WaveformMeter
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
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

    var textInput by remember { mutableStateOf("") }

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
        val state = speechState
        if (state is SpeechState.Success) {
            textInput = listOf(textInput.trim(), state.recognizedText)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            speechEngine.acknowledgeResult()
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
                Text("$model · voice-only preview", color = OnSurfaceVariantDark, fontSize = 12.sp)
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
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            VoiceOnlyModeContent(
                speechState = speechState,
                rmsDb = rmsDb,
                onMicClick = {
                    if (speechState is SpeechState.Listening) speechEngine.stopListening()
                    else speechEngine.startListening()
                },
                onDelete = {
                    if (textInput.isNotEmpty()) textInput = textInput.dropLast(1)
                },
                onSpace = { textInput += " " },
                onEnter = { textInput += "\n" }
            )
        }
    }
}
