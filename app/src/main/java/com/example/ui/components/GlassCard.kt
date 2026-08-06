package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && onClick != null) 0.985f else 1f,
        tween(110),
        label = "cardScale"
    )
    val borderColor by animateColorAsState(
        if (activeGlow) PrimaryAmber else PrimaryAmber.copy(alpha = borderAlpha),
        tween(220),
        label = "cardBorder"
    )
    val backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF202127), Color(0xFF14151A)))

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }.clip(shape).background(backgroundBrush)
            .border(if (activeGlow) 1.2.dp else 1.dp, borderColor, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource, indication = null, onClick = onClick)
                } else Modifier
            ),
        content = content
    )
}
