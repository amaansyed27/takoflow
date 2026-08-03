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
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TakoFlowPreferences
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SurfaceContainerHigh
import kotlinx.coroutines.launch

@Composable
fun VoiceProfilesScreen(
    preferences: TakoFlowPreferences,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val activeProfileId by preferences.activeProfileId.collectAsState(initial = "default")

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
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryAmber
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Formatting profiles", color = OnSurfaceDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Choose how recognized text is formatted", color = OnSurfaceVariantDark, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileCard(
                title = "Default",
                subtitle = "Normal sentences with your punctuation settings",
                icon = Icons.Default.Mic,
                isActive = activeProfileId == "default",
                onClick = { scope.launch { preferences.setActiveProfileId("default") } }
            )
            ProfileCard(
                title = "Work",
                subtitle = "Expands casual phrases such as “gonna” and “wanna”",
                icon = Icons.Default.Work,
                isActive = activeProfileId == "work",
                onClick = { scope.launch { preferences.setActiveProfileId("work") } }
            )
            ProfileCard(
                title = "Notes",
                subtitle = "Prefixes every completed dictation with a bullet",
                icon = Icons.Default.EditNote,
                isActive = activeProfileId == "notes",
                onClick = { scope.launch { preferences.setActiveProfileId("notes") } }
            )
        }

        Spacer(Modifier.height(24.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryAmber)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Profiles only format the final text. They do not upload audio or retrain either speech model.",
                    color = OnSurfaceVariantDark,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        activeGlow = isActive,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isActive) PrimaryAmber.copy(alpha = 0.15f) else SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isActive) PrimaryAmber else OnSurfaceVariantDark
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = OnSurfaceDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = OnSurfaceVariantDark, fontSize = 12.sp)
            }
            if (isActive) {
                Text("ACTIVE", color = PrimaryAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
