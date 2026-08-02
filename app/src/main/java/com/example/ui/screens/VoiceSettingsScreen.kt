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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TakoFlowPreferences
import com.example.speech.ModelDownloadState
import com.example.speech.SpeechModelManager
import com.example.speech.SpeechModels
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import kotlinx.coroutines.launch

@Composable
fun VoiceSettingsScreen(
    preferences: TakoFlowPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { SpeechModelManager.get(context) }

    val selectedModel by preferences.inferenceModel.collectAsState(initial = SpeechModels.VOSK)
    val punctuation by preferences.punctuation.collectAsState(initial = true)
    val autoCaps by preferences.autoCapitalization.collectAsState(initial = true)
    val sound by preferences.soundFeedback.collectAsState(initial = true)
    val vibration by preferences.vibrationFeedback.collectAsState(initial = false)
    val voskState by manager.voskState.collectAsState()
    val whisperState by manager.whisperState.collectAsState()

    LaunchedEffect(Unit) {
        manager.refresh()
        if (selectedModel == SpeechModels.WHISPER_TINY && !manager.isWhisperInstalled()) {
            preferences.setInferenceModel(SpeechModels.VOSK)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryAmber
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Voice engine", color = OnSurfaceDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Offline models stored on this device", color = OnSurfaceVariantDark, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        ModelCard(
            title = "Vosk",
            subtitle = "Fast streaming dictation · about 40 MB",
            state = voskState,
            selected = selectedModel == SpeechModels.VOSK,
            canDelete = false,
            onDownload = manager::downloadVosk,
            onSelect = { scope.launch { preferences.setInferenceModel(SpeechModels.VOSK) } },
            onDelete = {}
        )

        Spacer(Modifier.height(14.dp))

        ModelCard(
            title = "Whisper Tiny",
            subtitle = "Higher accuracy · about 75 MB · transcribes after recording",
            state = whisperState,
            selected = selectedModel == SpeechModels.WHISPER_TINY,
            canDelete = whisperState.installed,
            onDownload = manager::downloadWhisper,
            onSelect = { scope.launch { preferences.setInferenceModel(SpeechModels.WHISPER_TINY) } },
            onDelete = {
                scope.launch {
                    if (selectedModel == SpeechModels.WHISPER_TINY) {
                        preferences.setInferenceModel(SpeechModels.VOSK)
                    }
                    manager.deleteWhisper()
                }
            }
        )

        Spacer(Modifier.height(28.dp))
        SectionLabel("DICTATION")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ToggleRow(
                    title = "Automatic punctuation",
                    subtitle = "Add sentence-ending punctuation",
                    checked = punctuation,
                    onChange = { scope.launch { preferences.setPunctuation(it) } }
                )
                ToggleRow(
                    title = "Automatic capitalization",
                    subtitle = "Capitalize the first word",
                    checked = autoCaps,
                    onChange = { scope.launch { preferences.setAutoCapitalization(it) } }
                )
                ToggleRow(
                    title = "Sound feedback",
                    subtitle = "Play a tone when listening starts or stops",
                    checked = sound,
                    onChange = { scope.launch { preferences.setSoundFeedback(it) } }
                )
                ToggleRow(
                    title = "Vibration feedback",
                    subtitle = "Vibrate when listening starts or stops",
                    checked = vibration,
                    onChange = { scope.launch { preferences.setVibrationFeedback(it) } }
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryAmber)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Private by default", color = OnSurfaceDark, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Audio is processed locally. Internet access is used only when you explicitly download a model.",
                        color = OnSurfaceVariantDark,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ModelCard(
    title: String,
    subtitle: String,
    state: ModelDownloadState,
    selected: Boolean,
    canDelete: Boolean,
    onDownload: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), activeGlow = selected) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, color = OnSurfaceDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        if (selected) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = PrimaryAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(subtitle, color = OnSurfaceVariantDark, fontSize = 13.sp)
                }
            }

            if (state.downloading) {
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { state.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text("Downloading ${state.progressPercent}%", color = OnSurfaceVariantDark, fontSize = 12.sp)
            }

            state.error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    !state.installed -> Button(
                        onClick = onDownload,
                        enabled = !state.downloading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryAmber,
                            contentColor = DarkBackground
                        )
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.downloading) "Downloading" else "Download")
                    }

                    !selected -> Button(
                        onClick = onSelect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryAmber,
                            contentColor = DarkBackground
                        )
                    ) {
                        Text("Use model")
                    }

                    else -> Text(
                        "Currently active",
                        color = PrimaryAmber,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }

                if (canDelete && state.installed) {
                    OutlinedButton(onClick = onDelete, enabled = !state.downloading) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Remove")
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = OnSurfaceDark, fontSize = 15.sp)
            Text(subtitle, color = OnSurfaceVariantDark, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DarkBackground,
                checkedTrackColor = PrimaryAmber
            )
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = OnSurfaceVariantDark,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}
