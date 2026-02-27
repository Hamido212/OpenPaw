package com.openpaw.app.domain.tools

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val name = "create_calendar_event"
    override val description = "Create a calendar event. Opens the calendar app with the event pre-filled for the user to confirm."
    override val parameters = mapOf(
        "title" to ToolParameter("string", "Title/name of the event."),
        "start_time" to ToolParameter("string", "Start time in ISO 8601 format: yyyy-MM-dd'T'HH:mm, e.g. 2025-03-15T14:00"),
        "end_time" to ToolParameter("string", "End time in ISO 8601 format. Optional – defaults to 1h after start."),
        "description" to ToolParameter("string", "Event description (optional)."),
        "location" to ToolParameter("string", "Event location (optional).")
    )
    override val requiredParameters = listOf("title", "start_time")

    private val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())

    override suspend fun execute(input: Map<String, Any>): ToolResult {
        val title = input["title"] as? String ?: return ToolResult(false, "Missing 'title'.")
        val startStr = input["start_time"] as? String ?: return ToolResult(false, "Missing 'start_time'.")

        return try {
            val startMs = fmt.parse(startStr)?.time ?: return ToolResult(false, "Could not parse start_time: $startStr")
            val endMs = (input["end_time"] as? String)?.let { fmt.parse(it)?.time }
                ?: (startMs + 60 * 60 * 1000)

            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMs)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMs)
                (input["description"] as? String)?.let { putExtra(CalendarContract.Events.DESCRIPTION, it) }
                (input["location"] as? String)?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult(true, "Calendar opened with event '$title' pre-filled. User must save it.", needsConfirmation = true)
        } catch (e: Exception) {
            ToolResult(false, "Failed to open calendar: ${e.message}")
        }
    }
}
