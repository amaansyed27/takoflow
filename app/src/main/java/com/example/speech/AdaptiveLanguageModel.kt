package com.example.speech

import android.content.Context
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs

class AdaptiveLanguageModel private constructor(context: Context) {
    companion object {
        private const val PREFS_NAME = "takoflow_adaptive_language"
        private const val MAX_WORDS_PER_PROFILE = 1_500
        private const val MAX_BIGRAMS_PER_PROFILE = 3_000

        @Volatile
        private var instance: AdaptiveLanguageModel? = null

        fun get(context: Context): AdaptiveLanguageModel =
            instance ?: synchronized(this) {
                instance ?: AdaptiveLanguageModel(context.applicationContext).also { instance = it }
            }

        private val tokenRegex = Regex("[\\p{L}\\p{N}']+")

        private val commonWords = listOf(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
            "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
            "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
            "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
            "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
            "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
            "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
            "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
            "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
            "even", "new", "want", "because", "these", "give", "day", "most", "us", "is",
            "are", "was", "were", "has", "had", "did", "does", "should", "could", "please",
            "thanks", "thank", "hello", "hi", "yes", "okay", "today", "tomorrow", "project",
            "meeting", "send", "share", "update", "review", "report", "message", "call", "need"
        ).withIndex().associate { (index, word) -> word to (commonWordsWeight(index)) }

        private fun commonWordsWeight(index: Int): Int = (400 - index).coerceAtLeast(20)
    }

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val lock = Any()

    fun learnText(text: String, profileId: String) {
        val words = tokenize(text)
        if (words.isEmpty()) return

        synchronized(lock) {
            val wordCounts = readCounts(wordsKey(profileId))
            val bigramCounts = readCounts(bigramsKey(profileId))

            words.forEach { word ->
                wordCounts[word] = (wordCounts[word] ?: 0) + 1
            }
            words.zipWithNext().forEach { (first, second) ->
                val key = bigramKey(first, second)
                bigramCounts[key] = (bigramCounts[key] ?: 0) + 1
            }

            writeCounts(wordsKey(profileId), trimCounts(wordCounts, MAX_WORDS_PER_PROFILE))
            writeCounts(bigramsKey(profileId), trimCounts(bigramCounts, MAX_BIGRAMS_PER_PROFILE))
        }
    }

    fun learnWord(word: String, previousWord: String?, profileId: String) {
        val normalized = normalize(word) ?: return
        synchronized(lock) {
            val wordCounts = readCounts(wordsKey(profileId))
            wordCounts[normalized] = (wordCounts[normalized] ?: 0) + 1
            writeCounts(wordsKey(profileId), trimCounts(wordCounts, MAX_WORDS_PER_PROFILE))

            val previous = normalize(previousWord.orEmpty())
            if (previous != null) {
                val bigramCounts = readCounts(bigramsKey(profileId))
                val key = bigramKey(previous, normalized)
                bigramCounts[key] = (bigramCounts[key] ?: 0) + 1
                writeCounts(
                    bigramsKey(profileId),
                    trimCounts(bigramCounts, MAX_BIGRAMS_PER_PROFILE)
                )
            }
        }
    }

    fun suggestions(
        prefix: String,
        previousWord: String?,
        profile: FormattingProfile,
        limit: Int = 3
    ): List<String> {
        val normalizedPrefix = prefix.lowercase(Locale.getDefault())
        val previous = normalize(previousWord.orEmpty())

        synchronized(lock) {
            val learned = readCounts(wordsKey(profile.id))
            val bigrams = readCounts(bigramsKey(profile.id))
            val vocabulary = linkedMapOf<String, Int>()

            commonWords.forEach { (word, count) -> vocabulary[word] = count }
            learned.forEach { (word, count) ->
                vocabulary[word] = (vocabulary[word] ?: 0) + count * 12
            }
            profile.customWords.forEach { custom ->
                normalize(custom)?.let { vocabulary[it] = (vocabulary[it] ?: 0) + 700 }
            }

            return vocabulary.asSequence()
                .filter { (word, _) ->
                    if (normalizedPrefix.isBlank()) true
                    else word.startsWith(normalizedPrefix) && word != normalizedPrefix
                }
                .map { (word, frequency) ->
                    val bigramBoost = if (previous == null) {
                        0
                    } else {
                        (bigrams[bigramKey(previous, word)] ?: 0) * 150
                    }
                    val prefixBoost = if (normalizedPrefix.isBlank()) 0 else 100 - word.length
                    word to frequency + bigramBoost + prefixBoost
                }
                .sortedByDescending { it.second }
                .map { preserveCase(it.first, prefix) }
                .distinct()
                .take(limit)
                .toList()
        }
    }

