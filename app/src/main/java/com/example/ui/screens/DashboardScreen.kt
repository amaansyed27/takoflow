package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BottomNavBar
import com.example.ui.components.GlassCard
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SurfaceContainerHigh

@Composable
fun DashboardScreen(
    onNavigateToEnable: () -> Unit,
    onNavigateToVoiceSettings: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToGeneralSettings: () -> Unit,
    onNavigateToDictation: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentRoute = "dashboard",
                onNavigate = { route ->
                    if (route == "voice_profiles") onNavigateToProfiles()
                    else if (route == "general_settings") onNavigateToGeneralSettings()
                    else onNavigate(route)
                }
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(32.dp))

                Text(
                    text = "TAKOFLOW",
                    color = PrimaryAmber,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )

                IconButton(onClick = onNavigateToGeneralSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = OnSurfaceDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Keyboard Status Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                activeGlow = true
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "KEYBOARD STATUS",
                            color = OnSurfaceVariantDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ACTIVE",
                                color = PrimaryAmber,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(ActiveGreen)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "TakoFlow Keyboard is ready to use in any app.",
                            color = OnSurfaceDark,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Pulse Mic Button
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .scale(pulseScale)
                            .border(1.dp, PrimaryAmber.copy(alpha = 0.4f), CircleShape)
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh)
                            .clickable { onNavigateToDictation() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Status Mic",
                            tint = PrimaryAmber,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "QUICK ACTIONS",
                color = OnSurfaceVariantDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            // Quick Actions 2x2 Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNavigateToEnable
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Keyboard, contentDescription = null, tint = PrimaryAmber)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Enable Keyboard", color = OnSurfaceDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Current: TakoFlow", color = OnSurfaceVariantDark, fontSize = 12.sp)
                        }
                    }
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNavigateToVoiceSettings
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.MicNone, contentDescription = null, tint = PrimaryAmber)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Voice Settings", color = OnSurfaceDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Language, Model, etc.", color = OnSurfaceVariantDark, fontSize = 12.sp)
                        }
                    }
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNavigateToProfiles
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = PrimaryAmber)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Voice Profiles", color = OnSurfaceDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Manage your styles", color = OnSurfaceVariantDark, fontSize = 12.sp)
                        }
                    }
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNavigateToEnable
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Extension, contentDescription = null, tint = PrimaryAmber)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("How to Use", color = OnSurfaceDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Setup & guide", color = OnSurfaceVariantDark, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Action Button: "Start Voice Typing"
            Button(
                onClick = onNavigateToDictation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryAmber,
                    contentColor = DarkBackground
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Start Voice Typing", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Tap to test your microphone", fontSize = 12.sp, color = DarkBackground.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}
