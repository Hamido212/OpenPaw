package com.openpaw.app.domain.usecase

import com.openpaw.app.data.local.MessageDao
import com.openpaw.app.data.model.Message
import com.openpaw.app.data.model.MessageRole
import com.openpaw.app.data.remote.LlmProvider
import com.openpaw.app.data.remote.dto.ApiMessage
import com.openpaw.app.data.repository.MemoryRepository
import com.openpaw.app.domain.tools.ToolRegistry
import kotlinx.coroutines.flow.Flow
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
    private val llmProvider: LlmProvider,       // ← swappable: Cloud ↔ Local
    private val toolRegistry: ToolRegistry,
    private val messageDao: MessageDao,
    private val memoryRepository: MemoryRepository
) {
    private val systemPrompt = """
        Du bist OpenPaw, ein intelligenter KI-Agent der direkt auf einem Android-Handy läuft.
        Du kannst das Handy steuern, Nachrichten senden, Events erstellen, Alarme setzen,
        den Bildschirm lesen/klicken und wichtige Informationen über den Nutzer merken.

        Deine Fähigkeiten (Tools):
        - control_screen: Lesen was auf dem Bildschirm steht, Elemente klicken, Text eingeben, scrollen
        - send_whatsapp: WhatsApp mit vorausgefüllter Nachricht öffnen
        - create_calendar_event: Kalender-Event erstellen
        - set_alarm: Alarm oder Timer setzen
        - open_app: Apps starten (Spotify, Maps, Instagram usw.)
        - manage_memory: Wichtige Infos über den Nutzer persistent speichern/abrufen

        Grundprinzipien:
        - Antworte präzise und kurz (Deutsch oder Englisch – pass dich dem Nutzer an)
        - Nutze control_screen(action=read) BEVOR du klickst, um zu sehen was sichtbar ist
        - Frage IMMER nach Bestätigung bevor Nachrichten versendet werden
        - Nutze manage_memory um Nutzerpräferenzen dauerhaft zu speichern
        - Sei ehrlich wenn du etwas nicht kannst
    """.trimIndent()

    fun processMessage(
        userInput: String,
        sessionId: String
    ): Flow<AgentEvent> = flow {
        // 1. Nutzernachricht speichern
        messageDao.insert(Message(sessionId = sessionId, role = MessageRole.USER, content = userInput))

        // 2. Memory-Kontext laden
        val memoryContext = memoryRepository.buildMemoryContext()

        // 3. Konversationshistorie für API aufbauen
        val history = messageDao.getMessagesForSessionSync(sessionId)
        val apiMessages = history
            .filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
            .map { msg ->
                ApiMessage(
                    role = if (msg.role == MessageRole.USER) "user" else "assistant",
                    content = msg.content
                )
            }

        emit(AgentEvent.Thinking("Thinking..."))

        try {
            // 4. LLM aufrufen (über abstraktes Interface → Cloud oder Local)
            val response = llmProvider.complete(
                messages = apiMessages,
                systemPrompt = systemPrompt + memoryContext,
                tools = toolRegistry.toApiTools()
            )

            // 5. Tool-Calls ausführen
            var assistantText = response.textContent ?: ""

            for (toolCall in response.toolCalls) {
                emit(AgentEvent.ToolCall(toolCall.toolName, toolCall.input))

                val toolResult = toolRegistry.execute(toolCall.toolName, toolCall.input)
                emit(AgentEvent.ToolResult(toolCall.toolName, toolResult.output, toolResult.success))

                messageDao.insert(
                    Message(
                        sessionId = sessionId,
                        role = MessageRole.TOOL,
                        content = "${toolCall.toolName}: ${toolResult.output}",
                        toolName = toolCall.toolName
                    )
                )

                if (assistantText.isEmpty()) {
                    assistantText = if (toolResult.success) toolResult.output
                    else "Fehler: ${toolResult.output}"
                }
            }

            // 6. Antwort speichern und emittieren
            if (assistantText.isNotBlank()) {
                messageDao.insert(
                    Message(sessionId = sessionId, role = MessageRole.ASSISTANT, content = assistantText)
                )
                emit(AgentEvent.FinalResponse(assistantText))
            }

        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("401") == true -> "API-Schlüssel ungültig. Bitte in Einstellungen prüfen."
                e.message?.contains("429") == true -> "Zu viele Anfragen – kurz warten."
                e is java.net.UnknownHostException -> "Keine Internetverbindung."
                else -> "Fehler: ${e.message}"
            }
            messageDao.insert(Message(sessionId = sessionId, role = MessageRole.ASSISTANT, content = errorMsg))
            emit(AgentEvent.Error(errorMsg))
        }
    }
}
