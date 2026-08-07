package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import com.example.core.TakoFlowAppContainer
import com.example.ui.components.BrandHeader
import com.example.ui.components.GlassCard
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.viewmodel.SetupViewModel
import com.example.ui.viewmodel.takoFlowViewModel
import java.util.Locale

@Composable
fun EnableKeyboardStepperScreen(
    container: TakoFlowAppContainer,
    onBack: () -> Unit,
    onCompleteSetup: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SetupViewModel = takoFlowViewModel {
        SetupViewModel(
            settings = container.settings,
            models = container.models,
            systemStatus = container.systemStatus
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refresh() }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        BrandHeader("Set up TakoFlow", "Complete every required check", onBack = onBack)
        Spacer(Modifier.height(22.dp))

        SetupCheck(
            title = "Enable the voice keyboard",
            subtitle = "Allow TakoFlow in Android keyboard settings",
            complete = uiState.imeEnabled,
            action = if (uiState.imeEnabled) "Enabled" else "Open settings",
            icon = Icons.Default.Keyboard
        ) { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        Spacer(Modifier.height(11.dp))
        SetupCheck(
            title = "Select TakoFlow",
            subtitle = "Choose TakoFlow as the current input method",
            complete = uiState.imeSelected,
            action = if (uiState.imeSelected) "Selected" else "Choose keyboard",
            icon = Icons.Default.CheckCircle
        ) {
            context.getSystemService(InputMethodManager::class.java).showInputMethodPicker()
        }
        Spacer(Modifier.height(11.dp))
        SetupCheck(
            title = "Allow microphone",
            subtitle = "Required only while you are dictating",
            complete = uiState.microphoneGranted,
            action = if (uiState.microphoneGranted) "Allowed" else "Allow",
            icon = Icons.Default.Mic
        ) { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        Spacer(Modifier.height(11.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), activeGlow = uiState.vosk.installed) {
            Column(modifier = Modifier.padding(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (uiState.vosk.installed) Icons.Default.CheckCircle else Icons.Default.Download,
                        null,
                        tint = if (uiState.vosk.installed) ActiveGreen else PrimaryAmber
                    )
                    Spacer(Modifier.padding(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Install Vosk", color = OnSurfaceDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Default offline streaming model · about 40 MB", color = OnSurfaceVariantDark, fontSize = 12.sp)
                    }
                }
                if (uiState.vosk.downloading) {
                    Spacer(Modifier.height(13.dp))
                    LinearProgressIndicator(
                        progress = { uiState.vosk.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${uiState.vosk.progressPercent}% · ${formatSetupBytes(uiState.vosk.downloadedBytes)}",
                        color = OnSurfaceVariantDark,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
                uiState.vosk.error?.let {
                    Spacer(Modifier.height(9.dp))
                    Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                if (!uiState.vosk.installed) {
                    Spacer(Modifier.height(13.dp))
                    if (uiState.vosk.downloading) {
                        OutlinedButton(
                            onClick = viewModel::cancelVoskDownload,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.StopCircle, null)
                            Text("Cancel download", modifier = Modifier.padding(start = 7.dp))
                        }
                    } else {
                        Button(
                            onClick = viewModel::downloadVosk,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(PrimaryAmber, DarkBackground)
                        ) {
                            Text(
                                if (uiState.vosk.error == null) "Download Vosk" else "Retry download",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                viewModel.selectDefaultModel()
                onCompleteSetup()
            },
            enabled = uiState.ready,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(PrimaryAmber, DarkBackground)
        ) {
            Text("Continue", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        if (!uiState.ready) {
            Text(
                "Continue becomes available after all four checks pass.",
                color = OnSurfaceVariantDark,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 10.dp).align(Alignment.CenterHorizontally)
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SetupCheck(
    title: String,
    subtitle: String,
    complete: Boolean,
    action: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), activeGlow = complete, onClick = onClick) {
        Row(modifier = Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (complete) ActiveGreen else PrimaryAmber)
            Spacer(Modifier.padding(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = OnSurfaceDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = OnSurfaceVariantDark, fontSize = 12.sp)
            }
            Text(
                action,
                color = if (complete) ActiveGreen else PrimaryAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatSetupBytes(bytes: Long): String =
    if (bytes <= 0) "starting…" else String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
