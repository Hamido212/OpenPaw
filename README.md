# 🐾 OpenPaw – KI-Agent für Android

Ein intelligenter KI-Agent der direkt auf deinem Android-Handy läuft. Schreib oder sprich mit ihm – er steuert dein Gerät, auch wenn eine andere App geöffnet ist.

---

## Features

### 🎤 Voice Input & TTS
- **Mikrofon-Button** im Chat – kein Tippen nötig
- Android-eigene Speech-to-Text (Google STT, kein API-Key)
- **Sprachausgabe (TTS)**: Agent liest Antworten vor (Toggle in der TopBar)

### 🫧 Floating Bubble
- Schwebende 🐾-Schaltfläche über **allen** Apps
- Draggable – überall hinziehen
- Antippen → OpenPaw öffnet sich + Spracheingabe startet sofort
- Aktivieren: Einstellungen → Floating Bubble → Berechtigung erteilen → Starten

### ⚡ Quick Settings Tile
- 🐾 OpenPaw-Tile im Benachrichtigungsmenü
- Einmal tippen → App öffnet + Spracheingabe startet
- Hinzufügen: Panel → Stift-Symbol → OpenPaw reinziehen

### 🖥️ Screen Control (AccessibilityService)
Liest und steuert den **gesamten Bildschirm** – auch in anderen Apps:

| Aktion | Beschreibung |
|--------|-------------|
| `read` | Liest alles was auf dem Bildschirm steht |
| `click` | Klickt auf Buttons/Links per Text-Suche |
| `input` | Tippt Text in Eingabefelder |
| `scroll` | Scrollt hoch/runter |
| `swipe` | Wischt links/rechts (z.B. TikTok) |
| `back/home/recents` | System-Buttons |

### 🤖 AI Providers (umschaltbar ohne Neustart)

| Provider | Modell | Endpoint |
|----------|--------|----------|
| Anthropic Claude | haiku-4-5 / sonnet-4-6 / opus-4-6 | api.anthropic.com |
| Azure AI Foundry | Kimi-K2.5 / GPT-4o / … | *.services.ai.azure.com |
| Azure OpenAI (Classic) | GPT-4 / … | *.openai.azure.com |
| Local LLM | — | kommt bald (Gemini Nano / llama.cpp) |

---

## Tools

| Tool | Was es macht |
|------|-------------|
| `control_screen` | Bildschirm lesen, tippen, scrollen, wischen |
| `send_whatsapp` | WhatsApp mit vorausgefüllter Nachricht öffnen |
| `sms` | SMS senden oder lesen (klassisch, kein WhatsApp) |
| `create_calendar_event` | Kalender-Event erstellen |
| `set_alarm` | Alarm oder Timer setzen |
| `open_app` | App per Name starten (Spotify, Maps, Instagram…) |
| `manage_memory` | Fakten über dich dauerhaft speichern/abrufen |
| `file_manager` | Dateien lesen, schreiben, auflisten, teilen |
| `clipboard` | Text in Zwischenablage kopieren oder lesen |

---

## Quick Start

### 1. API-Key eintragen

In der App unter **Einstellungen → KI-Anbieter** auswählen und Key eintragen.

**Anthropic:**
```
sk-ant-api03-...
```

**Azure AI Foundry:**
```
Endpoint:    https://DEINE-RESSOURCE.services.ai.azure.com
Deployment:  Kimi-K2.5  (oder anderes Modell)
API Key:     aus Azure Portal → Schlüssel und Endpunkt
```

### 2. Build & installieren

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="/c/Users/DEIN_USER/AppData/Local/Android/Sdk"

./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. Einmalige Einrichtung (empfohlen)

```
Einstellungen → Bildschirm-Steuerung → Aktivieren
Einstellungen → Hintergrund-Agent → Starten
Einstellungen → Floating Bubble → Berechtigung erteilen → Starten
```

### 4. Benutzen

```
Du (Sprache oder Text): "Lies mir vor was auf dem Bildschirm steht"
OpenPaw: Liest TikTok/Instagram/YouTube vor ✓

Du: "Schick eine WhatsApp an Mama: Ich bin um 7 zuhause"
OpenPaw: WhatsApp mit vorausgefüllter Nachricht geöffnet ✓

Du: "Speicher das Rezept aus dem Video in eine Datei"
OpenPaw: Screen gelesen → recipes.txt gespeichert ✓

Du: "Sende eine SMS an +49123456789: Bin gleich da"
OpenPaw: SMS gesendet ✓
```

