package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TakoFlowPreferences
import com.example.service.VoiceKeyboardPanel
import com.example.speech.FormattingProfileStore
import com.example.speech.LocalSpeechEngine
import com.example.speech.SpeechModels
import com.example.speech.SpeechState
import com.example.ui.components.BrandHeader
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SurfaceContainerLow

@Composable
fun VoiceDictationScreen(
    preferences: TakoFlowPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val speechEngine = remember { LocalSpeechEngine(context.applicationContext) }
    val profileStore = remember { FormattingProfileStore.get(context) }
    val speechState by speechEngine.speechState.collectAsState()
    val rmsDb by speechEngine.rmsDb.collectAsState()
    val profiles by profileStore.profiles.collectAsState()
    val model by preferences.inferenceModel.collectAsState(initial = SpeechModels.VOSK)
    val punctuation by preferences.punctuation.collectAsState(initial = true)
    val autoCaps by preferences.autoCapitalization.collectAsState(initial = true)
    val sound by preferences.soundFeedback.collectAsState(initial = true)
    val vibration by preferences.vibrationFeedback.collectAsState(initial = false)
    val profileId by preferences.activeProfileId.collectAsState(initial = "default")
    val profileName = profiles.firstOrNull { it.id == profileId }?.name ?: "Default"
    var textInput by remember { mutableStateOf("") }

    LaunchedEffect(model, punctuation, autoCaps, sound, vibration, profileId) {
        speechEngine.activeModel = model
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
                .filter(String::isNotBlank)
                .joinToString(" ")
            speechEngine.acknowledgeResult()
        }
    }

    DisposableEffect(Unit) {
        onDispose { speechEngine.destroy() }
    }

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        BrandHeader(
            title = "Test dictation",
            subtitle = "$model · $profileName",
            onBack = onBack,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
        )

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Your transcription appears here…") },
                modifier = Modifier.fillMaxWidth().height(210.dp),
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceContainerLow,
                    unfocusedContainerColor = SurfaceContainerLow,
                    focusedBorderColor = PrimaryAmber,
                    unfocusedBorderColor = PrimaryAmber.copy(alpha = 0.22f),
                    focusedTextColor = OnSurfaceDark,
                    unfocusedTextColor = OnSurfaceDark
                )
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DictationAction(Icons.Default.ContentCopy, "Copy", textInput.isNotBlank(), Modifier.weight(1f)) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("TakoFlow dictation", textInput))
                }
                DictationAction(Icons.Default.Share, "Share", textInput.isNotBlank(), Modifier.weight(1f)) {
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, textInput)
                            },
                            "Share dictation"
                        )
                    )
                }
                DictationAction(Icons.Default.Clear, "Clear", textInput.isNotBlank(), Modifier.weight(1f)) {
                    textInput = ""
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                when (val state = speechState) {
                    is SpeechState.Error -> state.message
                    is SpeechState.Processing -> state.partialText
                    is SpeechState.Listening -> "Listening…"
                    else -> "Use this screen to verify the same engine and profile used by the IME."
                },
                color = if (speechState is SpeechState.Error) {
                    androidx.compose.material3.MaterialTheme.colorScheme.error
                } else OnSurfaceVariantDark,
                fontSize = 12.sp
            )
        }

        VoiceKeyboardPanel(
            state = speechState,
            rmsDb = rmsDb,
            modelName = model,
            profileName = profileName,
            sensitiveField = false,
            onMicClick = {
                val recording = speechState is SpeechState.Listening ||
                    (model == SpeechModels.VOSK && speechState is SpeechState.Processing)
                if (recording) speechEngine.stopListening()
                else if (speechState is SpeechState.Idle || speechState is SpeechState.Error) {
                    speechEngine.startListening()
                }
            },
            onSwitchKeyboard = {
                val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                manager.showInputMethodPicker()
            },
            onSpace = { textInput += " " },
            onDelete = { if (textInput.isNotEmpty()) textInput = textInput.dropLast(1) },
            onEnter = { textInput += "\n" }
        )
    }
}

@Composable
private fun DictationAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val tint = if (enabled) PrimaryAmber else OnSurfaceVariantDark.copy(alpha = 0.45f)
    Box(
        modifier = modifier.height(46.dp).background(
            SurfaceContainerLow,
            RoundedCornerShape(12.dp)
        ).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
