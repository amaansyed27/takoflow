package com.example.speech

import java.util.Locale

object DictationTextFormatter {
    fun format(
        raw: String,
        autoCapitalization: Boolean,
        autoPunctuation: Boolean,
        profile: String
    ): String = format(
        raw = raw,
        autoCapitalization = autoCapitalization,
        autoPunctuation = autoPunctuation,
        profile = FormattingProfileStore.currentProfile(profile)
    )

    fun format(
        raw: String,
        autoCapitalization: Boolean,
        autoPunctuation: Boolean,
        profile: FormattingProfile
    ): String {
        var result = raw.trim()

        profile.replacements.entries
            .sortedByDescending { it.key.length }
            .forEach { (source, replacement) ->
                if (source.isBlank() || replacement.isBlank()) return@forEach
                val pattern = Regex(
                    "(?<!\\p{L})${Regex.escape(source)}(?!\\p{L})",
                    RegexOption.IGNORE_CASE
                )
                result = pattern.replace(result, replacement)
            }

        if (autoCapitalization && profile.capitalizeSentences && result.isNotEmpty()) {
            result = result.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }

        if (
            autoPunctuation && profile.addPunctuation && result.isNotEmpty() &&
            result.last() !in charArrayOf('.', '?', '!', ':', ';')
        ) {
            result += "."
        }

        if (profile.bulletPrefix && result.isNotEmpty()) {
            result = "- $result"
        }

        return buildString {
            if (profile.prefix.isNotBlank()) append(profile.prefix.trimEnd()).append(' ')
            append(result)
            if (profile.suffix.isNotBlank()) append(' ').append(profile.suffix.trimStart())
        }.trim()
    }
}
