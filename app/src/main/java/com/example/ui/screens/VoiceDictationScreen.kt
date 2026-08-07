package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.TakoFlowAppContainer
import com.example.service.VoiceKeyboardPanel
import com.example.speech.SpeechState
import com.example.ui.components.BrandHeader
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.viewmodel.DictationViewModel
import com.example.ui.viewmodel.takoFlowViewModel

@Composable
fun VoiceDictationScreen(
    container: TakoFlowAppContainer,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: DictationViewModel = takoFlowViewModel { DictationViewModel(container) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        BrandHeader(
            title = "Test dictation",
            subtitle = "${uiState.model} · ${uiState.profileName}",
            onBack = onBack,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
        )

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp)
        ) {
            OutlinedTextField(
                value = uiState.text,
                onValueChange = viewModel::updateText,
                placeholder = { Text("Your transcription appears here…") },
                modifier = Modifier.fillMaxWidth().height(210.dp),
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceContainerLow,
                    unfocusedContainerColor = SurfaceContainerLow,
                    focusedBorderColor = PrimaryAmber,
                    unfocusedBorderColor = PrimaryAmber.copy(alpha = 0.22f),
                    focusedTextColor = OnSurfaceDark,
                    unfocusedTextColor = OnSurfaceDark
                )
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DictationAction(
                    Icons.Default.ContentCopy,
                    "Copy",
                    uiState.text.isNotBlank(),
                    Modifier.weight(1f)
                ) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("TakoFlow dictation", uiState.text))
                }
                DictationAction(
                    Icons.Default.Share,
                    "Share",
                    uiState.text.isNotBlank(),
                    Modifier.weight(1f)
                ) {
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, uiState.text)
                            },
                            "Share dictation"
                        )
                    )
                }
                DictationAction(
                    Icons.Default.Clear,
                    "Clear",
                    uiState.text.isNotBlank(),
                    Modifier.weight(1f),
                    viewModel::clear
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                when (val state = uiState.speechState) {
                    is SpeechState.Error -> state.message
                    is SpeechState.Preparing -> state.message
                    is SpeechState.Processing -> state.partialText
                    is SpeechState.Listening -> state.partialText.ifBlank { "Listening…" }
                    else -> "Use this screen to verify the same engine and profile used by the IME."
                },
                color = if (uiState.speechState is SpeechState.Error) {
                    androidx.compose.material3.MaterialTheme.colorScheme.error
                } else {
                    OnSurfaceVariantDark
                },
                fontSize = 12.sp
            )
        }

        VoiceKeyboardPanel(
            state = uiState.speechState,
            rmsDb = uiState.rmsDb,
            modelName = uiState.model,
            profileName = uiState.profileName,
            sensitiveField = false,
            onMicClick = viewModel::onMicClick,
            onSwitchKeyboard = {
                val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                manager.showInputMethodPicker()
            },
            onSpace = viewModel::addSpace,
            onDelete = viewModel::deleteCharacter,
            onEnter = viewModel::addLineBreak
        )
    }
}

@Composable
private fun DictationAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val tint = if (enabled) PrimaryAmber else OnSurfaceVariantDark.copy(alpha = 0.45f)
    Box(
        modifier = modifier.height(46.dp).background(
            SurfaceContainerLow,
            RoundedCornerShape(12.dp)
        ).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
