package com.example.speech

import java.util.Locale

/**
 * Conservative, deterministic text cleanup for local dictation.
 *
 * This intentionally avoids aggressive rewriting: it fixes common spelling errors,
 * spoken-text contractions, duplicated words, punctuation spacing and obvious question
 * endings without changing the user's meaning.
 */
object TextQualityProcessor {
    private val spellingCorrections = linkedMapOf(
        "teh" to "the",
        "adn" to "and",
        "recieve" to "receive",
        "definately" to "definitely",
        "seperate" to "separate",
        "occured" to "occurred",
        "untill" to "until",
        "becuase" to "because",
        "wierd" to "weird",
        "alot" to "a lot",
        "adress" to "address",
        "accomodate" to "accommodate",
        "goverment" to "government",
        "tommorow" to "tomorrow",
        "thier" to "their",
        "acheive" to "achieve",
        "sucessful" to "successful"
    )

    private val grammarCorrections = linkedMapOf(
        "im" to "I'm",
        "ive" to "I've",
        "dont" to "don't",
        "doesnt" to "doesn't",
        "didnt" to "didn't",
        "cant" to "can't",
        "couldnt" to "couldn't",
        "wouldnt" to "wouldn't",
        "shouldnt" to "shouldn't",
        "wont" to "won't",
        "isnt" to "isn't",
        "arent" to "aren't",
        "wasnt" to "wasn't",
        "werent" to "weren't",
        "havent" to "haven't",
        "hasnt" to "hasn't",
        "hadnt" to "hadn't",
        "youre" to "you're",
        "youve" to "you've",
        "youll" to "you'll",
        "theyre" to "they're",
        "weve" to "we've",
        "thats" to "that's",
        "theres" to "there's",
        "lets" to "let's"
    )

    private val questionStarters = setOf(
        "who", "what", "when", "where", "why", "how", "which",
        "can", "could", "would", "will", "should",
        "do", "does", "did", "is", "are", "am", "was", "were",
        "have", "has", "had"
    )

    fun clean(
        text: String,
        spellCorrection: Boolean,
        grammarCorrection: Boolean
    ): String {
        var result = normalizeSpacing(text)
        if (spellCorrection) result = replaceKnownWords(result, spellingCorrections)
        if (grammarCorrection) result = correctGrammar(result)
        return normalizePunctuationSpacing(result)
    }

    fun capitalizeSentences(text: String): String {
        if (text.isBlank()) return text
        val output = StringBuilder(text.length)
        var sentenceStart = true
        text.forEach { character ->
            val next = if (sentenceStart && character.isLetter()) {
                sentenceStart = false
                character.titlecaseChar()
            } else {
                character
            }
            output.append(next)
            if (character == '.' || character == '?' || character == '!') {
                sentenceStart = true
            }
        }
        return output.toString()
    }

    fun terminalPunctuation(text: String, grammarCorrection: Boolean): Char {
        if (!grammarCorrection) return '.'
        val firstWord = Regex("[\\p{L}']+")
            .find(text.trim())
            ?.value
            ?.lowercase(Locale.getDefault())
            .orEmpty()
        return if (firstWord in questionStarters) '?' else '.'
    }

    private fun correctGrammar(text: String): String {
        var result = replaceKnownWords(text, grammarCorrections)
        result = Regex("(?<![\\p{L}\\p{N}])i(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
            .replace(result, "I")

        val duplicateWord = Regex(
            "\\b([\\p{L}][\\p{L}'-]*)\\s+\\1\\b",
            RegexOption.IGNORE_CASE
        )
        while (duplicateWord.containsMatchIn(result)) {
            result = duplicateWord.replace(result) { it.groupValues[1] }
        }
        return result
    }

    private fun replaceKnownWords(
        text: String,
        replacements: Map<String, String>
    ): String {
        var result = text
        replacements.forEach { (source, replacement) ->
            val pattern = Regex(
                "(?<![\\p{L}\\p{N}])${Regex.escape(source)}(?![\\p{L}\\p{N}])",
                RegexOption.IGNORE_CASE
            )
            result = pattern.replace(result) { match ->
                preserveLeadingCase(match.value, replacement)
            }
        }
        return result
    }

    private fun preserveLeadingCase(source: String, replacement: String): String {
        if (source.firstOrNull()?.isUpperCase() != true) return replacement
        return replacement.replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }

    private fun normalizeSpacing(text: String): String =
        text.trim().replace(Regex("[\\t ]+"), " ")

    private fun normalizePunctuationSpacing(text: String): String =
        text
            .replace(Regex("\\s+([,.;!?])"), "\$1")
            .replace(Regex("([,.;!?])(?=[\\p{L}\\p{N}])"), "\$1 ")
            .replace(Regex(" {2,}"), " ")
            .trim()
}
