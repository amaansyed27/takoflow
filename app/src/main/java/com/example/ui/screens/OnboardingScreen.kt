package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BrandMark
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NavyGlow
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(onFinishOnboarding: () -> Unit) {
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(120)
        contentVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyGlow, DarkBackground, DarkBackground)))
            .padding(horizontal = 22.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(22.dp))
        BrandMark(size = 158.dp, animated = true)
        Text(
            "TAKOFLOW",
            color = PrimaryAmber,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Speak anywhere. Keep your keyboard.",
            color = OnSurfaceDark,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "TakoFlow is a private voice panel that works beside Samsung Keyboard, Gboard or any other Android keyboard.",
            color = OnSurfaceVariantDark,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(26.dp))
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(300)) + slideInVertically(tween(360)) { it / 4 }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureRow(Icons.Default.Mic, "Local dictation", "Fast Vosk streaming or optional Whisper Tiny")
                FeatureRow(Icons.Default.Keyboard, "Keep normal typing", "Switch back to your previous keyboard in one tap")
                FeatureRow(Icons.Default.Lock, "Private by design", "Audio is processed locally and disabled in password fields")
            }
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onFinishOnboarding,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryAmber,
                contentColor = DarkBackground
            )
        ) {
            Text("Set up TakoFlow", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = PrimaryAmber, modifier = Modifier.size(25.dp))
            Spacer(Modifier.size(14.dp))
            Column {
                Text(title, color = OnSurfaceDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = OnSurfaceVariantDark, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}
