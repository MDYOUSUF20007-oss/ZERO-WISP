# Zero Wisp

A 100% free, 100% offline Android voice-to-text floating bubble app powered by [whisper.cpp](https://github.com/ggml-org/whisper.cpp).

## Features
- 🎙️ Floating bubble with live mic animation
- 🔇 100% offline — no cloud, no API keys, no analytics
- 🌐 Multilingual (Bangla, Hindi, Arabic, English + more)
- 📋 Auto-paste into any focused text field via Accessibility Service
- ⚡ Pause-based chunking for 1000+ word dictations
- 🌙 Pitch-black AMOLED UI

## Supported Models
| Model | Size | Best For |
|---|---|---|
| ggml-tiny.en-q5_1 | ~32 MB | English fast |
| ggml-base.en-q5_1 | ~57 MB | English balanced |
| ggml-base-q5_1 | ~60 MB | Multilingual fast |
| ggml-small-q5_1 | ~190 MB | Bangla/Hindi/Pro |

## Building
1. Clone this repo
2. The GitHub Actions CI will clone whisper.cpp, build the NDK layer, and produce a signed APK automatically on push.
3. Or build locally: ensure NDK 27.x and CMake 3.22+ are installed, then:
   ```bash
   git clone --depth 1 https://github.com/ggml-org/whisper.cpp app/src/main/cpp/whisper.cpp
   ./gradlew assembleDebug
   ```

## Architecture
- **WhisperEngine.kt** — JNI bridge to libzerowisp_jni.so
- **AudioRecorder.kt** — 16kHz PCM capture with pause-based chunking
- **TextCleaner.kt** — Filler word removal and transcript cleanup
- **FloatingBubbleService.kt** — WindowManager overlay bubble
- **WispAccessibilityService.kt** — Cross-app text field paste
- **ModelDownloadService.kt** — OkHttp streaming download with progress

## Privacy
Zero Wisp processes all audio locally on device. No data leaves your phone.
