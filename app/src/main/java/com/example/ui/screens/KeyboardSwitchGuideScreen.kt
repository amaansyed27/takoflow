package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BrandHeader
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber

@Composable
fun KeyboardSwitchGuideScreen(
    onContinue: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        BrandHeader(
            title = "Switching keyboards",
            subtitle = "TakoFlow handles voice; your normal keyboard handles typing",
            onBack = onBack
        )
        Spacer(Modifier.height(22.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), activeGlow = true) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, null, tint = PrimaryAmber)
                    Spacer(Modifier.padding(6.dp))
                    Text("Open TakoFlow for dictation", color = OnSurfaceDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Use Android’s keyboard or globe button and choose TakoFlow whenever you want local voice typing.",
                    color = OnSurfaceVariantDark,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        GuideCard(
            number = "1",
            icon = Icons.Default.SwapHoriz,
            title = "Return to normal typing",
            text = "Tap Keyboard in the TakoFlow panel. Android returns to Samsung Keyboard, Gboard or the keyboard you used previously."
        )
        Spacer(Modifier.height(12.dp))
        GuideCard(
            number = "2",
            icon = Icons.Default.Keyboard,
            title = "Come back to TakoFlow",
            text = "Tap the keyboard-switch button in Android’s navigation area and select TakoFlow again."
        )
        Spacer(Modifier.height(12.dp))
        GuideCard(
            number = "3",
            icon = Icons.Default.Mic,
            title = "Use it anywhere",
            text = "TakoFlow commits recognized text into the active field, except password fields where voice capture is disabled."
        )

        Spacer(Modifier.height(22.dp))
        OutlinedButton(
            onClick = { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(13.dp)
        ) {
            Icon(Icons.Default.Keyboard, null)
            Text("Open keyboard settings", modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.height(11.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(PrimaryAmber, DarkBackground)
        ) {
            Text("Continue", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GuideCard(
    number: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    text: String
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.background(PrimaryAmber.copy(alpha = 0.15f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(number, color = PrimaryAmber, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.padding(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = PrimaryAmber)
                    Text(title, color = OnSurfaceDark, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
                Text(text, color = OnSurfaceVariantDark, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}
