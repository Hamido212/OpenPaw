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
    override val description = "Launch an installed app by name or package ID. " +
        "Examples: app_name=\"Spotify\", app_name=\"TikTok\", app_name=\"Instagram\", " +
        "package_name=\"com.spotify.music\"."
    override val parameters = mapOf(
        "app_name"     to ToolParameter("string", "Human-readable app name, e.g. 'Spotify', 'TikTok', 'Camera'."),
        "package_name" to ToolParameter("string", "Exact Android package name (optional), e.g. 'com.spotify.music'.")
    )
    override val requiredParameters = listOf<String>()

    // Clock app packages ordered by popularity – covers all major Android ROMs
    private val clockPackages = listOf(
        "com.google.android.deskclock",       // Pixel / Stock Android
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

    // Apps that need to try multiple package names (ROM-specific packages)
    private val multiPackages: Map<String, List<String>> by lazy {
        val clk = clockPackages
        mapOf(
            "clock" to clk,  "uhr" to clk,  "wecker" to clk,
            "alarm" to clk,  "timer" to clk, "stopwatch" to clk,
            "stoppuhr" to clk, "alarmclock" to clk, "deskclock" to clk
        )
    }

    // Fast lookup table for very common apps (avoid full package scan)
    private val wellKnown = mapOf(
        "whatsapp"        to "com.whatsapp",
        "spotify"         to "com.spotify.music",
        "maps"            to "com.google.android.apps.maps",
        "google maps"     to "com.google.android.apps.maps",
        "youtube"         to "com.google.android.youtube",
        "youtube music"   to "com.google.android.apps.youtube.music",
        "camera"          to "com.android.camera2",
        "photos"          to "com.google.android.apps.photos",
        "gmail"           to "com.google.android.gm",
        "chrome"          to "com.android.chrome",
        "settings"        to "com.android.settings",
        "einstellungen"   to "com.android.settings",
        "instagram"       to "com.instagram.android",
        "telegram"        to "org.telegram.messenger",
        "netflix"         to "com.netflix.mediaclient",
        "twitter"         to "com.twitter.android",
        "x"               to "com.twitter.android",
        "tiktok"          to "com.zhiliaoapp.musically",
        "snapchat"        to "com.snapchat.android",
        "facebook"        to "com.facebook.katana",
        "messenger"       to "com.facebook.orca",
        "reddit"          to "com.reddit.frontpage",
        "discord"         to "com.discord",
        "pinterest"       to "com.pinterest",
        "linkedin"        to "com.linkedin.android",
        "uber"            to "com.ubercab",
        "amazon"          to "com.amazon.mShop.android.shopping",
        "ebay"            to "com.ebay.mobile",
        "paypal"          to "com.paypal.android.p2pmobile",
        "zoom"            to "us.zoom.videomeetings",
        "teams"           to "com.microsoft.teams",
        "outlook"         to "com.microsoft.office.outlook",
        "word"            to "com.microsoft.office.word",
        "excel"           to "com.microsoft.office.excel",
        "powerpoint"      to "com.microsoft.office.powerpoint",
        "google drive"    to "com.google.android.apps.docs",
        "drive"           to "com.google.android.apps.docs",
        "dropbox"         to "com.dropbox.android",
        "twitch"          to "tv.twitch.android.app",
        "shazam"          to "com.shazam.android",
        "soundcloud"      to "com.soundcloud.android",
        "calculator"      to "com.google.android.calculator",
        "rechner"         to "com.google.android.calculator",
        "clock"           to "com.google.android.deskclock",
        "uhr"             to "com.google.android.deskclock",
        "wecker"          to "com.google.android.deskclock",
        "alarm"           to "com.google.android.deskclock",
        "timer"           to "com.google.android.deskclock",
        "calendar"        to "com.google.android.calendar",
        "kalender"        to "com.google.android.calendar",
        "contacts"        to "com.google.android.contacts",
        "kontakte"        to "com.google.android.contacts",
        "phone"           to "com.google.android.dialer",
        "telefon"         to "com.google.android.dialer",
        "dialer"          to "com.google.android.dialer",
        "messages"        to "com.google.android.apps.messaging",
        "nachrichten"     to "com.google.android.apps.messaging",
        "sms"             to "com.google.android.apps.messaging",
        "files"           to "com.google.android.apps.nbu.files",
        "dateien"         to "com.google.android.apps.nbu.files",
        "play store"      to "com.android.vending",
        "gallery"         to "com.google.android.apps.photos",
        "galerie"         to "com.google.android.apps.photos",
        "browser"         to "com.android.chrome",
        "maps navigation" to "com.google.android.apps.maps"
    )

    override suspend fun execute(input: Map<String, Any>): ToolResult {
        val rawAppName  = (input["app_name"] as? String)?.trim()
        val packageName = (input["package_name"] as? String)?.trim()

        val appNameLower = rawAppName?.lowercase()

        // ── Step 1: If explicit package provided, try it directly ─────────────
        if (!packageName.isNullOrBlank()) {
            return tryLaunch(packageName, rawAppName ?: packageName)
        }

        if (appNameLower.isNullOrBlank()) {
            return ToolResult(false, "Provide 'app_name' or 'package_name'.")
        }

        // ── Step 2: Multi-package fallback for ROM-specific apps (e.g. Clock) ──
        val fallbackList = multiPackages[appNameLower]
        if (fallbackList != null) {
            for (pkg in fallbackList) {
                val result = tryLaunch(pkg, rawAppName!!)
                if (result.success) return result
            }
            // All packages failed → fall through to fuzzy search
        }

        // ── Step 3: Fast wellKnown single-package lookup ───────────────────────
        val knownPkg = wellKnown[appNameLower]
        if (knownPkg != null) {
            val result = tryLaunch(knownPkg, rawAppName!!)
            if (result.success) return result
            // Package not installed → fall through to fuzzy search
        }

        // ── Step 3: Fuzzy search through all installed apps by display label ──
        val pm = context.packageManager
        val installedApps = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            return ToolResult(false, "Could not list installed apps: ${e.message}")
        }

        // Score each app: exact match > contains > first-word match
        data class AppMatch(val pkg: String, val label: String, val score: Int)

        val matches = installedApps.mapNotNull { appInfo ->
            // Skip apps without a launcher intent (non-launchable services/libs)
            if (pm.getLaunchIntentForPackage(appInfo.packageName) == null) return@mapNotNull null

            val label = appInfo.loadLabel(pm).toString()
            val labelLower = label.lowercase()

            val score = when {
                labelLower == appNameLower               -> 100   // exact
                labelLower.contains(appNameLower)        -> 60    // app name contains query
                appNameLower.contains(labelLower)        -> 50    // query contains app name
                labelLower.startsWith(appNameLower)      -> 70    // label starts with query
                levenshtein(labelLower, appNameLower) <= 2 -> 40  // close spelling
                else                                     -> 0
            }
            if (score > 0) AppMatch(appInfo.packageName, label, score) else null
        }

        val bestMatch = matches.maxByOrNull { it.score }
        if (bestMatch != null) {
            return tryLaunch(bestMatch.pkg, bestMatch.label)
        }

        return ToolResult(
            false,
            "App '$rawAppName' not found. Make sure it is installed. " +
            "Try a more exact name or provide the package_name."
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun tryLaunch(pkg: String, displayName: String): ToolResult {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(pkg)
                ?: return ToolResult(false, "App '$displayName' ($pkg) is not launchable or not installed.")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ToolResult(true, "Opened '$displayName'.")
        } catch (e: Exception) {
            ToolResult(false, "Error launching '$displayName': ${e.message}")
        }
    }

    /** Simple Levenshtein distance for fuzzy name matching (max string length 30). */
    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        // Limit to short strings to avoid performance issues
        if (a.length > 30 || b.length > 30) return Int.MAX_VALUE

        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
                else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
            }
        }
        return dp[a.length][b.length]
    }
}
