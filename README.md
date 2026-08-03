# TakoFlow

TakoFlow is a private Android voice-typing keyboard from Dawnlight Labs.
It is a separate product from Takokit; the shared name is branding only.

## Current speech engines

- **Vosk** is the default engine. The setup flow downloads the official lightweight English model (about 40 MB) and uses it for streaming offline dictation.
- **Whisper Tiny** is optional. It can be downloaded or removed from **Settings → Voice engine** and runs locally through `whisper.cpp` after the user stops recording.

TakoFlow never substitutes sample phrases or simulated recognition results. If a model, permission, or keyboard configuration is missing, the app reports that state and directs the user to fix it.

## Features

- Android `InputMethodService` keyboard usable in other apps
- Voice-only layout with microphone, backspace, space and enter
- Full QWERTY layout with voice dictation
- Real checks for keyboard enabled/selected state
- Runtime microphone permission flow
- Download progress and installation checks for speech models
- Local punctuation, capitalization and formatting profiles
- Companion app for setup, model management and dictation testing

## Privacy

Audio is processed on the device. Network access is used only when the user starts a model download. App data and downloaded models are excluded from Android backup.

## Build

Requirements:

- Android Studio with JDK 17
- Android SDK 36
- Android NDK and CMake 3.22.1
- An internet connection during the first native build so CMake can fetch the pinned `whisper.cpp` v1.8.1 source archive

Open the repository in Android Studio, allow Gradle sync to finish, and run the `app` configuration on an ARM64 device or x86_64 emulator.

The debug build does not require a keystore. Release signing is configured through:

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

## Model sources

- Vosk: `vosk-model-small-en-us-0.15`
- Whisper: `ggml-tiny.en.bin`

The initial implementation is English-only. Additional languages should be added only with a matching downloadable model and tested engine configuration.
