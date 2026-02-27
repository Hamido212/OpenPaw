package com.openpaw.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// ─── Request ────────────────────────────────────────────────────────────────

/**
 * Azure chat completion request.
 *
 * [model] is null for Classic Azure OpenAI (model is baked into the deployment URL).
 * [model] must be set to the deployment name for Azure AI Foundry (*.services.ai.azure.com).
 */
data class AzureChatRequest(
    /** Required for Azure AI Foundry endpoints; omit (null) for Classic Azure OpenAI. */
    val model: String? = null,
    val messages: List<AzureChatMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    val temperature: Float = 0.7f,
    /** Optional tool definitions – OpenAI function-calling format. */
    val tools: List<AzureTool>? = null,
    /** "auto" = LLM decides, "none" = never, or specific tool name. */
    @SerializedName("tool_choice") val toolChoice: String? = if (tools.isNullOrEmpty()) null else "auto"
)

data class AzureChatMessage(
    val role: String,       // "system" | "user" | "assistant" | "tool"
    val content: String?,   // null when role="assistant" with tool_calls
    /** Filled when role="assistant" and LLM requested tool calls. */
    @SerializedName("tool_calls") val toolCalls: List<AzureToolCall>? = null,
    /** Filled when role="tool" – references the tool_call id. */
    @SerializedName("tool_call_id") val toolCallId: String? = null
)

data class AzureTool(
    val type: String = "function",
    val function: AzureFunction
)

data class AzureFunction(
    val name: String,
    val description: String,
    val parameters: AzureFunctionParameters
)

data class AzureFunctionParameters(
    val type: String = "object",
    val properties: Map<String, AzureFunctionProperty>,
    val required: List<String> = emptyList()
)

data class AzureFunctionProperty(
    val type: String,
    val description: String
)

// ─── Response ────────────────────────────────────────────────────────────────

data class AzureChatResponse(
    val id: String,
    val `object`: String?,
    val created: Long?,
    val model: String?,
    val choices: List<AzureChoice>,
    val usage: AzureUsage?
)

data class AzureChoice(
    val index: Int,
    val message: AzureResponseMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

data class AzureResponseMessage(
    val role: String,
    /** Text response – null if only tool_calls are present. */
    val content: String?,
    /** Populated when finishReason = "tool_calls". */
    @SerializedName("tool_calls") val toolCalls: List<AzureToolCall>?
)

data class AzureToolCall(
    val id: String,
    val type: String,                   // always "function"
    val function: AzureToolCallFunction
)

/**
 * IMPORTANT: [arguments] is a JSON **string**, not an object.
 * Must be deserialized with Gson before passing to ToolRegistry.
 */
data class AzureToolCallFunction(
    val name: String,
    val arguments: String               // e.g. "{\"city\":\"Berlin\"}"
)

data class AzureUsage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)