    fun correct(
        word: String,
        previousWord: String?,
        profile: FormattingProfile
    ): String {
        val normalized = normalize(word) ?: return word
        if (normalized.length < 3 || word.any(Char::isDigit) || word.all(Char::isUpperCase)) return word

        synchronized(lock) {
            val learned = readCounts(wordsKey(profile.id))
            val bigrams = readCounts(bigramsKey(profile.id))
            val custom = profile.customWords.mapNotNull(::normalize).toSet()

            if (normalized in commonWords || normalized in learned || normalized in custom) return word

            val vocabulary = buildMap<String, Int> {
                commonWords.forEach { (candidate, weight) -> put(candidate, weight) }
                learned.forEach { (candidate, count) -> put(candidate, (get(candidate) ?: 0) + count * 20) }
                custom.forEach { candidate -> put(candidate, (get(candidate) ?: 0) + 1_000) }
            }

            val maximumDistance = if (normalized.length <= 5) 1 else 2
            val previous = normalize(previousWord.orEmpty())

            val candidate = vocabulary.asSequence()
                .filter { (candidate, _) ->
                    abs(candidate.length - normalized.length) <= maximumDistance &&
                        candidate.firstOrNull() == normalized.firstOrNull()
                }
                .mapNotNull { (candidate, frequency) ->
                    val distance = editDistance(normalized, candidate, maximumDistance)
                    if (distance > maximumDistance) return@mapNotNull null

                    val learnedCount = learned[candidate] ?: 0
                    if (candidate !in commonWords && candidate !in custom && learnedCount < 2) {
                        return@mapNotNull null
                    }

                    val bigramBoost = if (previous == null) {
                        0
                    } else {
                        (bigrams[bigramKey(previous, candidate)] ?: 0) * 250
                    }
                    Triple(candidate, distance, frequency + bigramBoost)
                }
                .sortedWith(compareBy<Triple<String, Int, Int>> { it.second }.thenByDescending { it.third })
                .firstOrNull()
                ?.first
                ?: return word

            return preserveCase(candidate, word)
        }
    }

    fun learnedWordCount(profileId: String): Int = synchronized(lock) {
        readCounts(wordsKey(profileId)).size
    }

    fun clearProfile(profileId: String) {
        synchronized(lock) {
            preferences.edit()
                .remove(wordsKey(profileId))
                .remove(bigramsKey(profileId))
                .apply()
        }
    }

    fun clearAll() {
        synchronized(lock) {
            preferences.edit().clear().apply()
        }
    }

    private fun tokenize(text: String): List<String> =
        tokenRegex.findAll(text)
            .mapNotNull { normalize(it.value) }
            .toList()

    private fun normalize(word: String): String? =
        word.trim(' ', '.', ',', '!', '?', ':', ';', '"', '(', ')', '[', ']', '{', '}')
            .lowercase(Locale.getDefault())
            .takeIf { it.length >= 2 && it.any(Char::isLetter) }

    private fun preserveCase(candidate: String, source: String): String = when {
        source.all(Char::isUpperCase) -> candidate.uppercase(Locale.getDefault())
        source.firstOrNull()?.isUpperCase() == true -> candidate.replaceFirstChar(Char::uppercase)
        else -> candidate
    }

    private fun wordsKey(profileId: String): String = "words_$profileId"
    private fun bigramsKey(profileId: String): String = "bigrams_$profileId"
    private fun bigramKey(first: String, second: String): String = "$first\u0001$second"

    private fun readCounts(key: String): MutableMap<String, Int> {
        val raw = preferences.getString(key, null) ?: return linkedMapOf()
        return runCatching {
            val json = JSONObject(raw)
            val output = linkedMapOf<String, Int>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val item = keys.next()
                val count = json.optInt(item, 0)
                if (count > 0) output[item] = count
            }
            output
        }.getOrDefault(linkedMapOf())
    }

    private fun writeCounts(key: String, counts: Map<String, Int>) {
        val json = JSONObject()
        counts.forEach { (item, count) -> json.put(item, count) }
        preferences.edit().putString(key, json.toString()).apply()
    }

    private fun trimCounts(counts: Map<String, Int>, maximum: Int): Map<String, Int> =
        counts.entries
            .sortedByDescending { it.value }
            .take(maximum)
            .associateTo(linkedMapOf()) { it.key to it.value }

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
