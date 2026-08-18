package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.theme.SurfaceContainer

private enum class KeyboardFamily(val label: String) {
    SAMSUNG("Samsung"),
    GBOARD("Gboard"),
    OTHER("Other")
}

@Composable
fun KeyboardSwitchGuideScreen(
    onContinue: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val defaultFamily = remember {
        if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
            KeyboardFamily.SAMSUNG
        } else {
            KeyboardFamily.GBOARD
        }
    }
    var family by remember { mutableStateOf(defaultFamily) }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        BrandHeader(
            title = "Switching keyboards",
            subtitle = "Use TakoFlow for voice and your regular keyboard for typing",
            onBack = onBack
        )
        Spacer(Modifier.height(22.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), activeGlow = true) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, null, tint = PrimaryAmber)
                    Spacer(Modifier.padding(6.dp))
                    Text(
                        "Two keyboards, one workflow",
                        color = OnSurfaceDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "Tap Keyboard inside TakoFlow to return to the keyboard you used before. To come back to TakoFlow, use your phone's input-method switcher below.",
                    color = OnSurfaceVariantDark,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "YOUR TYPING KEYBOARD",
            color = OnSurfaceVariantDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp
        )
        Spacer(Modifier.height(9.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeyboardFamily.entries.forEach { option ->
                FamilyTab(
                    label = option.label,
                    selected = family == option,
                    modifier = Modifier.weight(1f),
                    onClick = { family = option }
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        when (family) {
            KeyboardFamily.SAMSUNG -> SamsungGuide()
            KeyboardFamily.GBOARD -> GboardGuide()
            KeyboardFamily.OTHER -> OtherKeyboardGuide()
        }

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                manager.showInputMethodPicker()
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(PrimaryAmber, DarkBackground)
        ) {
            Icon(Icons.Default.SwapHoriz, null)
            Text("Choose keyboard now", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(13.dp)
        ) {
            Icon(Icons.Default.Settings, null)
            Text("Open keyboard settings", modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.height(11.dp))
        OutlinedButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(13.dp)
        ) {
            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SamsungGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GuideNotice(
            title = "The Samsung globe is for languages",
            text = "A normal tap on Samsung Keyboard's globe can cycle Samsung languages instead of changing the keyboard app. Use the system keyboard button for TakoFlow."
        )
        GuideCard(
            number = "1",
            icon = Icons.Default.Settings,
            title = "Enable Samsung's keyboard button",
            text = "Open Settings → General management → Keyboard list and default → turn on Keyboard button on navigation bar."
        )
        GuideCard(
            number = "2",
            icon = Icons.Default.SwapHoriz,
            title = "Switch to TakoFlow",
            text = "Open any text field, tap the keyboard button in the navigation area at the bottom of the screen, then choose TakoFlow Voice."
        )
        GuideCard(
            number = "3",
            icon = Icons.Default.Keyboard,
            title = "Return to Samsung Keyboard",
            text = "Inside TakoFlow, tap Keyboard. TakoFlow asks Android to return to your previously used input method."
        )
    }
}

@Composable
private fun GboardGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GuideCard(
            number = "1",
            icon = Icons.Default.SwapHoriz,
            title = "Switch from Gboard",
            text = "Touch and hold Gboard's globe key, then choose TakoFlow Voice. If the globe is hidden or only changes languages, use Android's keyboard selector in the navigation area instead."
        )
        GuideCard(
            number = "2",
            icon = Icons.Default.Keyboard,
            title = "Return to Gboard",
            text = "Tap Keyboard inside TakoFlow. Android normally returns to Gboard immediately because it was your previous input method."
        )
        GuideCard(
            number = "3",
            icon = Icons.Default.Settings,
            title = "If TakoFlow is missing",
            text = "Open keyboard settings and confirm both Gboard and TakoFlow Voice are enabled under On-screen keyboard."
        )
    }
}

@Composable
private fun OtherKeyboardGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GuideCard(
            number = "1",
            icon = Icons.Default.SwapHoriz,
            title = "Use Android's input-method selector",
            text = "With a text field focused, look for the keyboard/input-method button in the navigation or gesture area. Some Android skins expose the same chooser from a keyboard notification."
        )
        GuideCard(
            number = "2",
            icon = Icons.Default.Mic,
            title = "Choose TakoFlow Voice",
            text = "Select TakoFlow Voice from the system list. The exact icon and placement vary by phone maker, but Android owns this chooser."
        )
        GuideCard(
            number = "3",
            icon = Icons.Default.Keyboard,
            title = "Return to typing",
            text = "Tap Keyboard inside TakoFlow to return to the previously used keyboard."
        )
    }
}

@Composable
private fun GuideNotice(title: String, text: String) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .background(PrimaryAmber.copy(alpha = 0.10f), RoundedCornerShape(13.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(title, color = PrimaryAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                text,
                color = OnSurfaceVariantDark,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun FamilyTab(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                if (selected) PrimaryAmber else SurfaceContainer,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) DarkBackground else OnSurfaceDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
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
            Box(
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
                    Text(
                        title,
                        color = OnSurfaceDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Text(
                    text,
                    color = OnSurfaceVariantDark,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
    }
}