---

## Architektur

```
app/
├── data/
│   ├── local/          # Room DB (Nachrichtenverlauf, Erinnerungen)
│   ├── remote/         # LlmProvider Interface + Implementierungen
│   │   ├── AnthropicLlmProvider.kt
│   │   ├── AzureOpenAiLlmProvider.kt   # Auto-erkennt Classic vs Foundry
│   │   ├── DelegatingLlmProvider.kt    # Laufzeit-Switching
│   │   └── LocalLlmProvider.kt         # Stub (kommt bald)
│   └── repository/     # MemoryRepository, SettingsRepository
├── domain/
│   ├── tools/          # Tool-Implementierungen + ToolRegistry
│   │   ├── ScreenTool.kt        # AccessibilityService-Wrapper
│   │   ├── WhatsAppTool.kt
│   │   ├── SmsTool.kt
│   │   ├── CalendarTool.kt
│   │   ├── AlarmTool.kt
│   │   ├── OpenAppTool.kt
│   │   ├── MemoryTool.kt
│   │   ├── FileManagerTool.kt
│   │   └── ClipboardTool.kt
│   └── usecase/        # AgentUseCase (LLM + Tool-Execution Loop)
├── presentation/
│   ├── chat/           # ChatScreen + ChatViewModel
│   ├── settings/       # SettingsScreen + SettingsViewModel
│   ├── tile/           # OpenPawQsTile (Quick Settings)
│   └── voice/          # VoiceInputManager (STT + TTS)
├── service/
│   ├── AgentForegroundService.kt      # Hält Prozess am Leben
│   ├── FloatingBubbleService.kt       # Overlay-Blase über allen Apps
│   └── OpenPawAccessibilityService.kt # Screen lesen + steuern
└── di/                 # Hilt Module
```

---

## Tech Stack

| Technologie | Verwendung |
|------------|-----------|
| Kotlin + Jetpack Compose | UI |
| Hilt | Dependency Injection |
| Room | SQLite (Verlauf + Memory) |
| Retrofit + OkHttp + Gson | API-Clients |
| DataStore | Einstellungen |
| SpeechRecognizer | Spracheingabe (System-STT) |
| TextToSpeech | Sprachausgabe (System-TTS) |
| AccessibilityService | Screen lesen + steuern |
| WindowManager Overlay | Floating Bubble |
| TileService | Quick Settings Tile |

---

## Benötigte Berechtigungen

| Permission | Wozu |
|-----------|------|
| `INTERNET` | KI-API-Aufrufe |
| `RECORD_AUDIO` | Spracheingabe (wird beim ersten Tippen gefragt) |
| `SYSTEM_ALERT_WINDOW` | Floating Bubble über anderen Apps |
| `READ_SMS` / `SEND_SMS` | SMS lesen und senden |
| `READ_CALENDAR` / `WRITE_CALENDAR` | Kalender-Events |
| `SET_ALARM` | Alarme und Timer |
| `READ_CONTACTS` | Kontaktsuche (optional) |
| `FOREGROUND_SERVICE` | Hintergrund-Agent + Floating Bubble |
| `BIND_ACCESSIBILITY_SERVICE` | Screen-Control |
| `POST_NOTIFICATIONS` | Notifications (Android 13+) |

---

## Neues Tool hinzufügen

1. Klasse in `domain/tools/` erstellen die `Tool` implementiert
2. `@Singleton` + `@Inject constructor(@ApplicationContext context: Context)`
3. In `ToolRegistry` registrieren (Konstruktor-Parameter + Liste)

Hilt verdrahtet alles automatisch.

---

## Roadmap

- [x] Spracheingabe (STT)
- [x] Sprachausgabe (TTS)
- [x] Floating Bubble
- [x] Quick Settings Tile
- [x] AccessibilityService (Screen lesen + steuern)
- [x] Hintergrund-Agent (ForegroundService)
- [x] Azure OpenAI + Azure AI Foundry
- [x] SMS Tool
- [x] Datei-Manager Tool
- [x] Clipboard Tool
- [ ] Kontakte Tool (nach Name suchen → Telefonnummer)
- [ ] Kamera Tool (Foto + OCR)
- [ ] Standort Tool (GPS)
- [ ] Web-Suche Tool (Brave/Perplexity API)
- [ ] Local LLM (Gemini Nano / llama.cpp)
- [ ] Mehr-Schritt-Automatisierungen
