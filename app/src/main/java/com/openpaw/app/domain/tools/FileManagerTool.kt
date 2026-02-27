package com.openpaw.app.domain.tools

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManagerTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val name = "file_manager"
    override val description = "Manage files in the app's storage: read, write, list, delete, or share text files."
    override val parameters = mapOf(
        "action"    to ToolParameter("string", "Action: 'read', 'write', 'list', 'delete', 'share'"),
        "filename"  to ToolParameter("string", "File name (e.g. 'note.txt'). Not needed for 'list'."),
        "content"   to ToolParameter("string", "Text content to write. Required for 'write'."),
        "subfolder" to ToolParameter("string", "Optional subfolder within app storage (e.g. 'notes').")
    )
    override val requiredParameters = listOf("action")

    private fun storageDir(subfolder: String?): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return if (!subfolder.isNullOrBlank()) {
            File(base, subfolder).also { it.mkdirs() }
        } else {
            base
        }
    }

    override suspend fun execute(input: Map<String, Any>): ToolResult {
        val action   = input["action"]    as? String ?: return ToolResult(false, "Missing 'action'.")
        val filename = input["filename"]  as? String
        val content  = input["content"]   as? String
        val subfolder = input["subfolder"] as? String

        return when (action.lowercase()) {

            "list" -> {
                val dir = storageDir(subfolder)
                val files = dir.listFiles()
                if (files.isNullOrEmpty()) {
                    ToolResult(true, "No files found in ${dir.absolutePath}.")
                } else {
                    val listing = files.sortedBy { it.name }.joinToString("\n") { f ->
                        if (f.isDirectory) "[dir]  ${f.name}"
                        else               "       ${f.name}  (${f.length()} bytes)"
                    }
                    ToolResult(true, "Files in ${dir.absolutePath}:\n$listing")
                }
            }

            "read" -> {
                if (filename.isNullOrBlank()) return ToolResult(false, "Missing 'filename' for read.")
                val file = File(storageDir(subfolder), filename)
                if (!file.exists()) return ToolResult(false, "File not found: ${file.absolutePath}")
                try {
                    ToolResult(true, "Content of '${file.name}':\n${file.readText()}")
                } catch (e: Exception) {
                    ToolResult(false, "Failed to read '${file.name}': ${e.message}")
                }
            }

            "write" -> {
                if (filename.isNullOrBlank()) return ToolResult(false, "Missing 'filename' for write.")
                if (content == null)          return ToolResult(false, "Missing 'content' for write.")
                val file = File(storageDir(subfolder), filename)
                try {
                    file.writeText(content)
                    ToolResult(true, "Written ${content.length} characters to '${file.absolutePath}'.")
                } catch (e: Exception) {
                    ToolResult(false, "Failed to write '${file.name}': ${e.message}")
                }
            }

            "delete" -> {
                if (filename.isNullOrBlank()) return ToolResult(false, "Missing 'filename' for delete.")
                val file = File(storageDir(subfolder), filename)
                if (!file.exists()) return ToolResult(false, "File not found: '${file.name}'.")
                return if (file.delete())
                    ToolResult(true, "Deleted '${file.name}'.")
                else
                    ToolResult(false, "Could not delete '${file.name}'.")
            }

            "share" -> {
                if (filename.isNullOrBlank()) return ToolResult(false, "Missing 'filename' for share.")
                val file = File(storageDir(subfolder), filename)
                if (!file.exists()) return ToolResult(false, "File not found: '${file.name}'.")
                try {
                    val shareText = file.readText()
                    val chooser = Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, file.name)
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                        "Share '${file.name}'"
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(chooser)
                    ToolResult(true, "Share dialog opened for '${file.name}'.", needsConfirmation = true)
                } catch (e: Exception) {
                    ToolResult(false, "Failed to share '${file.name}': ${e.message}")
                }
            }

            else -> ToolResult(false, "Unknown action '$action'. Use: read, write, list, delete, share.")
        }
    }
}
