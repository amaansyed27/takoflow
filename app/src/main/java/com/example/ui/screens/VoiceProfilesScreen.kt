package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.TakoFlowAppContainer
import com.example.speech.DictationTextFormatter
import com.example.speech.FormattingProfile
import com.example.ui.components.BrandHeader
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.PrimaryAmber
import com.example.ui.viewmodel.ProfilesViewModel
import com.example.ui.viewmodel.takoFlowViewModel

@Composable
fun VoiceProfilesScreen(
    container: TakoFlowAppContainer,
    onBack: () -> Unit
) {
    val viewModel: ProfilesViewModel = takoFlowViewModel { ProfilesViewModel(container) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<FormattingProfile?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        BrandHeader("Formatting profiles", "Control how completed dictation is written", onBack = onBack)
        Spacer(Modifier.padding(8.dp))

        uiState.profiles.forEach { profile ->
            ProfileCard(
                profile = profile,
                active = uiState.activeId == profile.id,
                onSelect = { viewModel.select(profile.id) },
                onEdit = { editing = profile }
            )
            Spacer(Modifier.padding(5.dp))
        }

        Button(
            onClick = {
                viewModel.create().getOrNull()?.let { editing = it }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(PrimaryAmber, DarkBackground)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.padding(4.dp))
            Text("Create custom profile", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.padding(8.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Profiles are real formatting rules", color = OnSurfaceDark, fontWeight = FontWeight.Bold)
                Text(
                    "Replacements, preferred spellings, prefixes, suffixes, bullets, capitalization and punctuation are applied to Vosk and Whisper results on this device.",
                    color = OnSurfaceVariantDark,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
        Spacer(Modifier.padding(12.dp))
    }

    editing?.let { profile ->
        ProfileEditor(
            profile = profile,
            builtIn = viewModel.isBuiltIn(profile.id),
            onDismiss = { editing = null },
            onSave = {
                viewModel.save(it)
                editing = null
            },
            onResetOrDelete = {
                viewModel.resetOrDelete(profile.id)
                editing = null
            }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: FormattingProfile,
    active: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), activeGlow = active, onClick = onSelect) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Style, null, tint = if (active) PrimaryAmber else OnSurfaceVariantDark)
            Spacer(Modifier.padding(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.name, color = OnSurfaceDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (active) {
                        Spacer(Modifier.padding(4.dp))
                        Text("ACTIVE", color = PrimaryAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(profile.description, color = OnSurfaceVariantDark, fontSize = 12.sp)
                Text(
                    "${profile.customWords.size} preferred words · ${profile.replacements.size} replacements",
                    color = PrimaryAmber,
                    fontSize = 10.sp
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit ${profile.name}", tint = OnSurfaceVariantDark)
            }
        }
    }
}

@Composable
private fun ProfileEditor(
    profile: FormattingProfile,
    builtIn: Boolean,
    onDismiss: () -> Unit,
    onSave: (FormattingProfile) -> Unit,
    onResetOrDelete: () -> Unit
) {
    var name by remember(profile.id) { mutableStateOf(profile.name) }
    var description by remember(profile.id) { mutableStateOf(profile.description) }
    var words by remember(profile.id) { mutableStateOf(profile.customWords.joinToString(", ")) }
    var replacements by remember(profile.id) {
        mutableStateOf(profile.replacements.entries.joinToString("\n") { "${it.key} = ${it.value}" })
    }
    var prefix by remember(profile.id) { mutableStateOf(profile.prefix) }
    var suffix by remember(profile.id) { mutableStateOf(profile.suffix) }
    var bullets by remember(profile.id) { mutableStateOf(profile.bulletPrefix) }
    var capitals by remember(profile.id) { mutableStateOf(profile.capitalizeSentences) }
    var punctuation by remember(profile.id) { mutableStateOf(profile.addPunctuation) }

    val previewProfile = profile.copy(
        name = name,
        description = description,
        prefix = prefix,
        suffix = suffix,
        bulletPrefix = bullets,
        capitalizeSentences = capitals,
        addPunctuation = punctuation,
        customWords = parseWords(words),
        replacements = parseReplacements(replacements)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (builtIn) "Edit ${profile.name}" else "Custom profile") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 590.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text("Profile name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    description,
                    { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    words,
                    { words = it },
                    label = { Text("Preferred spellings") },
                    supportingText = { Text("Comma-separated names, products or technical terms") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    replacements,
                    { replacements = it },
                    label = { Text("Phrase replacements") },
                    supportingText = { Text("One per line: source = replacement") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    prefix,
                    { prefix = it },
                    label = { Text("Prefix") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    suffix,
                    { suffix = it },
                    label = { Text("Suffix") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ProfileToggle("Bullet prefix", bullets) { bullets = it }
                ProfileToggle("Capitalize sentences", capitals) { capitals = it }
                ProfileToggle("Add punctuation", punctuation) { punctuation = it }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("PREVIEW", color = PrimaryAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            DictationTextFormatter.format(
                                "gonna send the Takoflow update",
                                true,
                                true,
                                previewProfile
                            ),
                            color = OnSurfaceDark,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onResetOrDelete) {
                    Icon(if (builtIn) Icons.Default.Edit else Icons.Default.Delete, null)
                    Text(if (builtIn) "Reset" else "Delete")
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        previewProfile.copy(
                            name = name.trim().ifBlank { "Profile" },
                            description = description.trim()
                        )
                    )
                }
            ) { Text("Save") }
        }
    )
}

@Composable
private fun ProfileToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun parseWords(value: String): Set<String> =
    value.split(',', '\n').map(String::trim).filter(String::isNotBlank).toCollection(linkedSetOf())

private fun parseReplacements(value: String): Map<String, String> =
    value.lineSequence().mapNotNull { line ->
        val parts = line.split('=', limit = 2).map(String::trim)
        if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) parts[0] to parts[1] else null
    }.associateTo(linkedMapOf()) { it }
