# 🐾 OpenPaw – AI Agent for Android

An intelligent AI agent that runs on your Android phone. Talk to it, and it controls your device.

## What it can do

| Tool | What happens |
|------|-------------|
| `send_whatsapp` | Opens WhatsApp with pre-filled message |
| `create_calendar_event` | Opens calendar with event pre-filled |
| `set_alarm` | Sets alarm or countdown timer directly |
| `open_app` | Launches any installed app by name |
| `manage_memory` | Stores/recalls facts across conversations |

## Quick Start

### 1. Add your API key

Edit `local.properties` (never commit this!):
```
ANTHROPIC_API_KEY=sk-ant-api03-YOUR_KEY_HERE
```
Or add it in the app under **Settings → Anthropic API Key**.

### 2. Build & install

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. Talk to it

```
You: Send a WhatsApp to Mom that I'll be home at 7
OpenPaw: Opens WhatsApp with pre-filled message ✓

You: Set an alarm for 6:30 tomorrow
OpenPaw: Alarm set for 06:30 ✓

You: Remember that my favorite coffee is Oat Latte
OpenPaw: Remembered: favorite_coffee = Oat Latte ✓
```

## Architecture

```
app/
├── data/
│   ├── local/        # Room database (messages, memories)
│   ├── remote/       # Anthropic API client (Retrofit)
│   └── repository/   # MemoryRepository, SettingsRepository
├── domain/
│   ├── tools/        # Tool implementations + ToolRegistry
│   └── usecase/      # AgentUseCase (LLM + tool execution loop)
├── presentation/
│   ├── chat/         # ChatScreen + ChatViewModel
│   └── settings/     # SettingsScreen + SettingsViewModel
└── di/               # Hilt modules
```

## Tech Stack

- **Kotlin** + Jetpack Compose
- **Hilt** (Dependency Injection)
- **Room** (SQLite for message history + memory)
- **Retrofit** + OkHttp (Anthropic API)
- **DataStore** (API key storage)
- **Anthropic Claude** (claude-haiku-4-5 by default)

## Permissions required

- `INTERNET` – for Claude API
- `READ_SMS` – read SMS messages (optional)
- `READ_CALENDAR` / `WRITE_CALENDAR` – calendar events
- `SET_ALARM` – alarms & timers
- `READ_CONTACTS` – contact lookup (optional)

## Adding new tools

1. Create a class implementing `Tool` in `domain/tools/`
2. Add `@Inject constructor` + `@Singleton`
3. Register it in `ToolRegistry`

That's it – Hilt wires everything automatically.

## Roadmap

- [ ] Voice input (STT integration)
- [ ] SMS reading tool
- [ ] Contact search tool
- [ ] Screenshot analysis (Accessibility Service)
- [ ] Local LLM support (Gemini Nano / llama.cpp)
- [ ] Widget for quick access
- [ ] Multi-step automation chains
