import sys

with open("app/src/main/java/com/example/speech/LocalSpeechEngine.kt", "r") as f:
    content = f.read()

content = content.replace("private var isWhisperInstalled = false", "var isWhisperInstalled: Boolean = false")

with open("app/src/main/java/com/example/speech/LocalSpeechEngine.kt", "w") as f:
    f.write(content)
