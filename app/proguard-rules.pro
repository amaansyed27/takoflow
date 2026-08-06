# Preserve JNI entry points and native-backed speech libraries.
-keep class com.example.speech.WhisperBridge { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class org.vosk.** { *; }
-keep class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**

# Preserve useful source information in release crash reports.
-keepattributes SourceFile,LineNumberTable
