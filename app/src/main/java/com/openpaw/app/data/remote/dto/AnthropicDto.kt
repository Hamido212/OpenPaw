package com.openpaw.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// ─── Request DTOs ───────────────────────────────────────────────────────────

data class AnthropicRequest(
    val model: String = "claude-haiku-4-5-20251001",
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    val system: String,
    val messages: List<ApiMessage>,
    val tools: List<ApiTool>? = null
)

data class ApiMessage(
    val role: String,   // "user" | "assistant" | "tool"
    val content: Any,   // String or List<Map<String,Any>> (structured Anthropic content blocks)
    // ── Azure-only continuation fields ──────────────────────────────────────
    // These are null for Anthropic messages (Anthropic API ignores unknown fields).
    /** Set on role="tool" messages (Azure) – references the corresponding tool_call id. */
    val toolCallId: String? = null,
    /** Set on role="assistant" when the LLM returned tool calls (Azure).
     *  Carries the raw AzureToolCall list so it can be re-serialized correctly. */
    val azureToolCalls: List<Any>? = null
)

data class ApiTool(
    val name: String,
    val description: String,
    @SerializedName("input_schema") val inputSchema: ApiToolSchema
)

data class ApiToolSchema(
    val type: String = "object",
    val properties: Map<String, ApiToolProperty>,
    val required: List<String> = emptyList()
)

data class ApiToolProperty(
    val type: String,
    val description: String
)

// ─── Response DTOs ──────────────────────────────────────────────────────────

data class AnthropicResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    @SerializedName("stop_reason") val stopReason: String?,
    val usage: Usage
)

data class ContentBlock(
    val type: String,           // "text" | "tool_use"
    val text: String?,          // for type=text
    val id: String?,            // for type=tool_use
    val name: String?,          // for type=tool_use
    val input: Map<String, Any>? // for type=tool_use
)

data class Usage(
    @SerializedName("input_tokens") val inputTokens: Int,
    @SerializedName("output_tokens") val outputTokens: Int
)

data class AnthropicError(
    val type: String,
    val error: ErrorDetail
)

data class ErrorDetail(
    val type: String,
    val message: String
)
