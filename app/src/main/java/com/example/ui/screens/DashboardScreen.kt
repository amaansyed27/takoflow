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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.example.ui.components.BrandHeader
import com.example.ui.components.GlassCard
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.takoFlowViewModel

@Composable
fun DashboardScreen(
    container: TakoFlowAppContainer,
    onNavigateToEnable: () -> Unit,
    onNavigateToVoiceSettings: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToGeneralSettings: () -> Unit,
    onNavigateToSwitchingGuide: () -> Unit,
    onNavigateToDictation: () -> Unit
) {
    val viewModel: DashboardViewModel = takoFlowViewModel {
        DashboardViewModel(
            settings = container.settings,
            models = container.models,
            profiles = container.profiles,
            systemStatus = container.systemStatus
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardContent(
        uiState = uiState,
        onNavigateToEnable = onNavigateToEnable,
        onNavigateToVoiceSettings = onNavigateToVoiceSettings,
        onNavigateToProfiles = onNavigateToProfiles,
        onNavigateToGeneralSettings = onNavigateToGeneralSettings,
        onNavigateToSwitchingGuide = onNavigateToSwitchingGuide,
        onNavigateToDictation = onNavigateToDictation
    )
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onNavigateToEnable: () -> Unit,
    onNavigateToVoiceSettings: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToGeneralSettings: () -> Unit,
    onNavigateToSwitchingGuide: () -> Unit,
    onNavigateToDictation: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        BrandHeader("TakoFlow", "Voice typing is ready when every check is green")
        Spacer(Modifier.height(22.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), activeGlow = uiState.ready) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (uiState.ready) Icons.Default.CheckCircle else Icons.Default.Mic,
                        null,
                        tint = if (uiState.ready) ActiveGreen else PrimaryAmber,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.size(13.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (uiState.ready) "Ready to dictate" else "TakoFlow needs attention",
                            color = OnSurfaceDark,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${uiState.model} · ${uiState.profileName}",
                            color = OnSurfaceVariantDark,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                StatusLine("Keyboard enabled", uiState.imeEnabled)
                StatusLine("TakoFlow selected", uiState.imeSelected)
                StatusLine("Microphone allowed", uiState.microphoneGranted)
                StatusLine("Selected model installed", uiState.selectedModelReady)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = if (uiState.ready) onNavigateToDictation else onNavigateToEnable,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(PrimaryAmber, DarkBackground)
                ) {
                    Text(if (uiState.ready) "Test dictation" else "Finish setup", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "QUICK ACTIONS",
            color = OnSurfaceVariantDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
        )
        Spacer(Modifier.height(9.dp))
        ActionCard(Icons.Default.Mic, "Voice engine", uiState.model, onNavigateToVoiceSettings)
        Spacer(Modifier.height(10.dp))
        ActionCard(Icons.Default.Style, "Formatting profiles", uiState.profileName, onNavigateToProfiles)
        Spacer(Modifier.height(10.dp))
        ActionCard(
            Icons.Default.SwapHoriz,
            "Switching keyboards",
            "TakoFlow ↔ normal keyboard",
            onNavigateToSwitchingGuide
        )
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
