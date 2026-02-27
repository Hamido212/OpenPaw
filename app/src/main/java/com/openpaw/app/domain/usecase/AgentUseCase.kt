package com.openpaw.app.domain.usecase

import com.openpaw.app.data.local.MessageDao
import com.openpaw.app.data.model.Message
import com.openpaw.app.data.model.MessageRole
import com.openpaw.app.data.remote.LlmProvider
import com.openpaw.app.data.remote.ToolResultEntry
import com.openpaw.app.data.remote.dto.ApiMessage
import com.openpaw.app.data.repository.MemoryRepository
import com.openpaw.app.data.repository.SettingsRepository
import com.openpaw.app.domain.tools.ToolRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

sealed class AgentEvent {
    data class Thinking(val text: String) : AgentEvent()
    data class ToolCall(val toolName: String, val input: Map<String, Any>) : AgentEvent()
    data class ToolResult(val toolName: String, val output: String, val success: Boolean) : AgentEvent()
    data class FinalResponse(val text: String) : AgentEvent()
    data class Error(val message: String) : AgentEvent()
}

@Singleton
class AgentUseCase @Inject constructor(
    private val llmProvider: LlmProvider,
    private val toolRegistry: ToolRegistry,
    private val messageDao: MessageDao,
    private val memoryRepository: MemoryRepository,
    private val settingsRepository: SettingsRepository
) {
    /** Maximum number of LLM ↔ tool-execution cycles per user message. */
    private val maxIterations = 10

    /** Build the system prompt dynamically using the user's agent settings. */
    private suspend fun buildSystemPrompt(): String {
        val agentName    = settingsRepository.agentName.first().ifBlank { "OpenPaw" }
        val agentEmoji   = settingsRepository.agentEmoji.first().ifBlank { "🐾" }
        val personality  = settingsRepository.agentPersonality.first()
        val userName     = settingsRepository.userName.first().trim()
        val userBio      = settingsRepository.userBio.first().trim()

        val personalityLine = when (personality) {
            "professionell" -> "Du bist professionell, präzise und sachlich. Keine unnötigen Floskeln."
            "witzig"        -> "Du bist locker und humorvoll – du nutzt gelegentlich Wortspiele oder Emojis."
            "direkt"        -> "Du bist maximal direkt und kurz. Keine langen Erklärungen, nur das Wesentliche."
            else            -> "Du bist freundlich, warm und hilfsbereit."   // "freundlich"
        }

        val userContext = buildString {
            if (userName.isNotBlank() || userBio.isNotBlank()) {
                append("\n\n— Nutzer-Kontext —")
                if (userName.isNotBlank()) append("\nName: $userName")
                if (userBio.isNotBlank())  append("\nÜber den Nutzer: $userBio")
            }
        }

        return """
            Du bist $agentEmoji $agentName, ein intelligenter KI-Agent der direkt auf einem Android-Handy läuft.
            $personalityLine

            DEINE TOOLS (nutze sie aktiv!):
            - control_screen: Bildschirm lesen (action=read), Element klicken (action=click, text=<Text>),
              Text tippen (action=input, text=...), scrollen (action=scroll, direction=down/up),
              wischen (action=swipe), Home-Taste (action=home), Zurück (action=back)
            - open_app: App starten – app_name="Spotify" / "TikTok" / "Wecker" / "Uhr" / "Maps" usw.
            - send_whatsapp: WhatsApp mit Nachricht öffnen – phone="+49...", message="..."
            - sms: SMS senden (action=send, phone, message) oder Posteingang lesen (action=read)
            - create_calendar_event: Event anlegen – title, start_time (ISO-8601), end_time, description
            - set_alarm: Alarm (hour, minutes, message) oder Timer (timer_seconds)
            - manage_memory: Infos speichern (action=save, key, value) / lesen (action=get, key)
            - file_manager: Dateien lesen/schreiben/auflisten/teilen
            - clipboard: Text kopieren (action=copy, text) oder einfügen (action=paste)

            ══ WICHTIGE REGELN FÜR GERÄTEAKTIONEN ══
            Du läufst INNERHALB der OpenPaw App. Wenn der Nutzer eine Geräteaktion will:
            1. SOFORT control_screen(action=home) ausführen → verlässt OpenPaw, geht zum Homescreen
            2. DIREKT danach die eigentliche Aufgabe ausführen – open_app oder click, KEIN read dazwischen
            3. NIEMALS den OpenPaw Chat-Screen lesen – der ist irrelevant für Geräteaufgaben
            4. control_screen(action=read) NUR nutzen wenn du wirklich nicht weißt was auf dem Screen steht

            ══ TEXT IN APPS EINGEBEN (Notizen, Keep, Docs usw.) ══
            Wenn du langen Text in eine App einfügen sollst (Rezept, Liste, Notiz):
            1. clipboard(action=copy, text=<VOLLSTÄNDIGER TEXT>) – Text in Zwischenablage kopieren
            2. control_screen(action=home) – zum Homescreen
            3. open_app(app_name="Notizen") – App öffnen
            4. control_screen(action=read) – einmal lesen um "Neue Notiz" / "+" Button zu finden
            5. control_screen(action=click, query="+") ODER click auf "Neue Notiz" / "Neu"
            6. control_screen(action=input, text=<TITEL>) – Titel eintippen (kurz)
            7. control_screen(action=tap, x=540, y=700) – in den Textbereich tippen um Fokus zu setzen
            8. control_screen(action=input, text=" ") – leeres Leerzeichen um Paste-Menu zu triggern ODER
               clipboard(action=paste) – Text einfügen
            WICHTIG: Niemals versuchen langen Text Zeichen für Zeichen zu tippen – immer clipboard nutzen!

            EFFIZIENZ-REGELN (halte die Schritte minimal!):
            - Verwende open_app statt manuell zum Launcher zu navigieren
            - Lese den Screen NICHT nach jeder Aktion – nur wenn unbedingt nötig
            - Fasse mehrere Schritte zusammen wo möglich
            - Bei Notizen-Apps: immer clipboard als Brücke nutzen, nie langen Text mit input tippen

            ══ SELBST-LERNEN (WICHTIG!) ══
            Du lernst aus deinen Fehlern und merkst dir was funktioniert:
            - Wenn ein Ansatz fehlschlägt, analysiere WARUM und versuche sofort eine Alternative
            - Wenn du nach einem Fehler eine funktionierende Methode findest, speichere sie:
              manage_memory(action=save, key="learn_<app>_<aufgabe>", value="<was funktioniert hat>")
            - Beim nächsten Mal: manage_memory(action=get) lesen um direkt den richtigen Weg zu nehmen
            - Beispiel: click auf "+" hat nicht funktioniert, tap auf Koordinaten hat geklappt →
              speichere: key="learn_notizen_neue_notiz", value="tap bei x=950,y=200 statt click auf +"

            ALLGEMEINE REGELN:
            - Aufgaben VOLLSTÄNDIG ausführen – nicht nur antworten, sondern HANDELN
            - Bei mehrstufigen Aufgaben: jeden Schritt mit dem passenden Tool ausführen
            - Kurz nachfragen, BEVOR Nachrichten gesendet oder irrev. Aktionen ausgeführt werden
            - manage_memory für Nutzerpräferenzen und gelernte Methoden nutzen
            - Auf Deutsch antworten (oder Englisch wenn Nutzer Englisch schreibt)$userContext
        """.trimIndent()
    }

    fun processMessage(
        userInput: String,
        sessionId: String
    ): Flow<AgentEvent> = flow {

        // ── 1. Save user message ───────────────────────────────────────────────
        messageDao.insert(
            Message(sessionId = sessionId, role = MessageRole.USER, content = userInput)
        )

        // ── 2. Build system prompt (includes agent personality + user context) ─
        val memoryContext    = memoryRepository.buildMemoryContext()
        val fullSystemPrompt = buildSystemPrompt() + memoryContext

        // ── 3. Build initial conversation history from DB ──────────────────────
        // Cap at last 20 user/assistant exchanges to keep token count low
        val dbHistory = messageDao.getMessagesForSessionSync(sessionId)
        val conversationMessages: MutableList<ApiMessage> = dbHistory
            .filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
            .takeLast(40)
            .map { msg ->
                ApiMessage(
                    role    = if (msg.role == MessageRole.USER) "user" else "assistant",
                    content = msg.content
                )
            }
            .toMutableList()

        emit(AgentEvent.Thinking("Thinking..."))

        try {
            var iterations = 0
            var consecutiveFailures = 0   // tracks how many iterations in a row had failures
            var hadFailureLastIteration = false

            // ── 4. Agent loop ──────────────────────────────────────────────────
            while (iterations < maxIterations) {
                iterations++

                // 4a. Call the LLM
                val response = llmProvider.complete(
                    messages     = conversationMessages,
                    systemPrompt = fullSystemPrompt,
                    tools        = toolRegistry.toApiTools()
                )

                // 4b. No tool calls → final text response
                if (response.toolCalls.isEmpty()) {
                    val finalText = response.textContent?.trim() ?: ""
                    if (finalText.isNotBlank()) {
                        messageDao.insert(
                            Message(
                                sessionId = sessionId,
                                role      = MessageRole.ASSISTANT,
                                content   = finalText
                            )
                        )
                        emit(AgentEvent.FinalResponse(finalText))
                    }
                    break
                }

                // 4c. Execute all requested tool calls
                val toolResultEntries = mutableListOf<ToolResultEntry>()

                for (toolCall in response.toolCalls) {
                    emit(AgentEvent.ToolCall(toolCall.toolName, toolCall.input))

                    val result = toolRegistry.execute(toolCall.toolName, toolCall.input)
                    emit(AgentEvent.ToolResult(toolCall.toolName, result.output, result.success))

                    // Persist tool result to DB (shown in the UI as a tool chip)
                    messageDao.insert(
                        Message(
                            sessionId = sessionId,
                            role      = MessageRole.TOOL,
                            content   = "${toolCall.toolName}: ${result.output}",
                            toolName  = toolCall.toolName
                        )
                    )

                    toolResultEntries += ToolResultEntry(
                        toolCallId = toolCall.id,
                        toolName   = toolCall.toolName,
                        content    = result.output,
                        isError    = !result.success
                    )
                }

                // 4d. Build provider-specific continuation messages and loop
                val continuationMsgs = llmProvider.buildContinuationMessages(
                    response    = response,
                    toolResults = toolResultEntries
                )
                conversationMessages.addAll(continuationMsgs)

                // ── 4e. ReAct self-reflection on failure ───────────────────────
                val failedTools = toolResultEntries.filter { it.isError }
                val allSucceeded = failedTools.isEmpty()

                if (allSucceeded) {
                    // If previous iteration had failures but this one succeeded → recovered!
                    // Encourage the agent to save the working approach to memory.
                    if (hadFailureLastIteration) {
                        conversationMessages.add(
                            ApiMessage(
                                role    = "user",
                                content = "✓ Das hat jetzt funktioniert! Bitte speichere die erfolgreiche " +
                                    "Methode mit manage_memory(action=save), damit du sie beim nächsten " +
                                    "Mal direkt nutzen kannst. Dann mach weiter mit der Aufgabe."
                            )
                        )
                    }
                    consecutiveFailures = 0
                    hadFailureLastIteration = false
                } else {
                    consecutiveFailures++
                    hadFailureLastIteration = true
                    val failedNames = failedTools.joinToString(", ") { it.toolName }

                    // Build a reflection hint that escalates with repeated failures
                    val reflectionHint = buildString {
                        append("🔁 SELBST-REFLEKTION: Die folgenden Tools sind fehlgeschlagen: $failedNames.\n")
                        if (consecutiveFailures >= 3) {
                            append("Du hast jetzt $consecutiveFailures Mal in Folge Fehler gehabt. " +
                                "Wähle einen KOMPLETT anderen Ansatz:\n")
                        } else {
                            append("Analysiere kurz warum und probiere einen anderen Weg:\n")
                        }
                        append("- control_screen click fehlgeschlagen? → tap mit Koordinaten versuchen\n")
                        append("- control_screen input fehlgeschlagen? → clipboard(copy) + clipboard(paste)\n")
                        append("- open_app fehlgeschlagen? → control_screen(home) + manuell auf Launcher klicken\n")
                        append("- scroll fehlgeschlagen? → swipe versuchen\n")
                        append("Erkläre in einem Satz was du anders machen wirst, dann führe es aus.")
                    }

                    conversationMessages.add(
                        ApiMessage(role = "user", content = reflectionHint)
                    )
                }
            }

            // Guard against hitting the iteration limit
            if (iterations >= maxIterations) {
                val limitMsg = "Aufgabe benötigte zu viele Schritte. Bitte vereinfache die Anfrage."
                messageDao.insert(
                    Message(sessionId = sessionId, role = MessageRole.ASSISTANT, content = limitMsg)
                )
                emit(AgentEvent.Error(limitMsg))
            }

        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("401") == true  -> "API-Schlüssel ungültig. Bitte in Einstellungen prüfen."
                e.message?.contains("429") == true  -> "Zu viele Anfragen – bitte kurz warten."
                e is java.net.UnknownHostException  -> "Keine Internetverbindung."
                else                                -> "Fehler: ${e.message}"
            }
            messageDao.insert(
                Message(sessionId = sessionId, role = MessageRole.ASSISTANT, content = errorMsg)
            )
            emit(AgentEvent.Error(errorMsg))
        }
    }
}
