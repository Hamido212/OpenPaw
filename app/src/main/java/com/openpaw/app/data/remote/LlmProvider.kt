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
     * @param messages   Full conversation history (user + assistant turns)
     * @param systemPrompt  System instructions prepended to every call
     * @param tools      List of tools the LLM can call (empty = no tool use)
     * @return [LlmResponse] with text and/or tool call requests
     */
    suspend fun complete(
        messages: List<ApiMessage>,
        systemPrompt: String,
        tools: List<ApiTool> = emptyList()
    ): LlmResponse
}

/**
 * Unified response from any LLM provider.
 */
data class LlmResponse(
    /** Free-text response (may be null if the LLM only returned tool calls). */
    val textContent: String?,
    /** Tool calls requested by the LLM. */
    val toolCalls: List<ToolCallRequest> = emptyList(),
    /** Provider-specific stop reason (e.g. "end_turn", "tool_use"). */
    val stopReason: String? = null
)

data class ToolCallRequest(
    val id: String,
    val toolName: String,
    val input: Map<String, Any>
)
