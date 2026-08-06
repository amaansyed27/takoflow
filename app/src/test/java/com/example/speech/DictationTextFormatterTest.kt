package com.example.speech

import org.junit.Assert.assertEquals
import org.junit.Test

class DictationTextFormatterTest {
    @Test
    fun defaultProfileCapitalizesAndPunctuates() {
        assertEquals(
            "Hello world.",
            DictationTextFormatter.format(
                raw = "hello world",
                autoCapitalization = true,
                autoPunctuation = true,
                profile = "default"
            )
        )
    }

    @Test
    fun existingPunctuationIsPreserved() {
        assertEquals(
            "Already done!",
            DictationTextFormatter.format(
                raw = "already done!",
                autoCapitalization = true,
                autoPunctuation = true,
                profile = "default"
            )
        )
    }

    @Test
    fun workProfileExpandsCasualPhrasesBeforeCapitalization() {
        assertEquals(
            "Going to send it because I want to finish.",
            DictationTextFormatter.format(
                raw = "gonna send it because I wanna finish",
                autoCapitalization = true,
                autoPunctuation = true,
                profile = "work"
            )
        )
    }

    @Test
    fun notesProfileAddsBulletWithoutForcedPunctuation() {
        assertEquals(
            "- Buy milk",
            DictationTextFormatter.format(
                raw = "buy milk",
                autoCapitalization = true,
                autoPunctuation = true,
                profile = "notes"
            )
        )
    }

    @Test
    fun customProfileAppliesPrefixSuffixAndReplacement() {
        val profile = FormattingProfile(
            id = "default",
            name = "Custom",
            description = "",
            prefix = "TODO:",
            suffix = "#work",
            replacements = mapOf("asap" to "as soon as possible")
        )

        assertEquals(
            "TODO: Send this as soon as possible. #work",
            DictationTextFormatter.format(
                raw = "send this asap",
                autoCapitalization = true,
                autoPunctuation = true,
                profile = profile
            )
        )
    }
}
