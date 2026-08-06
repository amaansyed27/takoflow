package com.example.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TakoFlowPreferences
import com.example.speech.AdaptiveLanguageModel
import com.example.speech.FormattingProfile
import com.example.speech.FormattingProfileStore
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.SurfaceContainerHigh
import kotlinx.coroutines.launch

@Composable
fun VoiceProfilesScreen(
    preferences: TakoFlowPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileStore = remember { FormattingProfileStore.get(context) }
    val adaptiveModel = remember { AdaptiveLanguageModel.get(context) }
    val profiles by profileStore.profiles.collectAsState()
    val activeProfileId by preferences.activeProfileId.collectAsState(initial = "default")
    var editingProfile by remember { mutableStateOf<FormattingProfile?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            Column {
                Text("Formatting profiles", color = OnSurfaceDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Formatting and vocabulary adapt per profile", color = OnSurfaceVariantDark, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            profiles.forEach { profile ->
                ProfileCard(
                    profile = profile,
                    icon = when (profile.id) {
                        "work" -> Icons.Default.Work
                        "notes" -> Icons.Default.EditNote
                        else -> Icons.Default.Mic
                    },
                    learnedWords = adaptiveModel.learnedWordCount(profile.id),
                    isActive = activeProfileId == profile.id,
                    onSelect = { scope.launch { preferences.setActiveProfileId(profile.id) } },
                    onEdit = { editingProfile = profile }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryAmber)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Custom words, replacements and learned typing history stay on this device. " +
                        "Password fields are never learned.",
                    color = OnSurfaceVariantDark,
                    fontSize = 13.sp
                )
            }
        }
    }

    editingProfile?.let { profile ->
        ProfileEditorDialog(
            profile = profile,
            onDismiss = { editingProfile = null },
            onReset = {
                profileStore.reset(profile.id)
                adaptiveModel.clearProfile(profile.id)
                editingProfile = null
            },
            onSave = {
                profileStore.save(it)
                editingProfile = null
            }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: FormattingProfile,
    icon: ImageVector,
    learnedWords: Int,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        activeGlow = isActive,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isActive) PrimaryAmber.copy(alpha = 0.15f) else SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isActive) PrimaryAmber else OnSurfaceVariantDark
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.name, color = OnSurfaceDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (isActive) {
                        Spacer(Modifier.width(8.dp))
                        Text("ACTIVE", color = PrimaryAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(profile.description, color = OnSurfaceVariantDark, fontSize = 12.sp)
                Text(
                    "${profile.customWords.size} custom · $learnedWords learned words",
                    color = PrimaryAmber,
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${profile.name}", tint = OnSurfaceVariantDark)
            }
        }
    }
}

@Composable
private fun ProfileEditorDialog(
    profile: FormattingProfile,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onSave: (FormattingProfile) -> Unit
) {
    var name by remember(profile.id) { mutableStateOf(profile.name) }
    var description by remember(profile.id) { mutableStateOf(profile.description) }
    var customWords by remember(profile.id) {
        mutableStateOf(profile.customWords.sorted().joinToString(", "))
    }
    var replacements by remember(profile.id) {
        mutableStateOf(profile.replacements.entries.joinToString("\n") { "${it.key} = ${it.value}" })
    }
    var prefix by remember(profile.id) { mutableStateOf(profile.prefix) }
    var suffix by remember(profile.id) { mutableStateOf(profile.suffix) }
    var bulletPrefix by remember(profile.id) { mutableStateOf(profile.bulletPrefix) }
    var capitalize by remember(profile.id) { mutableStateOf(profile.capitalizeSentences) }
    var punctuation by remember(profile.id) { mutableStateOf(profile.addPunctuation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${profile.name}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customWords,
                    onValueChange = { customWords = it },
                    label = { Text("Custom vocabulary") },
                    supportingText = { Text("Comma-separated names, technical terms and slang") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = replacements,
                    onValueChange = { replacements = it },
                    label = { Text("Word replacements") },
                    supportingText = { Text("One per line: source = replacement") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                OutlinedTextField(
                    value = prefix,
                    onValueChange = { prefix = it },
                    label = { Text("Prefix") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = suffix,
                    onValueChange = { suffix = it },
                    label = { Text("Suffix") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                ProfileSwitch("Bullet prefix", bulletPrefix) { bulletPrefix = it }
                ProfileSwitch("Capitalize sentences", capitalize) { capitalize = it }
                ProfileSwitch("Add punctuation", punctuation) { punctuation = it }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) { Text("Reset") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedWords = customWords
                        .split(',', '\n')
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .toSet()
                    val parsedReplacements = replacements
                        .lineSequence()
                        .mapNotNull { line ->
                            val parts = line.split('=', limit = 2).map(String::trim)
                            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                                parts[0] to parts[1]
                            } else {
                                null
                            }
                        }
                        .toMap(linkedMapOf())

                    onSave(
                        profile.copy(
                            name = name.trim().ifBlank { profile.name },
                            description = description.trim(),
                            prefix = prefix,
                            suffix = suffix,
                            bulletPrefix = bulletPrefix,
                            capitalizeSentences = capitalize,
                            addPunctuation = punctuation,
                            replacements = parsedReplacements,
                            customWords = parsedWords
                        )
                    )
                }
            ) {
                Text("Save")
            }
        }
    )
}

@Composable
private fun ProfileSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
