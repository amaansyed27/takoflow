package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.PrimaryAmberSoft

@Composable
fun WaveformMeter(
    modifier: Modifier = Modifier,
    isListening: Boolean = true,
    rmsLevel: Float = 0.5f,
    barCount: Int = 5,
    maxHeight: Dp = 24.dp
) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val level = rmsLevel.coerceIn(0f, 1f)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            val phase by transition.animateFloat(
                0.25f,
                1f,
                infiniteRepeatable(
                    tween(520 + index * 95, index * 42, FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "bar$index"
            )
            val scale = if (isListening) {
                (0.24f + level * 0.68f) * (0.55f + phase * 0.45f)
            } else 0.18f
            Box(
                Modifier.width(4.dp)
                    .height(maxHeight * scale.coerceIn(0.15f, 1f))
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(PrimaryAmberSoft, PrimaryAmber)))
            )
        }
    }
}
