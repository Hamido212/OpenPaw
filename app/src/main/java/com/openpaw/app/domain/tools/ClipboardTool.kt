package com.openpaw.app.domain.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val name = "clipboard"
    override val description = "Copy text to the Android clipboard ('copy') or read the current clipboard content ('paste'). Note: reading clipboard requires the app to be in foreground on Android 10+."
    override val parameters = mapOf(
        "action" to ToolParameter("string", "Action: 'copy' (write to clipboard) or 'paste' (read from clipboard)"),
        "text"   to ToolParameter("string", "Text to copy. Required for 'copy'."),
        "label"  to ToolParameter("string", "Optional label for the clipboard entry (default: 'OpenPaw').")
    )
    override val requiredParameters = listOf("action")

    override suspend fun execute(input: Map<String, Any>): ToolResult {
        val action = input["action"] as? String ?: return ToolResult(false, "Missing 'action'.")

        // ClipboardManager must be accessed on the main thread
        return withContext(Dispatchers.Main) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return@withContext ToolResult(false, "Clipboard service unavailable.")

            when (action.lowercase()) {

                "copy" -> {
                    val text  = input["text"]  as? String ?: return@withContext ToolResult(false, "Missing 'text' for copy.")
                    val label = input["label"] as? String ?: "OpenPaw"
                    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
                    val preview = if (text.length > 120) text.take(120) + "…" else text
                    ToolResult(true, "Copied to clipboard: \"$preview\"")
                }

                "paste" -> {
                    val clip = clipboard.primaryClip
                    if (clip == null || clip.itemCount == 0) {
                        return@withContext ToolResult(true, "Clipboard is empty.")
                    }
                    val text = clip.getItemAt(0).coerceToText(context).toString()
                    ToolResult(true, "Clipboard content:\n$text")
                }

                else -> ToolResult(false, "Unknown action '$action'. Use: copy or paste.")
            }
        }
    }
}
