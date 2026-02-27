package com.openpaw.app.data.remote

import com.openpaw.app.data.remote.dto.ApiMessage
import com.openpaw.app.data.remote.dto.ApiTool

/**
 * Abstraction over any LLM backend (cloud or on-device).
 *
 * To add a new provider (e.g. Gemini Nano, llama.cpp):
 *  1. Implement this interface
 *  2. Create a Hilt binding in di/LlmModule.kt
 *  3. Let user pick it in Settings
 */
interface LlmProvider {

    val name: String

    /**
     * Send a conversation turn to the LLM.
     *
     * @param messages   Full conversation history (user + assistant turns + tool results)
     * @param systemPrompt  System instructions prepended to every call
     * @param tools      List of tools the LLM can call (empty = no tool use)
     * @return [LlmResponse] with text and/or tool call requests
     */
    suspend fun complete(
        messages: List<ApiMessage>,
        systemPrompt: String,
        tools: List<ApiTool> = emptyList()
    ): LlmResponse

    /**
     * Build the provider-specific continuation messages to append after tool execution.
     *
     * After the LLM requests tool calls and the tools are executed, we need to append
     * two (Anthropic) or N+1 (Azure/OpenAI) messages so the LLM can see the results:
     *
     * Anthropic:
     *   1. [assistant] content = original tool_use + text blocks  (from rawProviderData)
     *   2. [user]      content = [{ type: "tool_result", tool_use_id, content }, ...]
     *
     * Azure / OpenAI:
     *   1. [assistant] tool_calls = [{ id, function: { name, arguments } }, ...]
     *   2. [tool]      tool_call_id = ..., content = result   (one per tool call)
     *
     * @param response     The LlmResponse that contained the tool call requests
     * @param toolResults  The results of executing those tool calls
     * @return Messages to append to the conversation before the next LLM call
     */
    suspend fun buildContinuationMessages(
        response: LlmResponse,
        toolResults: List<ToolResultEntry>
    ): List<ApiMessage>
}

/**
 * Unified response from any LLM provider.
 */
data class LlmResponse(
    /** Free-text response (may be null if the LLM only returned tool calls). */
    val textContent: String?,
    /** Tool calls requested by the LLM. */
    val toolCalls: List<ToolCallRequest> = emptyList(),
    /** Provider-specific stop reason (e.g. "end_turn", "tool_use", "tool_calls"). */
    val stopReason: String? = null,
    /**
     * Opaque provider-specific raw data needed to build continuation messages.
     * Anthropic: List<ContentBlock>  (the original content blocks from the response)
     * Azure:     AzureResponseMessage  (the original message object with tool_calls)
     */
    val rawProviderData: Any? = null
)

data class ToolCallRequest(
    val id: String,
    val toolName: String,
    val input: Map<String, Any>
)

/** Result of executing a single tool call – used to build continuation messages. */
data class ToolResultEntry(
    val toolCallId: String,
    val toolName: String,
    val content: String,
    val isError: Boolean = false
)
