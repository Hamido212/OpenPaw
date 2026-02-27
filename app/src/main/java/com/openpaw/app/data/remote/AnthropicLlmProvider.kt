package com.openpaw.app.data.remote

import com.openpaw.app.data.remote.dto.ApiMessage
import com.openpaw.app.data.remote.dto.ApiTool
import com.openpaw.app.data.remote.dto.AnthropicRequest
import com.openpaw.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LlmProvider implementation backed by Anthropic's Claude API.
 * The API key and model are read from SettingsRepository at call time,
 * so changing them in Settings takes effect immediately.
 */
@Singleton
class AnthropicLlmProvider @Inject constructor(
    private val apiService: AnthropicApiService,
    private val settingsRepository: SettingsRepository
) : LlmProvider {

    override val name = "Anthropic Claude"

    override suspend fun complete(
        messages: List<ApiMessage>,
        systemPrompt: String,
        tools: List<ApiTool>
    ): LlmResponse {
        val apiKey = settingsRepository.apiKey.first()
        val model = settingsRepository.llmModel.first()

        val response = apiService.sendMessage(
            apiKey = apiKey,
            request = AnthropicRequest(
                model = model,
                system = systemPrompt,
                messages = messages,
                tools = tools.ifEmpty { null }
            )
        )

        val textContent = response.content
            .filter { it.type == "text" }
            .mapNotNull { it.text }
            .joinToString("\n")
            .ifBlank { null }

        val toolCalls = response.content
            .filter { it.type == "tool_use" }
            .mapNotNull { block ->
                val name = block.name ?: return@mapNotNull null
                ToolCallRequest(
                    id = block.id ?: "",
                    toolName = name,
                    input = block.input ?: emptyMap()
                )
            }

        return LlmResponse(
            textContent = textContent,
            toolCalls = toolCalls,
            stopReason = response.stopReason,
            rawProviderData = response.content   // List<ContentBlock> – needed for continuation
        )
    }

    /**
     * Anthropic continuation format:
     *
     *   [assistant]  content = [text block(s) + tool_use block(s)]  ← original response
     *   [user]       content = [{ type: "tool_result", tool_use_id, content }, ...]
     */
    override suspend fun buildContinuationMessages(
        response: LlmResponse,
        toolResults: List<ToolResultEntry>
    ): List<ApiMessage> {
        // 1. Reconstruct the assistant message with the original content blocks.
        //    rawProviderData holds List<ContentBlock> from the response.
        val rawContent: Any = response.rawProviderData
            ?: response.toolCalls.map { tc ->
                // Fallback: rebuild minimal tool_use blocks if raw data is missing
                mapOf("type" to "tool_use", "id" to tc.id, "name" to tc.toolName, "input" to tc.input)
            }
        val assistantMsg = ApiMessage(role = "assistant", content = rawContent)

        // 2. User message with one tool_result block per tool call.
        val resultBlocks = toolResults.map { entry ->
            mapOf(
                "type"        to "tool_result",
                "tool_use_id" to entry.toolCallId,
                "content"     to entry.content
            )
        }
        val userMsg = ApiMessage(role = "user", content = resultBlocks)

        return listOf(assistantMsg, userMsg)
    }
}
