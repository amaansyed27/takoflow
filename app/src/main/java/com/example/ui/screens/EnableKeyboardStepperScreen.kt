package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.service.ImeStatus
import com.example.speech.SpeechModelManager
import com.example.ui.components.GlassCard
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber

@Composable
fun EnableKeyboardStepperScreen(
    onBack: () -> Unit,
    onCompleteSetup: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val modelManager = remember { SpeechModelManager.get(context) }
    val voskState by modelManager.voskState.collectAsState()

    var imeEnabled by remember { mutableStateOf(false) }
    var imeSelected by remember { mutableStateOf(false) }
    var microphoneGranted by remember { mutableStateOf(false) }

    fun refresh() {
        imeEnabled = ImeStatus.isEnabled(context)
        imeSelected = ImeStatus.isSelected(context)
        microphoneGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        modelManager.refresh()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refresh()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        refresh()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val setupReady = imeEnabled && imeSelected && microphoneGranted && voskState.installed

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
            Column {
                Text("Set up TakoFlow", color = OnSurfaceDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Complete each required step", color = OnSurfaceVariantDark, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        SetupStep(
            number = "1",
            title = "Enable the keyboard",
            subtitle = "Allow TakoFlow in Android keyboard settings",
            complete = imeEnabled,
            actionLabel = if (imeEnabled) "Enabled" else "Open settings",
            onClick = {
                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        )

        Spacer(Modifier.height(12.dp))

        SetupStep(
            number = "2",
            title = "Select TakoFlow",
            subtitle = "Choose TakoFlow as the current input method",
            complete = imeSelected,
            actionLabel = if (imeSelected) "Selected" else "Choose keyboard",
            onClick = {
                val manager = context.getSystemService(InputMethodManager::class.java)
                manager.showInputMethodPicker()
            }
        )

        Spacer(Modifier.height(12.dp))

        SetupStep(
            number = "3",
            title = "Allow microphone",
            subtitle = "Required only while you are dictating",
            complete = microphoneGranted,
            actionLabel = if (microphoneGranted) "Allowed" else "Allow microphone",
            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        )

        Spacer(Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), activeGlow = voskState.installed) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepBadge("4", voskState.installed)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Download Vosk", color = OnSurfaceDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Default offline model · about 40 MB", color = OnSurfaceVariantDark, fontSize = 12.sp)
                    }
                    Icon(
                        if (voskState.installed) Icons.Default.Check else Icons.Default.Download,
                        contentDescription = null,
                        tint = if (voskState.installed) ActiveGreen else PrimaryAmber
                    )
                }

                if (voskState.downloading) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { voskState.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Downloading ${voskState.progressPercent}%", color = OnSurfaceVariantDark, fontSize = 12.sp)
                }

                voskState.error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                if (!voskState.installed) {
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = modelManager::downloadVosk,
                        enabled = !voskState.downloading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryAmber,
                            contentColor = DarkBackground
                        )
                    ) {
                        Text(if (voskState.downloading) "Downloading" else "Download Vosk")
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onCompleteSetup,
            enabled = setupReady,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryAmber,
                contentColor = DarkBackground
            )
        ) {
            Icon(Icons.Default.Keyboard, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Finish setup", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        if (!setupReady) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Finish setup becomes available after all four checks pass.",
                color = OnSurfaceVariantDark,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SetupStep(
    number: String,
    title: String,
    subtitle: String,
    complete: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), activeGlow = complete, onClick = onClick) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepBadge(number, complete)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = OnSurfaceDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = OnSurfaceVariantDark, fontSize = 12.sp)
            }
            Text(
                actionLabel,
                color = if (complete) ActiveGreen else PrimaryAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StepBadge(number: String, complete: Boolean) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (complete) ActiveGreen.copy(alpha = 0.18f) else PrimaryAmber.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        if (complete) {
            Icon(Icons.Default.Check, contentDescription = null, tint = ActiveGreen)
        } else {
            Text(number, color = PrimaryAmber, fontWeight = FontWeight.Bold)
        }
    }
}
