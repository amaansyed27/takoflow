package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SurfaceContainerHigh

@Composable
fun KeyboardSwitchGuideScreen(
    onContinue: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
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
            }

            Column {
                Text(
                    "Switching keyboards",
                    color = OnSurfaceDark,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "TakoFlow handles voice. Your normal keyboard handles typing.",
                    color = OnSurfaceVariantDark,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), activeGlow = true) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(PrimaryAmber.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = PrimaryAmber,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "TakoFlow is voice-first",
                        color = OnSurfaceDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Dictate, delete, add a space or press enter without replacing Samsung Keyboard or Gboard.",
                        color = OnSurfaceVariantDark,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        GuideStep(
            number = "1",
            title = "Open TakoFlow",
            description = "Tap Android’s keyboard or globe button and choose TakoFlow whenever you want to dictate."
        )
        Spacer(Modifier.height(12.dp))
        GuideStep(
            number = "2",
            title = "Switch back for typing",
            description = "Tap the “Use keyboard” button inside TakoFlow. It returns to your last-used keyboard."
        )
        Spacer(Modifier.height(12.dp))
        GuideStep(
            number = "3",
            title = "Keep the switch button available",
            description = "On Samsung phones, enable “Show input method button on navigation bar” in keyboard settings for quick access."
        )

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Keyboard, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Open keyboard settings")
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryAmber,
                contentColor = DarkBackground
            )
        ) {
            Text("Continue", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GuideStep(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainerHigh)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PrimaryAmber.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                color = PrimaryAmber,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                color = OnSurfaceDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                description,
                color = OnSurfaceVariantDark,
                fontSize = 13.sp
            )
        }
    }
}
