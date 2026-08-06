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
                profile = FormattingProfileStore.builtInProfile("default")
            )
        )
    }

    @Test
    fun workProfileAppliesRealPhraseReplacements() {
        assertEquals(
            "Going to send it because I want to finish.",
            DictationTextFormatter.format(
                raw = "gonna send it because I wanna finish",
                autoCapitalization = true,
                autoPunctuation = true,
                profile = FormattingProfileStore.builtInProfile("work")
            )
        )
    }

    @Test
    fun notesProfileUsesBulletWithoutForcedPunctuation() {
        assertEquals(
            "• Buy milk",
            DictationTextFormatter.format(
                raw = "buy milk",
                autoCapitalization = true,
                autoPunctuation = true,
                profile = FormattingProfileStore.builtInProfile("notes")
            )
        )
    }

    @Test
    fun preferredSpellingsCorrectCloseRecognitionResults() {
        val profile = FormattingProfile(
            id = "test",
            name = "Test",
            description = "",
            customWords = setOf("TakoFlow")
        )
        assertEquals(
            "Open TakoFlow.",
            DictationTextFormatter.format(
                raw = "open takoflo",
                autoCapitalization = true,
                autoPunctuation = true,
                profile = profile
            )
        )
    }

    @Test
    fun prefixAndSuffixArePersistedFormattingRules() {
        val profile = FormattingProfile(
            id = "test",
            name = "Test",
            description = "",
            prefix = "Note:",
            suffix = "— sent locally"
        )
        assertEquals(
            "Note: Review complete. — sent locally",
            DictationTextFormatter.format(
                raw = "review complete",
                autoCapitalization = true,
                autoPunctuation = true,
                profile = profile
            )
        )
    }
}
