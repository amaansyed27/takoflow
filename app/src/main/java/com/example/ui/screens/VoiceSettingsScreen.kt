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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TakoFlowPreferences
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.OutlineVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SurfaceContainerLow
import kotlinx.coroutines.launch

@Composable
fun VoiceSettingsScreen(
    preferences: TakoFlowPreferences,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val model by preferences.inferenceModel.collectAsState(initial = "TakoFlow Whisper Small")
    val language by preferences.language.collectAsState(initial = "English (US)")
    val punctuation by preferences.punctuation.collectAsState(initial = true)
    val autoCaps by preferences.autoCapitalization.collectAsState(initial = true)
    val sound by preferences.soundFeedback.collectAsState(initial = true)
    val vibration by preferences.vibrationFeedback.collectAsState(initial = false)

    var showLanguageMenu by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }

    val availableLanguages = listOf("English (US)", "English (UK)", "Spanish", "French", "German", "Japanese")
    val availableModels = listOf("TakoFlow Whisper Small", "Vosk Offline Engine", "Android On-Device Engine")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OnSurfaceVariantDark
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Voice Settings",
                color = PrimaryAmber,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(40.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Language Selector
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Language", color = OnSurfaceVariantDark, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))

            Box {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLow)
                        .clickable { showLanguageMenu = true }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(language, color = OnSurfaceDark, fontSize = 16.sp)
                        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = OutlineVariantDark)
                    }
                }

                DropdownMenu(
                    expanded = showLanguageMenu,
                    onDismissRequest = { showLanguageMenu = false }
                ) {
                    availableLanguages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang) },
                            onClick = {
                                scope.launch { preferences.setLanguage(lang) }
                                showLanguageMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Model Selector
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Model", color = OnSurfaceVariantDark, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))

            Box {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLow)
                        .clickable { showModelMenu = true }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(model, color = OnSurfaceDark, fontSize = 16.sp)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OutlineVariantDark)
                    }
                }

                DropdownMenu(
                    expanded = showModelMenu,
                    onDismissRequest = { showModelMenu = false }
                ) {
                    availableModels.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m) },
                            onClick = {
                                scope.launch { preferences.setInferenceModel(m) }
                                showModelMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Toggles List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Punctuation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Punctuation", color = OnSurfaceDark, fontSize = 16.sp)
                    Text("Auto add punctuation", color = OnSurfaceVariantDark, fontSize = 13.sp)
                }
                Switch(
                    checked = punctuation,
                    onCheckedChange = { scope.launch { preferences.setPunctuation(it) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = PrimaryAmber)
                )
            }

            // Auto Capitalization
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto Capitalization", color = OnSurfaceDark, fontSize = 16.sp)
                    Text("Capitalize sentence start", color = OnSurfaceVariantDark, fontSize = 13.sp)
                }
                Switch(
                    checked = autoCaps,
                    onCheckedChange = { scope.launch { preferences.setAutoCapitalization(it) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = PrimaryAmber)
                )
            }

            // Sound Feedback
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Sound Feedback", color = OnSurfaceDark, fontSize = 16.sp)
                    Text("Play sound on start/stop", color = OnSurfaceVariantDark, fontSize = 13.sp)
                }
                Switch(
                    checked = sound,
                    onCheckedChange = { scope.launch { preferences.setSoundFeedback(it) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = PrimaryAmber)
                )
            }

            // Vibration Feedback
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Vibration Feedback", color = OnSurfaceDark, fontSize = 16.sp)
                    Text("Vibrate on start/stop", color = OnSurfaceVariantDark, fontSize = 13.sp)
                }
                Switch(
                    checked = vibration,
                    onCheckedChange = { scope.launch { preferences.setVibrationFeedback(it) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = PrimaryAmber)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Tip Box
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = PrimaryAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Tip", color = OnSurfaceDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("You can change these settings anytime to match your style.", color = OnSurfaceVariantDark, fontSize = 14.sp)
                }
            }
        }
    }
}
