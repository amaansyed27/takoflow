import sys

with open("app/src/main/java/com/example/ui/screens/VoiceDictationScreen.kt", "r") as f:
    content = f.read()

# Add isWhisperInstalled state
state = """    val vibration by preferences.vibrationFeedback.collectAsState(initial = false)
    val isWhisperInstalled by preferences.isWhisperInstalled.collectAsState(initial = false)"""
content = content.replace("    val vibration by preferences.vibrationFeedback.collectAsState(initial = false)", state)

# Add to LaunchedEffect
effect_replace = """        speechEngine.soundFeedbackEnabled = sound
        speechEngine.vibrationFeedbackEnabled = vibration
        speechEngine.activeProfile = profileId
        speechEngine.isWhisperInstalled = isWhisperInstalled"""
content = content.replace("""        speechEngine.soundFeedbackEnabled = sound
        speechEngine.vibrationFeedbackEnabled = vibration
        speechEngine.activeProfile = profileId""", effect_replace)

effect_key = """    LaunchedEffect(model, language, punctuation, autoCaps, sound, vibration, profileId, isWhisperInstalled) {"""
content = content.replace("    LaunchedEffect(model, language, punctuation, autoCaps, sound, vibration, profileId) {", effect_key)

with open("app/src/main/java/com/example/ui/screens/VoiceDictationScreen.kt", "w") as f:
    f.write(content)
