package com.example.speech

import java.util.Locale

object DictationTextFormatter {
    fun format(
        raw: String,
        autoCapitalization: Boolean,
        autoPunctuation: Boolean,
        profile: String
    ): String {
        var result = raw.trim()

        if (profile == "work") {
            result = result
                .replace(Regex("\\bgonna\\b", RegexOption.IGNORE_CASE), "going to")
                .replace(Regex("\\bwanna\\b", RegexOption.IGNORE_CASE), "want to")
        }

        if (autoCapitalization && result.isNotEmpty()) {
            result = result.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }

        if (
            autoPunctuation && result.isNotEmpty() &&
            result.last() !in charArrayOf('.', '?', '!')
        ) {
            result += "."
        }

        return if (profile == "notes" && result.isNotEmpty()) "- $result" else result
    }
}
