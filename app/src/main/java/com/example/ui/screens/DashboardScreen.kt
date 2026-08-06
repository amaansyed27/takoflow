package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.core.content.ContextCompat
import com.example.data.TakoFlowPreferences
import com.example.service.ImeStatus
import com.example.speech.FormattingProfileStore
import com.example.speech.SpeechModelManager
import com.example.speech.SpeechModels
import com.example.ui.components.BrandHeader
import com.example.ui.components.GlassCard
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    preferences: TakoFlowPreferences,
    onNavigateToEnable: () -> Unit,
    onNavigateToVoiceSettings: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToGeneralSettings: () -> Unit,
    onNavigateToSwitchingGuide: () -> Unit,
    onNavigateToDictation: () -> Unit
) {
    val context = LocalContext.current
    val modelManager = remember { SpeechModelManager.get(context) }
    val profileStore = remember { FormattingProfileStore.get(context) }
    val model by preferences.inferenceModel.collectAsState(initial = SpeechModels.VOSK)
    val profileId by preferences.activeProfileId.collectAsState(initial = "default")
    val profiles by profileStore.profiles.collectAsState()
    val voskState by modelManager.voskState.collectAsState()
    val whisperState by modelManager.whisperState.collectAsState()
    var imeEnabled by remember { mutableStateOf(false) }
    var imeSelected by remember { mutableStateOf(false) }
    var microphoneGranted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            imeEnabled = ImeStatus.isEnabled(context)
            imeSelected = ImeStatus.isSelected(context)
            microphoneGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            modelManager.refresh()
            delay(1_000)
        }
    }

    val selectedModelReady = if (model == SpeechModels.WHISPER_TINY) {
        whisperState.installed
    } else {
        voskState.installed
    }
    val ready = imeEnabled && imeSelected && microphoneGranted && selectedModelReady
    val profileName = profiles.firstOrNull { it.id == profileId }?.name ?: "Default"

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        BrandHeader("TakoFlow", "Voice typing is ready when every check is green")
        Spacer(Modifier.height(22.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), activeGlow = ready) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (ready) Icons.Default.CheckCircle else Icons.Default.Mic,
                        null,
                        tint = if (ready) ActiveGreen else PrimaryAmber,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.size(13.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (ready) "Ready to dictate" else "TakoFlow needs attention",
                            color = OnSurfaceDark,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$model · $profileName",
                            color = OnSurfaceVariantDark,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                StatusLine("Keyboard enabled", imeEnabled)
                StatusLine("TakoFlow selected", imeSelected)
                StatusLine("Microphone allowed", microphoneGranted)
                StatusLine("Selected model installed", selectedModelReady)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = if (ready) onNavigateToDictation else onNavigateToEnable,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(PrimaryAmber, DarkBackground)
                ) {
                    Text(if (ready) "Test dictation" else "Finish setup", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("QUICK ACTIONS", color = OnSurfaceVariantDark, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(9.dp))
        ActionCard(Icons.Default.Mic, "Voice engine", model, onNavigateToVoiceSettings)
        Spacer(Modifier.height(10.dp))
        ActionCard(Icons.Default.Style, "Formatting profiles", profileName, onNavigateToProfiles)
        Spacer(Modifier.height(10.dp))
        ActionCard(Icons.Default.SwapHoriz, "Switching keyboards", "TakoFlow ↔ normal keyboard", onNavigateToSwitchingGuide)
        Spacer(Modifier.height(10.dp))
        ActionCard(Icons.Default.Settings, "Settings", "Feedback, setup and app details", onNavigateToGeneralSettings)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatusLine(label: String, complete: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = OnSurfaceVariantDark, fontSize = 13.sp)
        Text(
            if (complete) "READY" else "REQUIRED",
            color = if (complete) ActiveGreen else PrimaryAmber,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = PrimaryAmber, modifier = Modifier.size(25.dp))
            Spacer(Modifier.size(13.dp))
            Column {
                Text(title, color = OnSurfaceDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = OnSurfaceVariantDark, fontSize = 12.sp)
            }
        }
    }
}
