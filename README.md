<div align="center">

<img src="docs/banner.png" alt="OpenPaw Banner" width="100%">

# 🐾 OpenPaw

**The open-source AI agent that runs directly on your Android phone.**

Voice or text — OpenPaw reads your screen, taps buttons, opens apps, and executes tasks across your entire device, even while another app is in the foreground.

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build](https://img.shields.io/badge/Build-Passing-brightgreen)](https://github.com/Hamido212/OpenPaw)

</div>

---

## What is OpenPaw?

OpenPaw turns any Android phone into a voice-first AI assistant that can **actually do things** — not just answer questions. Using Android's Accessibility API, it gains eyes and hands across your entire device. Tap the floating 🐾 bubble from anywhere, speak your request, and watch it happen.

> **"Play my liked songs on Spotify"** → OpenPaw opens Spotify, navigates to Liked Songs, hits Play.
> **"Send a WhatsApp to Mom: I'll be home by 7"** → WhatsApp opens with the message pre-filled.
> **"Set a timer for 20 minutes"** → Done, no app switching needed.

---

## Features

### 🎤 Voice-First Interaction
- **One-tap to talk** — microphone button in the chat bar
- Uses Android's native SpeechRecognizer (Google STT, no extra API key)
- **Text-to-Speech** responses with the highest-quality offline voice available
- Automatically prefers Google TTS engine for natural-sounding output

### 🫧 Floating Bubble — Control Your Phone Hands-Free
- A draggable 🐾 overlay that floats **over every app**
- **Tap once** → voice interaction starts directly in the overlay (no app switch)
- The bubble shows real-time status: 🔵 Idle → 🔴 Listening → 🟠 Processing
- Response appears in a minimal overlay card and is read aloud via TTS
- Enable: *Settings → Floating Bubble → Grant permission → Start*

### 🖥️ Full Screen Control via AccessibilityService
The AI can see and interact with your entire screen — in any app:

| Action | Description |
|--------|-------------|
| `read` | Dump all visible text and interactive elements |
| `click` | Click any button or link by its label text |
| `input` | Type text into any field |
| `scroll` | Scroll up / down / left / right |
| `swipe` | Swipe gestures (TikTok, carousels, etc.) |
| `tap` | Tap at exact pixel coordinates |
| `back / home / recents` | System navigation keys |

### 🤖 Multi-Provider LLM Support
Switch providers at runtime — no restart required:

| Provider | Models | Notes |
|----------|--------|-------|
| **Anthropic Claude** | claude-haiku-4-5, sonnet-4-6, opus-4-6 | Direct API, streaming |
| **Azure AI Foundry** | Kimi-K2.5, GPT-4o, Phi-4, and more | Auto-detects Foundry vs. Classic |
| **Azure OpenAI (Classic)** | GPT-4, GPT-4o | Standard OpenAI-compatible endpoint |
| **Local LLM** *(coming soon)* | Gemini Nano, llama.cpp | 100% on-device, no API key |

### ⚡ Quick Settings Tile
- 🐾 tile in your notification shade
- One tap from the lock screen → app opens with voice input already active

### 🧠 Persistent Memory
The agent remembers facts about you across sessions — name, preferences, routines — using a key/value memory store.

---

## Built-in Tools

| Tool | What It Does |
|------|-------------|
| `control_screen` | Read screen, click, type, scroll, swipe, navigate |
| `open_app` | Launch any app by name (Spotify, Maps, Instagram…) |
| `send_whatsapp` | Open WhatsApp with a pre-filled message |
| `sms` | Send or read SMS messages |
| `create_calendar_event` | Add calendar events (ISO 8601 time support) |
| `set_alarm` | Set alarms or countdown timers |
| `manage_memory` | Save and recall persistent facts about the user |
| `file_manager` | Read, write, list, and share files |
| `clipboard` | Copy text to clipboard or read clipboard content |

---

## Quick Start

### 1. Get an API Key

**Anthropic Claude** — [console.anthropic.com](https://console.anthropic.com)
```
sk-ant-api03-...
```

**Azure AI Foundry** — [ai.azure.com](https://ai.azure.com)
```
Endpoint:    https://YOUR-RESOURCE.services.ai.azure.com
Deployment:  Kimi-K2.5   (or any deployed model)
API Key:     Azure Portal → Keys and Endpoints
```

### 2. Build and Install

Prerequisites: Android Studio with JDK 17+ and Android SDK

```bash
# Clone the repo
git clone https://github.com/Hamido212/OpenPaw.git
cd OpenPaw

# Build the debug APK
./gradlew assembleDebug

# Install on a connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. One-Time Setup (Recommended)

Open the app and go to **Settings** to enable the following:

```
Settings → AI Provider        → Enter your API key
Settings → Screen Control     → Enable Accessibility Service
Settings → Background Agent   → Start Foreground Service
Settings → Floating Bubble    → Grant "Draw over apps" permission → Start
```

### 4. Start Talking

```
You: "What's on my screen right now?"
OpenPaw: Reads the current screen content aloud ✓

You: "Open Spotify and play my liked songs"
OpenPaw: Opens Spotify → navigates to Liked Songs → hits Play ✓

You: "Send a WhatsApp to +49123456789: On my way!"
OpenPaw: WhatsApp opens with message pre-filled ✓

You: "Save the recipe from this video to a file"
OpenPaw: Reads screen → saves content to recipes.txt ✓
```

---

## Architecture

```
app/
├── data/
│   ├── local/              # Room DB — message history & memory store
│   ├── remote/             # LLM provider abstraction + implementations
│   │   ├── AnthropicLlmProvider.kt
│   │   ├── AzureOpenAiLlmProvider.kt   # Auto-detects Classic vs. Foundry
│   │   ├── DelegatingLlmProvider.kt    # Runtime provider switching
│   │   └── LocalLlmProvider.kt         # Stub (coming soon)
│   └── repository/         # MemoryRepository, SettingsRepository
├── domain/
│   ├── tools/              # Tool implementations + ToolRegistry
│   │   ├── ScreenTool.kt           # Accessibility Service wrapper
│   │   ├── WhatsAppTool.kt
│   │   ├── SmsTool.kt
│   │   ├── CalendarTool.kt
│   │   ├── AlarmTool.kt
│   │   ├── OpenAppTool.kt
│   │   ├── MemoryTool.kt
│   │   ├── FileManagerTool.kt
│   │   └── ClipboardTool.kt
│   └── usecase/            # AgentUseCase — LLM ↔ tool execution loop
├── presentation/
│   ├── chat/               # ChatScreen + ChatViewModel
│   ├── settings/           # SettingsScreen + SettingsViewModel
│   ├── tile/               # OpenPawQsTile (Quick Settings)
│   └── voice/              # VoiceInputManager (STT + TTS)
├── service/
│   ├── AgentForegroundService.kt       # Keeps the process alive
│   ├── FloatingBubbleService.kt        # Overlay bubble with full voice loop
│   └── OpenPawAccessibilityService.kt  # Screen reader + controller
└── di/                     # Hilt dependency injection modules
```

**Agent Loop** (`AgentUseCase`):
```
User input
    ↓
LLM call (with tool definitions)
    ↓
Tool calls? ──Yes──→ Execute tools → Feed results back to LLM → Loop (max 10×)
    ↓ No
Final response → DB → UI + TTS
```

---

## Tech Stack

| Technology | Purpose |
|------------|---------|
| Kotlin + Jetpack Compose | UI |
| Hilt | Dependency Injection |
| Room | SQLite — message history & memory |
| Retrofit + OkHttp + Gson | LLM API clients |
| DataStore | User settings persistence |
| SpeechRecognizer | Voice input (on-device, no API key) |
| TextToSpeech | Voice output (prefers Google TTS engine) |
| AccessibilityService | Screen reading and control |
| WindowManager Overlay | Floating bubble |
| TileService | Quick Settings tile |

---

## Required Permissions

| Permission | Why |
|-----------|-----|
| `INTERNET` | LLM API calls |
| `RECORD_AUDIO` | Voice input |
| `SYSTEM_ALERT_WINDOW` | Floating bubble overlay |
| `READ_SMS` / `SEND_SMS` | SMS tool |
| `READ_CALENDAR` / `WRITE_CALENDAR` | Calendar tool |
| `SET_ALARM` | Alarm and timer tool |
| `READ_CONTACTS` | Contact lookup (optional) |
| `FOREGROUND_SERVICE` | Background agent + floating bubble |
| `BIND_ACCESSIBILITY_SERVICE` | Screen control |
| `POST_NOTIFICATIONS` | Android 13+ notifications |

---

## Adding a Custom Tool

1. Create a class in `domain/tools/` that implements the `Tool` interface
2. Annotate it with `@Singleton` and inject dependencies via `@Inject constructor`
3. Register it in `ToolRegistry` — Hilt wires everything automatically

```kotlin
@Singleton
class MyTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {
    override val name = "my_tool"
    override val description = "What this tool does"
    override val parameters = mapOf(
        "input" to ToolParameter("string", "The input value")
    )
    override val requiredParameters = listOf("input")

    override suspend fun execute(input: Map<String, Any>): ToolResult {
        val value = input["input"] as? String ?: return ToolResult(false, "Missing input")
        // ... do something
        return ToolResult(true, "Done: $value")
    }
}
```

---

## Roadmap

- [x] Voice input (STT) + output (TTS)
- [x] Floating overlay bubble with full voice loop
- [x] Quick Settings tile
- [x] Full screen control via AccessibilityService
- [x] Background agent (ForegroundService)
- [x] Anthropic Claude support
- [x] Azure OpenAI + Azure AI Foundry support
- [x] WhatsApp, SMS, Calendar, Alarm tools
- [x] File Manager + Clipboard tools
- [x] Persistent memory store
- [x] Multi-session chat history
- [ ] Contacts tool (search by name → phone number)
- [ ] Camera tool (photo + OCR)
- [ ] Location tool (GPS)
- [ ] Web search tool (Brave / Perplexity API)
- [ ] Local LLM support (Gemini Nano / llama.cpp)
- [ ] Multi-step automation builder

---

## License

MIT — free to use, modify, and distribute. See [LICENSE](LICENSE).

---

<div align="center">

Built with ❤️ for the open-source AI community.

**[GitHub](https://github.com/Hamido212/OpenPaw)** · **[Report a Bug](https://github.com/Hamido212/OpenPaw/issues)** · **[Request a Feature](https://github.com/Hamido212/OpenPaw/issues)**

</div>

---
