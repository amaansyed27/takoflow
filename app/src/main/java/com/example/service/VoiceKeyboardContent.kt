package com.example.service

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speech.SpeechState
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SurfaceContainerHigh

@Composable
fun VoiceOnlyModeContent(
    speechState: SpeechState,
    rmsDb: Float,
    onMicClick: () -> Unit,
    onSpace: () -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(204.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size((76 + (rmsDb * 12)).dp)
                        .clip(CircleShape)
                        .background(
                            if (speechState is SpeechState.Listening) PrimaryAmber
                            else SurfaceContainerHigh
                        )
                        .clickable(onClick = onMicClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Dictate",
                        tint = if (speechState is SpeechState.Listening) DarkBackground else PrimaryAmber,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (speechState is SpeechState.Listening) "Tap to stop" else "Tap to dictate",
                    color = OnSurfaceVariantDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UtilityKey(modifier = Modifier.width(58.dp), onClick = onDelete) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = OnSurfaceDark
                )
            }
            UtilityKey(modifier = Modifier.weight(1f), onClick = onSpace) {
                Text("Space", color = OnSurfaceVariantDark, fontSize = 14.sp)
            }
            Box(
                modifier = Modifier
                    .width(58.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(PrimaryAmber)
                    .clickable(onClick = onEnter),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardReturn,
                    contentDescription = "Return",
                    tint = DarkBackground
                )
            }
        }
    }
}

@Composable
private fun UtilityKey(
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(SurfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
