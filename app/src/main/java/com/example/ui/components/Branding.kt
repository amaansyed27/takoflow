package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NavyGlow
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber

@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    animated: Boolean = false,
    contentDescription: String? = "TakoFlow"
) {
    val transition = rememberInfiniteTransition(label = "brandMark")
    val scale by transition.animateFloat(
        initialValue = if (animated) 0.98f else 1f,
        targetValue = if (animated) 1.035f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brandScale"
    )
    val offset by transition.animateFloat(
        initialValue = if (animated) -2f else 0f,
        targetValue = if (animated) 2f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brandOffset"
    )

    Image(
        painter = painterResource(R.drawable.takoflow_logo),
        contentDescription = contentDescription,
        modifier = modifier.size(size).graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationY = offset
        }
    )
}

@Composable
fun BrandHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PrimaryAmber)
            }
            Spacer(Modifier.width(8.dp))
        }
        BrandMark(size = 38.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = OnSurfaceDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = OnSurfaceVariantDark, fontSize = 12.sp)
            }
        }
        trailing()
    }
}

@Composable
fun BrandLaunchOverlay(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.96f),
        exit = fadeOut(tween(260)) + scaleOut(targetScale = 1.04f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(listOf(NavyGlow, DarkBackground), radius = 920f)
            ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BrandMark(size = 156.dp, animated = true)
                Text(
                    "TAKOFLOW",
                    color = PrimaryAmber,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    "Local voice typing",
                    color = OnSurfaceVariantDark,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
