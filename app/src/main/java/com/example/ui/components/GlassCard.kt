package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PrimaryAmber

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderAlpha: Float = 0.15f,
    activeGlow: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1F1F1F),
            Color(0xFF161616)
        )
    )

    val borderColor = if (activeGlow) PrimaryAmber else PrimaryAmber.copy(alpha = borderAlpha)

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundBrush)
            .border(1.dp, borderColor, shape)
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        content = content
    )
}
