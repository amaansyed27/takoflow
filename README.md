# TakoFlow

TakoFlow is a private, voice-first Android input method from Dawnlight Labs. It handles local dictation while Samsung Keyboard, Gboard or another installed keyboard remains available for normal typing.

## What works

- Real Android `InputMethodService` usable in other apps
- One-tap switching back to the previously used typing keyboard
- Vosk as the default streaming offline engine
- Optional Whisper Tiny through an optimized `whisper.cpp` Android JNI bridge
- Live partial Vosk text, recording timer, waveform feedback and animated processing states
- Password-field protection: voice capture is disabled for sensitive fields
- Backspace, space and editor-aware enter/action controls
- Real microphone, enabled-IME, selected-IME and model-installation checks
- Resumable user flow for setup, permission repair and keyboard switching guidance
- Download, progress, cancellation, validation and removal for local speech models
- Editable formatting profiles with phrase replacements, preferred spellings, prefixes, suffixes, bullets, capitalization and punctuation
- Companion app dictation testing with copy, share and clear actions
- Local-only speech processing; network access is used only for user-initiated model downloads

## Architecture

The companion app follows MVVM with explicit dependency boundaries:

```text
Compose UI / navigation
        ↓
ViewModels + immutable UI state
        ↓
Repositories
        ↓
DataStore / profile storage / Android status / model manager
        ↓
Vosk / whisper.cpp / Android platform APIs
```

`TakoFlowAppContainer` is the composition root. It creates and owns application-scoped repositories, while each ViewModel receives only the dependencies it needs. Compose screens render `UiState`, forward user actions to ViewModels, and keep Android UI side effects such as permission launchers and system pickers at the UI boundary.

The IME is intentionally service/controller based rather than forcing an `InputMethodService` into MVVM. It uses the same app container, repositories and speech engine as the companion app, so there is one settings/model/profile source of truth.

New features should preserve these rules:

- do not access DataStore, model storage or profile persistence directly from Compose screens
- keep long-running work and mutable product state outside composables
- expose observable state with `Flow`/`StateFlow`
- inject only required dependencies into ViewModels/controllers
- keep Android framework side effects at lifecycle-aware UI/service boundaries
- prefer focused files and reusable components over feature-wide monoliths
- add tests around derived state and domain behavior when adding new logic

## Speech engines

### Vosk

The setup flow downloads `vosk-model-small-en-us-0.15` and uses it for low-latency streaming English dictation.

### Whisper Tiny

Whisper Tiny downloads `ggml-tiny.en.bin` and runs locally through the pinned `whisper.cpp` native build. The model is prepared while recording and uses mobile-oriented low-latency decoding settings.

## Build

Requirements:

- Android Studio with JDK 17
- Android SDK 36
- Android NDK `28.2.13676358`
- CMake `3.22.1`
- Internet access during the first native build so CMake can fetch the pinned `whisper.cpp` source

Build and install on a connected device:

```powershell
.\gradlew.bat :app:installDebug --no-daemon
```

The supported native ABIs are `arm64-v8a` and `x86_64`.

Release signing uses:

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

## Privacy

Downloaded models, formatting rules and audio processing remain inside the app sandbox. TakoFlow excludes app data from Android backup and does not enable voice capture in password fields.
