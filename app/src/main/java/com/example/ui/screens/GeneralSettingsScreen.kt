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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import com.example.data.TakoFlowPreferences
import com.example.service.ImeStatus
import com.example.speech.AdaptiveLanguageModel
import com.example.speech.SpeechModels
import com.example.ui.components.GlassCard
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import kotlinx.coroutines.launch

@Composable
fun GeneralSettingsScreen(
    preferences: TakoFlowPreferences,
    onBack: () -> Unit,
    onNavigateToVoiceSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val adaptiveModel = remember { AdaptiveLanguageModel.get(context) }

    val autoStart by preferences.autoStartListening.collectAsState(initial = false)
    val keyboardMode by preferences.keyboardMode.collectAsState(initial = "Voice Only Mode")
    val model by preferences.inferenceModel.collectAsState(initial = SpeechModels.VOSK)
    val autoCorrect by preferences.autoCorrect.collectAsState(initial = true)
    val predictions by preferences.wordPredictions.collectAsState(initial = true)
    val learnHistory by preferences.learnFromTyping.collectAsState(initial = true)

    var imeEnabled by remember { mutableStateOf(false) }
    var imeSelected by remember { mutableStateOf(false) }

    fun refresh() {
        imeEnabled = ImeStatus.isEnabled(context)
        imeSelected = ImeStatus.isSelected(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        refresh()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            Text("Settings", color = OnSurfaceDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))
        SectionHeader("SYSTEM")
        GlassCard(modifier = Modifier.fillMaxWidth(), activeGlow = imeEnabled && imeSelected) {
            Column(modifier = Modifier.padding(16.dp)) {
                StatusRow("TakoFlow keyboard enabled", imeEnabled)
                Spacer(Modifier.height(8.dp))
                StatusRow("TakoFlow selected", imeSelected)
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionHeader("KEYBOARD")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingToggleRow(
                    title = "Start listening when opened",
                    subtitle = "Begin dictation when the TakoFlow keyboard appears",
                    checked = autoStart,
                    onCheckedChange = { scope.launch { preferences.setAutoStartListening(it) } }
                )
                SettingToggleRow(
                    title = "Autocorrect",
                    subtitle = "Correct typed words using local vocabulary and your profile",
                    checked = autoCorrect,
                    onCheckedChange = { scope.launch { preferences.setAutoCorrect(it) } }
                )
                SettingToggleRow(
                    title = "Word predictions",
                    subtitle = "Suggest the next word and complete the current word",
                    checked = predictions,
                    onCheckedChange = { scope.launch { preferences.setWordPredictions(it) } }
                )
                SettingToggleRow(
                    title = "Learn from typing",
                    subtitle = "Adapt locally to words and word pairs you use",
                    checked = learnHistory,
                    onCheckedChange = { scope.launch { preferences.setLearnFromTyping(it) } }
                )
                SettingClickableRow(
                    title = "Keyboard mode",
                    value = keyboardMode,
                    onClick = {
                        val next = if (keyboardMode == "Voice Only Mode") {
                            "Full Keyboard"
                        } else {
                            "Voice Only Mode"
                        }
                        scope.launch { preferences.setKeyboardMode(next) }
                    }
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionHeader("VOICE ENGINE")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            SettingClickableRow(
                title = "Inference model",
                value = model,
                onClick = onNavigateToVoiceSettings
            )
        }

        Spacer(Modifier.height(22.dp))
        SectionHeader("PRIVACY")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Local processing", color = OnSurfaceDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Speech, custom vocabulary and learned typing history remain on this device. " +
                            "TakoFlow disables learning and predictions in password fields.",
                        color = OnSurfaceVariantDark,
                        fontSize = 13.sp
                    )
                }
                SettingClickableRow(
                    title = "Learned typing data",
                    value = "Clear local history",
                    onClick = adaptiveModel::clearAll
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionHeader("ABOUT")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Version", color = OnSurfaceDark, fontSize = 16.sp)
                Text(BuildConfig.VERSION_NAME, color = OnSurfaceVariantDark, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatusRow(label: String, complete: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = OnSurfaceDark, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (complete) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = if (complete) ActiveGreen else PrimaryAmber,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                if (complete) "Ready" else "Required",
                color = if (complete) ActiveGreen else PrimaryAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        color = OnSurfaceVariantDark,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DarkBackground,
                checkedTrackColor = PrimaryAmber
            )
        )
    }
}

@Composable
private fun SettingClickableRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = OnSurfaceDark, fontSize = 15.sp)
            Text(value, color = PrimaryAmber, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnSurfaceVariantDark)
    }
}
