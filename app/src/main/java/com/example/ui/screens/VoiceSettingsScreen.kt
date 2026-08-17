package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.TakoFlowAppContainer
import com.example.speech.ModelDownloadState
import com.example.speech.SpeechModels
import com.example.speech.WhisperModes
import com.example.ui.components.BrandHeader
import com.example.ui.components.GlassCard
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.viewmodel.VoiceSettingsViewModel
import com.example.ui.viewmodel.takoFlowViewModel
import java.util.Locale

@Composable
fun VoiceSettingsScreen(
    container: TakoFlowAppContainer,
    onBack: () -> Unit
) {
    val viewModel: VoiceSettingsViewModel = takoFlowViewModel {
        VoiceSettingsViewModel(
            settings = container.settings,
            models = container.models
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        BrandHeader("Voice engine", "Choose and tune on-device speech", onBack = onBack)
        Spacer(Modifier.height(22.dp))

        ModelCard(
            title = "Vosk Small English",
            subtitle = "Default · true streaming · low latency · about 40 MB",
            state = uiState.vosk,
            selected = uiState.selectedModel == SpeechModels.VOSK,
            onSelect = { viewModel.selectModel(SpeechModels.VOSK) },
            onDownload = viewModel::downloadVosk,
            onCancel = viewModel::cancelVoskDownload,
            onDelete = viewModel::deleteVosk
        )
        Spacer(Modifier.height(12.dp))
        ModelCard(
            title = "Whisper Tiny English",
            subtitle = "Optional · higher accuracy · batch or live · about 75 MB",
            state = uiState.whisper,
            selected = uiState.selectedModel == SpeechModels.WHISPER_TINY,
            onSelect = { viewModel.selectModel(SpeechModels.WHISPER_TINY) },
            onDownload = viewModel::downloadWhisper,
            onCancel = viewModel::cancelWhisperDownload,
            onDelete = viewModel::deleteWhisper
        )

        if (uiState.whisper.installed) {
            Spacer(Modifier.height(22.dp))
            SectionLabel("WHISPER TRANSCRIPTION")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WhisperModeButton(
                            label = "Batch",
                            selected = uiState.whisperMode == WhisperModes.BATCH,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setWhisperMode(WhisperModes.BATCH) }
                        )
                        WhisperModeButton(
                            label = "Live",
                            selected = uiState.whisperMode == WhisperModes.LIVE,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setWhisperMode(WhisperModes.LIVE) }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (uiState.whisperMode == WhisperModes.LIVE) {
                            "Live mode repeatedly transcribes the in-progress recording so text can update while you speak. It uses more CPU than Batch mode."
                        } else {
                            "Batch mode records first and runs one final transcription after you stop. It is the most stable and efficient Whisper mode."
                        },
                        color = OnSurfaceVariantDark,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("TEXT QUALITY")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                VoiceToggle(
                    "Automatic punctuation",
                    "Choose a sensible sentence ending when a profile allows it",
                    uiState.punctuation,
                    viewModel::setPunctuation
                )
                VoiceToggle(
                    "Automatic capitalization",
                    "Capitalize sentence starts in completed dictation",
                    uiState.autoCapitalization,
                    viewModel::setAutoCapitalization
                )
                VoiceToggle(
                    "Grammar cleanup",
                    "Fix common contractions, duplicate words and obvious question endings",
                    uiState.grammarCorrection,
                    viewModel::setGrammarCorrection
                )
                VoiceToggle(
                    "Spell correction",
                    "Conservatively correct common misspellings and profile-preferred spellings",
                    uiState.spellCorrection,
                    viewModel::setSpellCorrection
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "All recognition and correction runs locally. Whisper Live trades battery and CPU for lower visible latency; Batch performs one transcription after recording.",
            color = OnSurfaceVariantDark,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = OnSurfaceVariantDark,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun WhisperModeButton(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(PrimaryAmber, DarkBackground)
        ) {
            Text(label, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    }
}

@Composable
private fun ModelCard(
    title: String,
    subtitle: String,
    state: ModelDownloadState,
    selected: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), activeGlow = selected) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (state.installed) Icons.Default.Memory else Icons.Default.CloudDownload,
                    null,
                    tint = if (state.installed) ActiveGreen else PrimaryAmber
                )
                Spacer(Modifier.padding(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = OnSurfaceDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = OnSurfaceVariantDark, fontSize = 12.sp, lineHeight = 17.sp)
                }
                if (selected) {
                    Text("ACTIVE", color = PrimaryAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (state.downloading) {
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { state.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    buildString {
                        append("${state.progressPercent}%")
                        if (state.downloadedBytes > 0) {
                            append(" · ${formatBytes(state.downloadedBytes)}")
                            if (state.totalBytes > 0) append(" / ${formatBytes(state.totalBytes)}")
                        }
                    },
                    color = OnSurfaceVariantDark,
                    fontSize = 11.sp
                )
            }

            state.error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                when {
                    state.downloading -> {
                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.StopCircle, null)
                            Spacer(Modifier.padding(3.dp))
                            Text("Cancel")
                        }
                    }
                    !state.installed -> {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(PrimaryAmber, DarkBackground)
                        ) {
                            Text(if (state.error == null) "Download" else "Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                    !selected -> {
                        Button(
                            onClick = onSelect,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(PrimaryAmber, DarkBackground)
                        ) {
                            Icon(Icons.Default.CheckCircle, null)
                            Spacer(Modifier.padding(3.dp))
                            Text("Use model", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Delete model")
                        }
                    }
                    else -> {
                        Text(
                            "Selected and ready",
                            color = ActiveGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = OnSurfaceDark, fontSize = 15.sp)
            Text(subtitle, color = OnSurfaceVariantDark, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val mb = bytes / (1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f MB", mb)
}
