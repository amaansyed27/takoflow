package com.example.speech

import java.util.Locale
import kotlin.math.abs

object DictationTextFormatter {
    private val wordPattern = Regex("[\\p{L}][\\p{L}\\p{N}'-]*")

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
        var result = raw.trim().replace(Regex("\\s+"), " ")

        profile.replacements.entries
            .sortedByDescending { it.key.length }
            .forEach { (source, replacement) ->
                if (source.isBlank() || replacement.isBlank()) return@forEach
                val pattern = Regex(
                    "(?<![\\p{L}\\p{N}])${Regex.escape(source)}(?![\\p{L}\\p{N}])",
                    RegexOption.IGNORE_CASE
                )
                result = pattern.replace(result, replacement)
            }

        result = applyPreferredSpellings(result, profile.customWords)

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
            result = "• $result"
        }

        return buildString {
            if (profile.prefix.isNotBlank()) append(profile.prefix.trimEnd()).append(' ')
            append(result)
            if (profile.suffix.isNotBlank()) append(' ').append(profile.suffix.trimStart())
        }.trim()
    }

    private fun applyPreferredSpellings(text: String, preferredWords: Set<String>): String {
        if (preferredWords.isEmpty()) return text

        val words = preferredWords
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase(Locale.getDefault()) }

        return wordPattern.replace(text) { match ->
            val source = match.value
            val normalized = source.lowercase(Locale.getDefault())

            val exact = words.firstOrNull {
                it.lowercase(Locale.getDefault()) == normalized
            }
            if (exact != null) return@replace exact

            if (normalized.length < 4) return@replace source
            val maximumDistance = if (normalized.length >= 8) 2 else 1

            val candidates = words.asSequence()
                .filter {
                    val candidate = it.lowercase(Locale.getDefault())
                    candidate.firstOrNull() == normalized.firstOrNull() &&
                        abs(candidate.length - normalized.length) <= maximumDistance
                }
                .map { preferred ->
                    preferred to editDistance(
                        normalized,
                        preferred.lowercase(Locale.getDefault()),
                        maximumDistance
                    )
                }
                .filter { it.second <= maximumDistance }
                .sortedBy { it.second }
                .toList()

            val best = candidates.firstOrNull() ?: return@replace source
            val tied = candidates.drop(1).firstOrNull()?.second == best.second
            if (tied) source else best.first
        }
    }

    private fun editDistance(first: String, second: String, limit: Int): Int {
        if (first == second) return 0
        if (abs(first.length - second.length) > limit) return limit + 1

        var previous = IntArray(second.length + 1) { it }
        var current = IntArray(second.length + 1)

        for (firstIndex in first.indices) {
            current[0] = firstIndex + 1
            var rowMinimum = current[0]

            for (secondIndex in second.indices) {
                val substitution = previous[secondIndex] +
                    if (first[firstIndex] == second[secondIndex]) 0 else 1
                val insertion = current[secondIndex] + 1
                val deletion = previous[secondIndex + 1] + 1
                current[secondIndex + 1] = minOf(substitution, insertion, deletion)
                rowMinimum = minOf(rowMinimum, current[secondIndex + 1])
            }

            if (rowMinimum > limit) return limit + 1
            val swap = previous
            previous = current
            current = swap
        }

        return previous[second.length]
    }
}
