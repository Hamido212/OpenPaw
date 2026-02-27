package com.openpaw.app.domain.tools

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val name = "set_alarm"
    override val description = "Set an alarm or reminder on the device. Can create alarms at specific times or timers for a duration."
    override val parameters = mapOf(
        "hour" to ToolParameter("integer", "Hour of alarm (0-23, 24h format)."),
        "minutes" to ToolParameter("integer", "Minutes of alarm (0-59). Defaults to 0."),
        "message" to ToolParameter("string", "Label/message for the alarm (optional)."),
        "timer_seconds" to ToolParameter("integer", "Set a timer instead: duration in seconds. If provided, hour/minutes are ignored.")
    )
    override val requiredParameters = listOf<String>()

    override suspend fun execute(input: Map<String, Any>): ToolResult {
        return try {
            val timerSecs = (input["timer_seconds"] as? Number)?.toInt()

            if (timerSecs != null) {
                // Set a timer
                val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, timerSecs)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    (input["message"] as? String)?.let { putExtra(AlarmClock.EXTRA_MESSAGE, it) }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ToolResult(true, "Timer set for $timerSecs seconds.")
            } else {
                val hour = (input["hour"] as? Number)?.toInt()
                    ?: return ToolResult(false, "Provide 'hour' for alarm or 'timer_seconds' for timer.")
                val minutes = (input["minutes"] as? Number)?.toInt() ?: 0
                val label = input["message"] as? String ?: "OpenPaw reminder"

                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ToolResult(true, "Alarm set for ${"%02d".format(hour)}:${"%02d".format(minutes)} – \"$label\".")
            }
        } catch (e: Exception) {
            ToolResult(false, "Failed to set alarm: ${e.message}")
        }
    }
}
