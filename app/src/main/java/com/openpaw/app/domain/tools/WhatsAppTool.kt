package com.openpaw.app.domain.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsAppTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val name = "send_whatsapp"
    override val description = "Send a WhatsApp message to a phone number or contact name. Opens WhatsApp with the message pre-filled so the user can confirm before sending."
    override val parameters = mapOf(
        "phone" to ToolParameter("string", "Phone number with country code, e.g. +49123456789. Use 'contact_name' if you don't know the number."),
        "contact_name" to ToolParameter("string", "Name of the contact (optional, used if phone is unknown)."),
        "message" to ToolParameter("string", "The message text to send.")
    )
    override val requiredParameters = listOf("message")

    override suspend fun execute(input: Map<String, Any>): ToolResult {
        val message = input["message"] as? String ?: return ToolResult(false, "Missing 'message' parameter.")
        val phone = input["phone"] as? String
        val contactName = input["contact_name"] as? String

        return try {
            val intent = if (!phone.isNullOrBlank()) {
                // Direct phone number → wa.me deep link
                val cleanPhone = phone.replace(Regex("[^+0-9]"), "")
                Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(message)}"))
            } else {
                // No phone: open WhatsApp general share
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    setPackage("com.whatsapp")
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            val target = phone ?: contactName ?: "contact"
            ToolResult(
                success = true,
                output = "WhatsApp opened with pre-filled message to $target. User must press send.",
                needsConfirmation = true
            )
        } catch (e: Exception) {
            ToolResult(false, "Failed to open WhatsApp: ${e.message}. Is WhatsApp installed?")
        }
    }
}
