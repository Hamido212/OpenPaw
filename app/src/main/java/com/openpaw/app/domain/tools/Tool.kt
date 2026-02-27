package com.openpaw.app.domain.tools

/**
 * Result of a tool execution.
 * @param success Whether the tool ran without error.
 * @param output Human-readable output returned to the LLM.
 * @param needsConfirmation If true, the UI must ask the user before considering this done.
 */
data class ToolResult(
    val success: Boolean,
    val output: String,
    val needsConfirmation: Boolean = false
)

/**
 * Base interface for every tool the agent can use.
 * Tool names must be snake_case and unique inside the registry.
 */
interface Tool {
    val name: String
    val description: String

    /** JSON-schema properties map used when registering tool with the LLM. */
    val parameters: Map<String, ToolParameter>

    /** Parameters that MUST be present (from the JSON schema required array). */
    val requiredParameters: List<String>

    /**
     * Execute the tool with the provided input map.
     * Called on a background dispatcher.
     */
    suspend fun execute(input: Map<String, Any>): ToolResult
}

data class ToolParameter(
    val type: String,
    val description: String
)
