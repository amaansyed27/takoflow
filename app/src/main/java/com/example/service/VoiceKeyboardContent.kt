package com.example.service

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speech.SpeechModels
import com.example.speech.SpeechState
import com.example.ui.components.BrandMark
import com.example.ui.components.WaveformMeter
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun VoiceKeyboardPanel(
    state: SpeechState,
    rmsDb: Float,
    modelName: String,
    profileName: String,
    sensitiveField: Boolean,
    onMicClick: () -> Unit,
    onSwitchKeyboard: () -> Unit,
    onSpace: () -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit
) {
    val isRecording = state is SpeechState.Listening ||
        (modelName == SpeechModels.VOSK && state is SpeechState.Processing)
    val isTranscribing = modelName == SpeechModels.WHISPER_TINY && state is SpeechState.Processing
    val partial = if (modelName == SpeechModels.VOSK && state is SpeechState.Processing) {
        state.partialText
    } else {
        ""
    }
    var elapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isRecording) {
        if (!isRecording) {
            elapsedMs = 0L
            return@LaunchedEffect
        }
        val started = android.os.SystemClock.elapsedRealtime()
        while (true) {
            elapsedMs = android.os.SystemClock.elapsedRealtime() - started
            delay(100)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BrandMark(size = 32.dp, animated = isRecording)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("TakoFlow", color = OnSurfaceDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "$modelName · $profileName",
                    color = OnSurfaceVariantDark,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerHigh)
                    .clickable(onClick = onSwitchKeyboard)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Keyboard, null, tint = PrimaryAmber, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Keyboard", color = OnSurfaceDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(6.dp))
        val visualState = when {
            sensitiveField -> "sensitive"
            isRecording -> "recording"
            isTranscribing -> "processing"
            state is SpeechState.Error -> "error"
            else -> "idle"
        }

        AnimatedContent(
            targetState = visualState,
            transitionSpec = {
                (fadeIn(tween(170)) + scaleIn(initialScale = 0.96f)) togetherWith
                    (fadeOut(tween(110)) + scaleOut(targetScale = 1.02f))
            },
            label = "voiceState"
        ) { target ->
            Box(
                modifier = Modifier.fillMaxWidth().height(154.dp),
                contentAlignment = Alignment.Center
            ) {
                when (target) {
                    "sensitive" -> SensitiveState()
                    "recording" -> RecordingState(partial, rmsDb, elapsedMs, onMicClick)
                    "processing" -> ProcessingState(
                        (state as? SpeechState.Processing)?.partialText ?: "Transcribing on device…"
                    )
                    "error" -> ErrorState(
                        (state as? SpeechState.Error)?.message ?: "Voice typing failed.",
                        onMicClick
                    )
                    else -> IdleState(onMicClick)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UtilityKey(Modifier.width(58.dp), false, onDelete) {
                Icon(Icons.AutoMirrored.Filled.Backspace, "Backspace", tint = OnSurfaceDark)
            }
            UtilityKey(Modifier.weight(1f), false, onSpace) {
                Text("Space", color = OnSurfaceVariantDark, fontSize = 14.sp)
            }
            UtilityKey(Modifier.width(58.dp), true, onEnter) {
                Icon(Icons.AutoMirrored.Filled.KeyboardReturn, "Enter", tint = DarkBackground)
            }
        }
    }
}

@Composable
private fun IdleState(onStart: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedMicButton(false, 0f, onStart)
        Spacer(Modifier.height(8.dp))
        Text("Tap to dictate", color = OnSurfaceDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text("Audio stays on this device", color = OnSurfaceVariantDark, fontSize = 10.sp)
    }
}

@Composable
private fun RecordingState(partial: String, rmsDb: Float, elapsedMs: Long, onStop: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedMicButton(true, rmsDb, onStop)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.width(170.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WaveformMeter(
                        isListening = true,
                        rmsLevel = rmsDb,
                        barCount = 7,
                        maxHeight = 28.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(formatElapsed(elapsedMs), color = PrimaryAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    partial.ifBlank { "Listening…" },
                    color = if (partial.isBlank()) OnSurfaceVariantDark else OnSurfaceDark,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text("Tap the microphone to finish", color = OnSurfaceVariantDark, fontSize = 10.sp)
    }
}

@Composable
private fun ProcessingState(message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(66.dp).clip(CircleShape).background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(Modifier.size(34.dp), color = PrimaryAmber, strokeWidth = 3.dp)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            message,
            color = OnSurfaceDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(62.dp).clip(CircleShape)
                .background(ErrorRed.copy(alpha = 0.14f)).clickable(onClick = onRetry),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Refresh, "Retry", tint = ErrorRed, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(message, color = ErrorRed, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("Tap to retry", color = OnSurfaceVariantDark, fontSize = 10.sp)
    }
}

@Composable
private fun SensitiveState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, null, tint = PrimaryAmber, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(9.dp))
        Text("Voice typing is disabled here", color = OnSurfaceDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text("Password fields stay private", color = OnSurfaceVariantDark, fontSize = 10.sp)
    }
}

@Composable
private fun AnimatedMicButton(listening: Boolean, rmsDb: Float, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "micPulse")
    val ringScale by transition.animateFloat(
        1f,
        if (listening) 1.34f else 1.08f,
        infiniteRepeatable(tween(if (listening) 900 else 1600, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ringScale"
    )
    val ringAlpha by transition.animateFloat(
        if (listening) 0.28f else 0.12f,
        0f,
        infiniteRepeatable(tween(if (listening) 900 else 1600), RepeatMode.Restart),
        label = "ringAlpha"
    )
    val scale by animateFloatAsState(
        if (listening) 1f + rmsDb.coerceIn(0f, 1f) * 0.12f else 1f,
        tween(90),
        label = "micScale"
    )
    val color by animateColorAsState(
        if (listening) PrimaryAmber else SurfaceContainerHigh,
        tween(170),
        label = "micColor"
    )

    Box(Modifier.size(86.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(66.dp).graphicsLayer {
                scaleX = ringScale
                scaleY = ringScale
                alpha = ringAlpha
            }.clip(CircleShape).background(PrimaryAmber)
        )
        Box(
            Modifier.size(68.dp).graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.clip(CircleShape).background(color).clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (listening) Icons.Default.MicOff else Icons.Default.Mic,
                if (listening) "Stop dictation" else "Start dictation",
                tint = if (listening) DarkBackground else PrimaryAmber,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun UtilityKey(
    modifier: Modifier,
    highlighted: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(90), label = "keyScale")
    Box(
        modifier = modifier.height(48.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlighted) PrimaryAmber else SurfaceContainer)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

private fun formatElapsed(milliseconds: Long): String {
    val seconds = milliseconds / 1_000L
    return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
}
