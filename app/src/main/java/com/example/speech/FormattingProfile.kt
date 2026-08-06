package com.example.speech

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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
        private const val CUSTOM_IDS_KEY = "custom_profile_ids"
        private const val MAX_CUSTOM_PROFILES = 12
        private const val MAX_CUSTOM_WORDS = 200
        private const val MAX_REPLACEMENTS = 100

        @Volatile
        private var instance: FormattingProfileStore? = null

        @Volatile
        private var currentInstance: FormattingProfileStore? = null

        fun get(context: Context): FormattingProfileStore =
            instance ?: synchronized(this) {
                instance ?: FormattingProfileStore(context.applicationContext).also {
                    instance = it
                    currentInstance = it
                }
            }

        fun currentProfile(id: String): FormattingProfile =
            currentInstance?.getProfile(id) ?: builtInProfile(id)

        fun builtInProfile(id: String): FormattingProfile =
            builtIns.firstOrNull { it.id == id } ?: builtIns.first()

        fun isBuiltIn(id: String): Boolean = builtIns.any { it.id == id }

        val builtIns: List<FormattingProfile> = listOf(
            FormattingProfile(
                id = "default",
                name = "Default",
                description = "Natural sentences for everyday dictation"
            ),
            FormattingProfile(
                id = "work",
                name = "Work",
                description = "More formal wording for messages and reports",
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
                description = "Fast bullet-style notes without forced punctuation",
                bulletPrefix = true,
                addPunctuation = false
            )
        )
    }

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _profiles = MutableStateFlow(loadAll())
    val profiles: StateFlow<List<FormattingProfile>> = _profiles.asStateFlow()

    init {
        currentInstance = this
    }

    fun getProfile(id: String): FormattingProfile =
        _profiles.value.firstOrNull { it.id == id } ?: builtInProfile(id)

    fun createProfile(): FormattingProfile {
        val customCount = _profiles.value.count { !isBuiltIn(it.id) }
        require(customCount < MAX_CUSTOM_PROFILES) {
            "You can create up to $MAX_CUSTOM_PROFILES custom profiles."
        }

        val profile = FormattingProfile(
            id = "custom-${UUID.randomUUID()}",
            name = "Custom profile",
            description = "Your own formatting rules"
        )
        save(profile)
        return profile
    }

    fun save(profile: FormattingProfile) {
        val sanitized = sanitize(profile)
        preferences.edit()
            .putString(profileKey(sanitized.id), encode(sanitized).toString())
            .apply()

        if (!isBuiltIn(sanitized.id)) {
            val customIds = readCustomIds().toMutableList()
            if (sanitized.id !in customIds) {
                require(customIds.size < MAX_CUSTOM_PROFILES) {
                    "You can create up to $MAX_CUSTOM_PROFILES custom profiles."
                }
                customIds += sanitized.id
                writeCustomIds(customIds)
            }
        }
        _profiles.value = loadAll()
    }

    fun reset(profileId: String) {
        if (isBuiltIn(profileId)) {
            preferences.edit().remove(profileKey(profileId)).apply()
            _profiles.value = loadAll()
        }
    }

    fun delete(profileId: String): Boolean {
        if (isBuiltIn(profileId)) return false
        val customIds = readCustomIds().filterNot { it == profileId }
        preferences.edit()
            .remove(profileKey(profileId))
            .putString(CUSTOM_IDS_KEY, JSONArray(customIds).toString())
            .apply()
        _profiles.value = loadAll()
        return true
    }

    private fun loadAll(): List<FormattingProfile> {
        val builtInProfiles = builtIns.map { fallback ->
            loadProfile(fallback.id, fallback)
        }
        val customProfiles = readCustomIds().mapNotNull { id ->
            val raw = preferences.getString(profileKey(id), null) ?: return@mapNotNull null
            runCatching {
                decode(
                    JSONObject(raw),
                    FormattingProfile(
                        id = id,
                        name = "Custom profile",
                        description = "Your own formatting rules"
                    )
                )
            }.getOrNull()
        }
        return builtInProfiles + customProfiles
    }

    private fun loadProfile(id: String, fallback: FormattingProfile): FormattingProfile {
        val stored = preferences.getString(profileKey(id), null)
        return if (stored.isNullOrBlank()) {
            fallback
        } else {
            runCatching { decode(JSONObject(stored), fallback) }.getOrDefault(fallback)
        }
    }

    private fun sanitize(profile: FormattingProfile): FormattingProfile = profile.copy(
        name = profile.name.trim().take(48).ifBlank { "Profile" },
        description = profile.description.trim().take(140),
        prefix = profile.prefix.take(80),
        suffix = profile.suffix.take(80),
        customWords = profile.customWords
            .asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase() }
            .take(MAX_CUSTOM_WORDS)
            .toCollection(linkedSetOf()),
        replacements = profile.replacements.entries
            .asSequence()
            .map { it.key.trim() to it.value.trim() }
            .filter { it.first.isNotBlank() && it.second.isNotBlank() }
            .distinctBy { it.first.lowercase() }
            .take(MAX_REPLACEMENTS)
            .associateTo(linkedMapOf()) { it }
    )

    private fun profileKey(id: String): String = "profile_$id"

    private fun readCustomIds(): List<String> {
        val raw = preferences.getString(CUSTOM_IDS_KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    array.optString(index)
                        .takeIf { it.startsWith("custom-") }
                        ?.let(::add)
                }
            }.distinct().take(MAX_CUSTOM_PROFILES)
        }.getOrDefault(emptyList())
    }

    private fun writeCustomIds(ids: List<String>) {
        preferences.edit().putString(CUSTOM_IDS_KEY, JSONArray(ids).toString()).apply()
    }

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
            profile.customWords.forEach(::put)
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

        return sanitize(
            fallback.copy(
                id = fallback.id,
                name = json.optString("name", fallback.name),
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
        )
    }
}
