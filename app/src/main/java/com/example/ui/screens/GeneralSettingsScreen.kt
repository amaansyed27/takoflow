package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TakoFlowPreferences
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
    val scope = rememberCoroutineScope()

    val autoStart by preferences.autoStartListening.collectAsState(initial = false)
    val keepActive by preferences.keepKeyboardActive.collectAsState(initial = true)
    val localProcessing by preferences.localProcessing.collectAsState(initial = true)
    val analytics by preferences.analytics.collectAsState(initial = false)
    val keyboardMode by preferences.keyboardMode.collectAsState(initial = "Voice Only Mode")
    val model by preferences.inferenceModel.collectAsState(initial = "Vosk")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar Header
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
                    tint = PrimaryAmber
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Settings",
                color = OnSurfaceDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(40.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SYSTEM
        SectionHeader("SYSTEM")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Configuration Status", color = OnSurfaceDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("IME & Voice Typing", color = OnSurfaceVariantDark, fontSize = 13.sp)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ActiveGreen.copy(alpha = 0.15f))
                        .border(1.dp, ActiveGreen.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ActiveGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SETUP COMPLETE", color = ActiveGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // GENERAL
        SectionHeader("GENERAL")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingToggleRow(
                    title = "Auto Start Listening",
                    subtitle = "Listen on mic key press",
                    checked = autoStart,
                    onCheckedChange = { scope.launch { preferences.setAutoStartListening(it) } }
                )
                SettingToggleRow(
                    title = "Keep Keyboard Active",
                    subtitle = "Prevent system from killing the app",
                    checked = keepActive,
                    onCheckedChange = { scope.launch { preferences.setKeepKeyboardActive(it) } }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // INPUT & APPEARANCE
        SectionHeader("INPUT & APPEARANCE")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingClickableRow(
                    title = "Keyboard Mode",
                    value = keyboardMode,
                    onClick = {
                        val nextMode = if (keyboardMode == "Voice Only Mode") "Full Keyboard" else "Voice Only Mode"
                        scope.launch { preferences.setKeyboardMode(nextMode) }
                    }
                )
                SettingClickableRow(
                    title = "Appearance",
                    value = "Edit theme and styling",
                    onClick = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // VOICE ENGINE
        SectionHeader("VOICE ENGINE")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            SettingClickableRow(
                title = "Inference Model",
                value = model,
                onClick = onNavigateToVoiceSettings
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // DATA & PRIVACY
        SectionHeader("DATA & PRIVACY")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingToggleRow(
                    title = "Local Processing",
                    subtitle = "Your voice stays on device",
                    checked = localProcessing,
                    onCheckedChange = { scope.launch { preferences.setLocalProcessing(it) } }
                )
                SettingToggleRow(
                    title = "Analytics",
                    subtitle = "Help improve TakoFlow",
                    checked = analytics,
                    onCheckedChange = { scope.launch { preferences.setAnalytics(it) } }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ABOUT
        SectionHeader("ABOUT")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Version", color = OnSurfaceDark, fontSize = 16.sp)
                    Text("1.0.0 (Build 1)", color = OnSurfaceVariantDark, fontSize = 14.sp)
                }
                SettingClickableRow(
                    title = "Open Source Licenses",
                    value = "",
                    onClick = {}
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
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
            Text(title, color = OnSurfaceDark, fontSize = 16.sp)
            Text(subtitle, color = OnSurfaceVariantDark, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = PrimaryAmber)
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
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, color = OnSurfaceDark, fontSize = 16.sp)
            if (value.isNotEmpty()) {
                Text(value, color = PrimaryAmber, fontSize = 13.sp)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnSurfaceVariantDark)
    }
}
