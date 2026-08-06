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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BrandMark
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber

@Composable
fun SetupScreen(onNavigateToEnable: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
            .padding(horizontal = 22.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandMark(size = 98.dp, animated = true)
        Text("Set up TakoFlow", color = OnSurfaceDark, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Four real checks prepare the voice keyboard for daily use.",
            color = OnSurfaceVariantDark,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(26.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), activeGlow = true) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SetupRow(Icons.Default.Keyboard, "Enable TakoFlow", "Allow the Android input method")
                SetupRow(Icons.Default.CheckCircle, "Select TakoFlow", "Make the voice panel current")
                SetupRow(Icons.Default.Mic, "Allow microphone", "Only used while dictating")
                SetupRow(Icons.Default.Storage, "Install Vosk", "Download the default offline model")
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            "You can switch back to Samsung Keyboard or Gboard from inside TakoFlow.",
            color = OnSurfaceVariantDark,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onNavigateToEnable,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(PrimaryAmber, DarkBackground)
        ) {
            Text("Start checks", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SetupRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = PrimaryAmber, modifier = Modifier.size(25.dp))
        Spacer(Modifier.size(14.dp))
        Column {
            Text(title, color = OnSurfaceDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = OnSurfaceVariantDark, fontSize = 12.sp)
        }
    }
}
