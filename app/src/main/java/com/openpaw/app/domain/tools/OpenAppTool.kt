package com.openpaw.app.domain.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAppTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val name = "open_app"
    override val description = "Launch an app on the device by name or package ID. Example: 'open Spotify' or 'open com.spotify.music'."
    override val parameters = mapOf(
        "app_name" to ToolParameter("string", "Human-readable app name, e.g. 'Spotify', 'Maps', 'Camera'."),
        "package_name" to ToolParameter("string", "Exact Android package name (optional), e.g. 'com.spotify.music'.")
    )
    override val requiredParameters = listOf<String>()

    // Commonly used app name → package mappings
    private val wellKnown = mapOf(
        "whatsapp" to "com.whatsapp",
        "spotify" to "com.spotify.music",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "youtube" to "com.google.android.youtube",
        "camera" to "com.android.camera2",
        "photos" to "com.google.android.apps.photos",
        "gmail" to "com.google.android.gm",
        "chrome" to "com.android.chrome",
        "settings" to "com.android.settings",
        "instagram" to "com.instagram.android",
        "telegram" to "org.telegram.messenger",
        "netflix" to "com.netflix.mediaclient",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android"
    )

    override suspend fun execute(input: Map<String, Any>): ToolResult {
        val appName = (input["app_name"] as? String)?.lowercase()?.trim()
        val packageName = input["package_name"] as? String

        val pkg = packageName
            ?: appName?.let { wellKnown[it] }
            ?: return ToolResult(false, "Provide 'app_name' or 'package_name'.")

        return try {
            val pm: PackageManager = context.packageManager
            val launchIntent: Intent? = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                ToolResult(true, "Opened app: ${appName ?: pkg}")
            } else {
                ToolResult(false, "App '$pkg' not found or not launchable. Is it installed?")
            }
        } catch (e: Exception) {
            ToolResult(false, "Error launching app: ${e.message}")
        }
    }
}
