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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
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

            Text(
                text = "Voice Profiles",
                color = OnSurfaceDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Profile",
                    tint = OnSurfaceVariantDark
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Profiles List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileCard(
                id = "default",
                title = "Default",
                subtitle = "General Dictation",
                icon = Icons.Default.Mic,
                isActive = activeProfileId == "default",
                onClick = { scope.launch { preferences.setActiveProfileId("default") } }
            )

            ProfileCard(
                id = "work",
                title = "Work",
                subtitle = "Professional Tone",
                icon = Icons.Default.Work,
                isActive = activeProfileId == "work",
                onClick = { scope.launch { preferences.setActiveProfileId("work") } }
            )

            ProfileCard(
                id = "notes",
                title = "Notes",
                subtitle = "Quick Thoughts",
                icon = Icons.Default.EditNote,
                isActive = activeProfileId == "notes",
                onClick = { scope.launch { preferences.setActiveProfileId("notes") } }
            )

            ProfileCard(
                id = "creative",
                title = "Creative",
                subtitle = "Stories & Ideas",
                icon = Icons.Default.AutoAwesome,
                isActive = activeProfileId == "creative",
                onClick = { scope.launch { preferences.setActiveProfileId("creative") } }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Info Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = OnSurfaceVariantDark,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("About Profiles", color = OnSurfaceDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Profiles help TakoFlow adapt to different contexts and styles. Customize language models, vocabulary, and punctuation per profile.",
                        color = OnSurfaceVariantDark,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    id: String,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isActive) PrimaryAmber.copy(alpha = 0.15f) else SurfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isActive) PrimaryAmber else OnSurfaceVariantDark,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(title, color = OnSurfaceDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = OnSurfaceVariantDark, fontSize = 13.sp)
                }
            }

            if (isActive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryAmber.copy(alpha = 0.1f))
                            .border(1.dp, PrimaryAmber.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Active", color = PrimaryAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = OnSurfaceVariantDark)
                }
            }
        }
    }
}
