package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SurfaceContainerLow

@Composable
fun BottomNavBar(currentRoute: String, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val navItems = listOf(
        NavItem("dashboard", "Home", Icons.Default.Home),
        NavItem("voice_profiles", "Profiles", Icons.Default.Person),
        NavItem("general_settings", "Settings", Icons.Default.Settings)
    )
    Row(
        modifier.fillMaxWidth().background(SurfaceContainerLow).navigationBarsPadding()
            .height(68.dp).padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEach { item ->
            val active = currentRoute == item.route
            val color by animateColorAsState(
                if (active) PrimaryAmber else OnSurfaceVariantDark,
                tween(180),
                label = "navColor"
            )
            val padding by animateDpAsState(if (active) 18.dp else 12.dp, tween(180), label = "navPadding")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(if (active) PrimaryAmber.copy(alpha = 0.11f) else Color.Transparent)
                    .clickable { onNavigate(item.route) }
                    .padding(horizontal = padding, vertical = 7.dp)
            ) {
                Icon(item.icon, item.label, tint = color)
                Text(item.label, color = color, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)
