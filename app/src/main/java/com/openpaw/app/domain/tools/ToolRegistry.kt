package com.openpaw.app.domain.tools

import com.openpaw.app.data.remote.dto.ApiTool
import com.openpaw.app.data.remote.dto.ApiToolProperty
import com.openpaw.app.data.remote.dto.ApiToolSchema
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor(
    whatsAppTool: WhatsAppTool,
    calendarTool: CalendarTool,
    alarmTool: AlarmTool,
    openAppTool: OpenAppTool,
    memoryTool: MemoryTool,
    screenTool: ScreenTool,
    fileManagerTool: FileManagerTool,
    smsTool: SmsTool,
    clipboardTool: ClipboardTool
) {
    private val tools: Map<String, Tool> = listOf(
        screenTool,                 // first = highest priority for the agent
        whatsAppTool,
        calendarTool,
        alarmTool,
        openAppTool,
        memoryTool,
        fileManagerTool,
        smsTool,
        clipboardTool
    ).associateBy { it.name }

    /** All tools as Anthropic API-compatible DTOs. */
    fun toApiTools(): List<ApiTool> = tools.values.map { tool ->
        ApiTool(
            name = tool.name,
            description = tool.description,
            inputSchema = ApiToolSchema(
                type = "object",
                properties = tool.parameters.mapValues { (_, p) ->
                    ApiToolProperty(type = p.type, description = p.description)
                },
                required = tool.requiredParameters
            )
        )
    }

    /** Execute a tool by name with the given input. */
    suspend fun execute(toolName: String, input: Map<String, Any>): ToolResult {
        val tool = tools[toolName]
            ?: return ToolResult(false, "Unknown tool: '$toolName'")
        return tool.execute(input)
    }

    fun getTool(name: String): Tool? = tools[name]
}
