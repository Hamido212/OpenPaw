package com.openpaw.app.domain.tools

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val name = "sms"
    override val description = "Send or read classic SMS messages (not WhatsApp). For 'send': sends an SMS to a phone number. For 'read': reads recent inbox messages."
    override val parameters = mapOf(
        "action"  to ToolParameter("string", "Action: 'send' or 'read'"),
        "phone"   to ToolParameter("string", "Recipient phone number with country code, e.g. +49123456789. Required for 'send'."),
        "message" to ToolParameter("string", "SMS text to send. Required for 'send'."),
        "count"   to ToolParameter("number", "Number of recent messages to retrieve (default: 5, max: 20). For 'read'.")
    )
    override val requiredParameters = listOf("action")

    @Suppress("DEPRECATION")
    private fun getSmsManager(): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            context.getSystemService(SmsManager::class.java)
        else
            SmsManager.getDefault()

    override suspend fun execute(input: Map<String, Any>): ToolResult {
        val action = input["action"] as? String ?: return ToolResult(false, "Missing 'action'.")

        return when (action.lowercase()) {

            "send" -> {
                val phone   = input["phone"]   as? String ?: return ToolResult(false, "Missing 'phone' for send.")
                val message = input["message"] as? String ?: return ToolResult(false, "Missing 'message' for send.")
                try {
                    getSmsManager().sendTextMessage(phone, null, message, null, null)
                    ToolResult(true, "SMS sent to $phone.")
                } catch (e: SecurityException) {
                    ToolResult(false, "SEND_SMS permission not granted. Please enable it in App-Info → Permissions.")
                } catch (e: Exception) {
                    ToolResult(false, "Failed to send SMS: ${e.message}")
                }
            }

            "read" -> {
                val count = when (val raw = input["count"]) {
                    is Number -> raw.toInt().coerceIn(1, 20)
                    is String -> raw.toIntOrNull()?.coerceIn(1, 20) ?: 5
                    else      -> 5
                }
                try {
                    val cursor: Cursor? = context.contentResolver.query(
                        Telephony.Sms.Inbox.CONTENT_URI,
                        arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                        null, null,
                        "${Telephony.Sms.DATE} DESC"
                    )
                    if (cursor == null) {
                        return ToolResult(false, "Cannot access SMS inbox. READ_SMS permission may be missing.")
                    }

                    val dateFormat = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
                    val messages = mutableListOf<String>()
                    var i = 0
                    cursor.use { c ->
                        while (c.moveToNext() && i < count) {
                            val from = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: "Unknown"
                            val body = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.BODY))    ?: ""
                            val date = dateFormat.format(Date(c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.DATE))))
                            messages.add("[$date] From: $from\n$body")
                            i++
                        }
                    }

                    if (messages.isEmpty())
                        ToolResult(true, "SMS inbox is empty.")
                    else
                        ToolResult(true, "Last $i SMS:\n\n${messages.joinToString("\n\n---\n\n")}")

                } catch (e: SecurityException) {
                    ToolResult(false, "READ_SMS permission not granted. Please enable it in App-Info → Permissions.")
                } catch (e: Exception) {
                    ToolResult(false, "Failed to read SMS: ${e.message}")
                }
            }

            else -> ToolResult(false, "Unknown action '$action'. Use: send or read.")
        }
    }
}
