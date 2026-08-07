package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.core.TakoFlowAppContainer
import com.example.ui.components.BrandHeader
import com.example.ui.components.GlassCard
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.viewmodel.GeneralSettingsViewModel
import com.example.ui.viewmodel.takoFlowViewModel

@Composable
fun GeneralSettingsScreen(
    container: TakoFlowAppContainer,
    onBack: () -> Unit,
    onNavigateToVoiceSettings: () -> Unit,
    onNavigateToSwitchingGuide: () -> Unit,
    onRunSetupAgain: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: GeneralSettingsViewModel = takoFlowViewModel {
        GeneralSettingsViewModel(
            settings = container.settings,
            systemStatus = container.systemStatus
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        BrandHeader("Settings", "System, voice and feedback", onBack = onBack)
        Spacer(Modifier.height(22.dp))

        SectionLabel("SYSTEM")
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            activeGlow = uiState.imeEnabled && uiState.imeSelected
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingStatus("TakoFlow enabled", uiState.imeEnabled)
                SettingStatus("TakoFlow selected", uiState.imeSelected)
                Spacer(Modifier.height(8.dp))
                SettingAction("Open Android keyboard settings", "Manage enabled keyboards") {
                    context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("VOICE PANEL")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                ToggleSetting(
                    "Start listening when opened",
                    "Begin dictation as soon as TakoFlow appears",
                    uiState.autoStart,
                    viewModel::setAutoStart
                )
                ToggleSetting(
                    "Sound feedback",
                    "Play a short start and stop tone",
                    uiState.sound,
                    viewModel::setSound
                )
                ToggleSetting(
                    "Vibration feedback",
                    "Use a short haptic pulse",
                    uiState.vibration,
                    viewModel::setVibration
                )
                SettingAction("Voice engine", uiState.model, onNavigateToVoiceSettings)
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("HELP")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingAction(
                    "Switching keyboards",
                    "Learn the TakoFlow ↔ keyboard flow",
                    onNavigateToSwitchingGuide,
                    Icons.Default.SwapHoriz
                )
                SettingAction(
                    "Run setup again",
                    "Recheck permissions, keyboard and model",
                    onRunSetupAgain,
                    Icons.Default.Refresh
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("ABOUT")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Version", color = OnSurfaceDark)
                    Text(BuildConfig.VERSION_NAME, color = PrimaryAmber)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Speech is processed locally. Network access is used only for model downloads you start.",
                    color = OnSurfaceVariantDark,
                    fontSize = 12.sp
                )
            }
        }
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
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(start = 3.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingStatus(label: String, ready: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = OnSurfaceDark, fontSize = 14.sp)
        Text(
            if (ready) "READY" else "REQUIRED",
            color = if (ready) ActiveGreen else PrimaryAmber,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ToggleSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = OnSurfaceDark, fontSize = 15.sp)
            Text(subtitle, color = OnSurfaceVariantDark, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingAction(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = PrimaryAmber)
            Spacer(Modifier.padding(5.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = OnSurfaceDark, fontSize = 15.sp)
            Text(subtitle, color = OnSurfaceVariantDark, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = OnSurfaceVariantDark)
    }
}
