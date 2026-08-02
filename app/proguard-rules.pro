# JNI method names are resolved by whisper_jni.cpp and must not be renamed.
-keep class com.example.speech.WhisperBridge { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Vosk's reflection-facing native bindings.
-keep class org.vosk.** { *; }
-dontwarn org.vosk.**
