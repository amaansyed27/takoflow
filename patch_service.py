import sys

with open("app/src/main/java/com/example/service/TakoFlowInputMethodService.kt", "r") as f:
    content = f.read()

# Add states inside setContent
states = """                    val mode by preferences.keyboardMode.collectAsState(initial = "Voice Only Mode")
                    val model by preferences.inferenceModel.collectAsState(initial = "Vosk")
                    val language by preferences.language.collectAsState(initial = "English (US)")
                    val punctuation by preferences.punctuation.collectAsState(initial = true)
                    val autoCaps by preferences.autoCapitalization.collectAsState(initial = true)
                    val sound by preferences.soundFeedback.collectAsState(initial = true)
                    val vibration by preferences.vibrationFeedback.collectAsState(initial = false)
                    val profileId by preferences.activeProfileId.collectAsState(initial = "default")
                    val isWhisperInstalled by preferences.isWhisperInstalled.collectAsState(initial = false)
                    
                    LaunchedEffect(model, language, punctuation, autoCaps, sound, vibration, profileId, isWhisperInstalled) {
                        speechEngine.activeModel = model
                        speechEngine.activeLanguage = language
                        speechEngine.autoPunctuation = punctuation
                        speechEngine.autoCapitalization = autoCaps
                        speechEngine.soundFeedbackEnabled = sound
                        speechEngine.vibrationFeedbackEnabled = vibration
                        speechEngine.activeProfile = profileId
                        speechEngine.isWhisperInstalled = isWhisperInstalled
                    }"""

content = content.replace('                    val mode by preferences.keyboardMode.collectAsState(initial = "Voice Only Mode")', states)

with open("app/src/main/java/com/example/service/TakoFlowInputMethodService.kt", "w") as f:
    f.write(content)
