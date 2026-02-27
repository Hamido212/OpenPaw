package com.openpaw.app.domain.tools

import com.openpaw.app.data.repository.MemoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryTool @Inject constructor(
    private val memoryRepository: MemoryRepository
) : Tool {

    override val name = "manage_memory"
    override val description = "Store, retrieve, or delete facts about the user that should persist across conversations. Use this to remember user preferences, important info, etc."
    override val parameters = mapOf(
        "action" to ToolParameter("string", "One of: 'remember', 'recall', 'forget', 'list'"),
        "key" to ToolParameter("string", "The memory key (short identifier, e.g. 'user_name', 'favorite_music')."),
        "value" to ToolParameter("string", "Value to store (required for 'remember' action)."),
        "category" to ToolParameter("string", "Category for organizing memories: 'personal', 'preferences', 'work', 'general' (default).")
    )
    override val requiredParameters = listOf("action")

    override suspend fun execute(input: Map<String, Any>): ToolResult {
        val action = input["action"] as? String ?: return ToolResult(false, "Missing 'action'.")
        val key = input["key"] as? String
        val value = input["value"] as? String
        val category = input["category"] as? String ?: "general"

        return when (action) {
            "remember" -> {
                if (key == null || value == null) return ToolResult(false, "Both 'key' and 'value' required for 'remember'.")
                memoryRepository.remember(key, value, category)
                ToolResult(true, "Remembered: $key = $value")
            }
            "recall" -> {
                if (key == null) return ToolResult(false, "'key' required for 'recall'.")
                val recalled = memoryRepository.recall(key)
                if (recalled != null) ToolResult(true, "Memory for '$key': $recalled")
                else ToolResult(true, "No memory found for '$key'.")
            }
            "forget" -> {
                if (key == null) return ToolResult(false, "'key' required for 'forget'.")
                memoryRepository.forget(key)
                ToolResult(true, "Forgot memory: $key")
            }
            "list" -> {
                val all = memoryRepository.getAllMemories()
                if (all.isEmpty()) ToolResult(true, "No memories stored yet.")
                else ToolResult(true, all.joinToString("\n") { "- ${it.key}: ${it.value}" })
            }
            else -> ToolResult(false, "Unknown action '$action'. Use: remember, recall, forget, list.")
        }
    }
}
