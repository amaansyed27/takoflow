package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.TakoFlowPreferences
import com.example.service.ImeStatus
import com.example.speech.SpeechModelManager
import com.example.speech.SpeechModels
import com.example.ui.components.GlassCard
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber

@Composable
fun DashboardScreen(
    preferences: TakoFlowPreferences,
    onNavigateToEnable: () -> Unit,
    onNavigateToVoiceSettings: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToGeneralSettings: () -> Unit,
    onNavigateToDictation: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val manager = remember { SpeechModelManager.get(context) }
    val selectedModel by preferences.inferenceModel.collectAsState(initial = SpeechModels.VOSK)
    val voskState by manager.voskState.collectAsState()
    val whisperState by manager.whisperState.collectAsState()

    var imeEnabled by remember { mutableStateOf(false) }
    var imeSelected by remember { mutableStateOf(false) }
    var microphoneGranted by remember { mutableStateOf(false) }

    fun refreshStatus() {
        imeEnabled = ImeStatus.isEnabled(context)
        imeSelected = ImeStatus.isSelected(context)
        microphoneGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        manager.refresh()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        refreshStatus()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val modelInstalled = when (selectedModel) {
        SpeechModels.WHISPER_TINY -> whisperState.installed
        else -> voskState.installed
    }
    val ready = imeEnabled && imeSelected && microphoneGranted && modelInstalled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "TAKOFLOW",
                color = PrimaryAmber,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onNavigateToGeneralSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = OnSurfaceDark)
            }
        }

        Spacer(Modifier.height(20.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), activeGlow = ready) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("VOICE KEYBOARD", color = OnSurfaceVariantDark, fontSize = 12.sp, letterSpacing = 1.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (ready) "READY" else "SETUP REQUIRED",
                                color = if (ready) ActiveGreen else PrimaryAmber,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(if (ready) ActiveGreen else PrimaryAmber)
                            )
                        }
                    }
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = PrimaryAmber,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                StatusLine("Keyboard enabled", imeEnabled)
                StatusLine("TakoFlow selected", imeSelected)
                StatusLine("Microphone permission", microphoneGranted)
                StatusLine("$selectedModel installed", modelInstalled)
            }
        }

        if (!ready) {
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = if (!modelInstalled) onNavigateToVoiceSettings else onNavigateToEnable,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryAmber,
                    contentColor = DarkBackground
                )
            ) {
                Text(if (!modelInstalled) "Download required model" else "Finish keyboard setup")
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("SETTINGS", color = OnSurfaceVariantDark, fontSize = 12.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(10.dp))

        ActionCard(
            icon = Icons.Default.Keyboard,
            title = "Keyboard setup",
            subtitle = if (imeSelected) "TakoFlow is selected" else "Enable and select TakoFlow",
            onClick = onNavigateToEnable
        )
        Spacer(Modifier.height(10.dp))
        ActionCard(
            icon = Icons.Default.Mic,
            title = "Voice engine",
            subtitle = selectedModel,
            onClick = onNavigateToVoiceSettings
        )
        Spacer(Modifier.height(10.dp))
        ActionCard(
            icon = Icons.Default.GraphicEq,
            title = "Formatting profiles",
            subtitle = "Work, notes and general dictation",
            onClick = onNavigateToProfiles
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onNavigateToDictation,
            enabled = ready,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryAmber,
                contentColor = DarkBackground
            )
        ) {
            Icon(Icons.Default.Mic, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Test voice typing", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusLine(label: String, complete: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = OnSurfaceVariantDark, fontSize = 13.sp)
        Text(
            if (complete) "OK" else "Missing",
            color = if (complete) ActiveGreen else PrimaryAmber,
            fontSize = 12.sp,
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
            Icon(icon, contentDescription = null, tint = PrimaryAmber)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, color = OnSurfaceDark, fontWeight = FontWeight.Bold)
                Text(subtitle, color = OnSurfaceVariantDark, fontSize = 12.sp)
            }
        }
    }
}
