package com.openpaw.app.domain.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val name = "set_alarm"
    override val description = "Set an alarm or countdown timer on the device."
    override val parameters = mapOf(
        "hour"          to ToolParameter("integer", "Hour of alarm (0-23, 24-hour format)."),
        "minutes"       to ToolParameter("integer", "Minutes of alarm (0-59). Defaults to 0."),
        "message"       to ToolParameter("string", "Label for the alarm (optional)."),
        "timer_seconds" to ToolParameter("integer", "Set a countdown timer instead: duration in seconds.")
    )
    override val requiredParameters = listOf<String>()

    // Clock app packages to try in order of preference — covers all major Android ROMs
    private val clockPackages = listOf(
        "com.google.android.deskclock",       // Stock Android / Pixel
        "com.android.deskclock",              // AOSP
        "com.sec.android.app.clockpackage",   // Samsung One UI
        "com.samsung.android.app.clockpack",  // Older Samsung
        "com.miui.clock",                     // Xiaomi MIUI
        "com.asus.deskclock",                 // ASUS ZenUI
        "com.oneplus.deskclock",              // OnePlus OxygenOS
        "com.oppo.clock",                     // OPPO / ColorOS
        "com.vivo.clock",                     // Vivo
        "com.realme.clock"                    // Realme
    )

    override suspend fun execute(input: Map<String, Any>): ToolResult {
        val timerSecs = (input["timer_seconds"] as? Number)?.toInt()
        val label     = (input["message"] as? String) ?: "OpenPaw reminder"

        return if (timerSecs != null) {
            setTimer(timerSecs, label)
        } else {
            val hour = (input["hour"] as? Number)?.toInt()
                ?: return ToolResult(false, "Provide 'hour' for alarm or 'timer_seconds' for timer.")
            val minutes = (input["minutes"] as? Number)?.toInt() ?: 0
            setAlarm(hour, minutes, label)
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun setAlarm(hour: Int, minutes: Int, label: String): ToolResult {
        val time = "%02d:%02d".format(hour, minutes)
        val base = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minutes)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // 1. Try without specifying a package (system picks best clock app)
        if (tryStart(base)) return ToolResult(true, "Alarm set for $time – \"$label\".")

        // 2. Try each known clock app explicitly
        for (pkg in clockPackages) {
            if (!isInstalled(pkg)) continue
            if (tryStart(Intent(base).setPackage(pkg))) {
                return ToolResult(true, "Alarm set for $time – \"$label\".")
            }
        }

        return ToolResult(false, "Could not set alarm automatically. Please open your Clock app and set it manually.")
    }

    private fun setTimer(seconds: Int, label: String): ToolResult {
        val base = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (tryStart(base)) return ToolResult(true, "Timer set for ${fmtSeconds(seconds)}.")

        for (pkg in clockPackages) {
            if (!isInstalled(pkg)) continue
            if (tryStart(Intent(base).setPackage(pkg))) {
                return ToolResult(true, "Timer set for ${fmtSeconds(seconds)}.")
            }
        }

        return ToolResult(false, "Could not set timer automatically. Please open your Clock app manually.")
    }

    private fun tryStart(intent: Intent): Boolean = try {
        context.startActivity(intent); true
    } catch (_: SecurityException) { false
    } catch (_: android.content.ActivityNotFoundException) { false
    } catch (_: Exception) { false }

    private fun isInstalled(pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0); true
    } catch (_: PackageManager.NameNotFoundException) { false }

    private fun fmtSeconds(s: Int): String = when {
        s >= 3600 -> "${s / 3600}h ${(s % 3600) / 60}min"
        s >= 60   -> "${s / 60}min${if (s % 60 > 0) " ${s % 60}s" else ""}"
        else      -> "${s}s"
    }
}
