package com.example.speech

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class FormattingProfile(
    val id: String,
    val name: String,
    val description: String,
    val prefix: String = "",
    val suffix: String = "",
    val bulletPrefix: Boolean = false,
    val capitalizeSentences: Boolean = true,
    val addPunctuation: Boolean = true,
    val replacements: Map<String, String> = emptyMap(),
    val customWords: Set<String> = emptySet()
)

class FormattingProfileStore private constructor(context: Context) {
    companion object {
        private const val PREFS_NAME = "takoflow_formatting_profiles"

        @Volatile
        private var instance: FormattingProfileStore? = null

        fun get(context: Context): FormattingProfileStore =
            instance ?: synchronized(this) {
                instance ?: FormattingProfileStore(context.applicationContext).also { instance = it }
            }

        fun builtInProfile(id: String): FormattingProfile =
            builtIns.firstOrNull { it.id == id } ?: builtIns.first()

        fun currentProfile(id: String): FormattingProfile =
            instance?.getProfile(id) ?: builtInProfile(id)

        val builtIns: List<FormattingProfile> = listOf(
            FormattingProfile(
                id = "default",
                name = "Default",
                description = "Natural sentences using your global punctuation settings"
            ),
            FormattingProfile(
                id = "work",
                name = "Work",
                description = "More formal wording for messages, reports and email",
                replacements = linkedMapOf(
                    "gonna" to "going to",
                    "wanna" to "want to",
                    "can't" to "cannot",
                    "won't" to "will not",
                    "asap" to "as soon as possible"
                )
            ),
            FormattingProfile(
                id = "notes",
                name = "Notes",
                description = "Fast bullet-style notes without forced sentence punctuation",
                bulletPrefix = true,
                addPunctuation = false
            )
        )
    }

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _profiles = MutableStateFlow(loadAll())
    val profiles: StateFlow<List<FormattingProfile>> = _profiles.asStateFlow()

    fun getProfile(id: String): FormattingProfile =
        _profiles.value.firstOrNull { it.id == id } ?: builtInProfile(id)

    fun save(profile: FormattingProfile) {
        require(profile.id in builtIns.map { it.id }) { "Unknown profile id: ${profile.id}" }
        preferences.edit()
            .putString(profileKey(profile.id), encode(profile).toString())
            .apply()
        _profiles.value = loadAll()
    }

    fun reset(profileId: String) {
        preferences.edit().remove(profileKey(profileId)).apply()
        _profiles.value = loadAll()
    }

    private fun loadAll(): List<FormattingProfile> = builtIns.map { builtIn ->
        val stored = preferences.getString(profileKey(builtIn.id), null)
        if (stored.isNullOrBlank()) {
            builtIn
        } else {
            runCatching { decode(JSONObject(stored), builtIn) }.getOrDefault(builtIn)
        }
    }

    private fun profileKey(id: String): String = "profile_$id"

    private fun encode(profile: FormattingProfile): JSONObject = JSONObject().apply {
        put("id", profile.id)
        put("name", profile.name)
        put("description", profile.description)
        put("prefix", profile.prefix)
        put("suffix", profile.suffix)
        put("bulletPrefix", profile.bulletPrefix)
        put("capitalizeSentences", profile.capitalizeSentences)
        put("addPunctuation", profile.addPunctuation)

        put("replacements", JSONObject().apply {
            profile.replacements.forEach { (source, replacement) ->
                put(source, replacement)
            }
        })

        put("customWords", JSONArray().apply {
            profile.customWords.sorted().forEach { word -> put(word) }
        })
    }

    private fun decode(json: JSONObject, fallback: FormattingProfile): FormattingProfile {
        val replacementsJson = json.optJSONObject("replacements") ?: JSONObject()
        val replacements = linkedMapOf<String, String>()
        val replacementKeys = replacementsJson.keys()
        while (replacementKeys.hasNext()) {
            val key = replacementKeys.next()
            val value = replacementsJson.optString(key).trim()
            if (key.isNotBlank() && value.isNotBlank()) {
                replacements[key.trim()] = value
            }
        }

        val wordsJson = json.optJSONArray("customWords") ?: JSONArray()
        val customWords = buildSet {
            for (index in 0 until wordsJson.length()) {
                wordsJson.optString(index)
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
        }

        return fallback.copy(
            name = json.optString("name", fallback.name).ifBlank { fallback.name },
            description = json.optString("description", fallback.description),
            prefix = json.optString("prefix", fallback.prefix),
            suffix = json.optString("suffix", fallback.suffix),
            bulletPrefix = json.optBoolean("bulletPrefix", fallback.bulletPrefix),
            capitalizeSentences = json.optBoolean(
                "capitalizeSentences",
                fallback.capitalizeSentences
            ),
            addPunctuation = json.optBoolean("addPunctuation", fallback.addPunctuation),
            replacements = replacements,
            customWords = customWords
        )
    }
}
