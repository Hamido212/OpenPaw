package com.openpaw.app.domain.tools

import com.openpaw.app.service.OpenPawAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Gives the AI agent full control over the Android screen via AccessibilityService.
 *
 * Exposed sub-tools:
 *   action=read   → dump all visible text + interactive elements
 *   action=click  → click element matching query text
 *   action=input  → type text into a field
 *   action=scroll → scroll up/down/left/right
 *   action=swipe  → swipe gesture (for apps without a11y nodes)
 *   action=tap    → tap at exact x,y coordinates
 *   action=back   → press Back
 *   action=home   → press Home
 */
@Singleton
class ScreenTool @Inject constructor() : Tool {

    override val name = "control_screen"
    override val description = """
        Read the current Android screen content or interact with UI elements.
        The AI agent can read text, click buttons, type into fields, and scroll — just like a human.
        Requires the OpenPaw Accessibility Service to be enabled in system Settings → Accessibility.
    """.trimIndent()

    override val parameters = mapOf(
        "action" to ToolParameter("string", "What to do: 'read', 'click', 'input', 'scroll', 'swipe', 'tap', 'back', 'home', 'recents'"),
        "query" to ToolParameter("string", "For 'click': text/label/id of element to click. For 'input': field hint or leave empty for focused field."),
        "text" to ToolParameter("string", "For 'input': the text to type."),
        "direction" to ToolParameter("string", "For 'scroll'/'swipe': 'up', 'down', 'left', 'right'."),
        "x" to ToolParameter("number", "For 'tap': x coordinate in screen pixels."),
        "y" to ToolParameter("number", "For 'tap': y coordinate in screen pixels.")
    )
    override val requiredParameters = listOf("action")

    override suspend fun execute(input: Map<String, Any>): ToolResult {
        val service = OpenPawAccessibilityService.instance.value
            ?: return ToolResult(
                success = false,
                output = "AccessibilityService not active. Please enable 'OpenPaw' under Settings → Accessibility → Installed services."
            )

        return when (val action = input["action"] as? String) {

            "read" -> {
                // Truncate to keep token count small – 2500 chars is enough for the LLM
                val screen = service.readScreen().take(2500)
                ToolResult(true, "Current screen:\n$screen")
            }

            "click" -> {
                val query = input["query"] as? String
                    ?: return ToolResult(false, "Provide 'query' (text to click).")

                // 1. Try accessibility node click (instant, works when node is clickable)
                if (service.clickElement(query)) {
                    return ToolResult(true, "Clicked: '$query'")
                }

                // 2. Fallback: find the element's visual bounds and gesture-tap the center.
                //    This handles cases where text lives in a non-clickable child of a clickable parent.
                val bounds = service.getNodeBounds(query)
                if (bounds != null && !bounds.isEmpty) {
                    val ok = suspendCancellableCoroutine<Boolean> { cont ->
                        service.tapAt(bounds.centerX().toFloat(), bounds.centerY().toFloat()) { cont.resume(it) }
                    }
                    if (ok) return ToolResult(true, "Tapped '$query' at (${bounds.centerX()}, ${bounds.centerY()})")
                }

                ToolResult(false, "Element '$query' not found. Use 'read' to check what's visible.")
            }

            "input" -> {
                val text = input["text"] as? String
                    ?: return ToolResult(false, "Provide 'text' to type.")
                val fieldHint = input["query"] as? String ?: ""
                val ok = service.inputText(text, fieldHint)
                if (ok) ToolResult(true, "Typed '$text' into ${if (fieldHint.isBlank()) "focused field" else "'$fieldHint'"}")
                else ToolResult(false, "Could not find editable field. Tap a text field first.")
            }

            "scroll" -> {
                val dir = input["direction"] as? String ?: "down"

                // 1. Try accessibility scroll (works when app exposes isScrollable=true)
                if (service.scroll(dir)) {
                    return ToolResult(true, "Scrolled $dir")
                }

                // 2. Fallback: gesture swipe — works in any app, even without a11y scroll nodes
                val swiped = suspendCancellableCoroutine<Boolean> { cont ->
                    service.swipe(dir) { cont.resume(it) }
                }
                if (swiped) ToolResult(true, "Scrolled $dir via gesture")
                else ToolResult(false, "Could not scroll $dir.")
            }

            "swipe" -> {
                val dir = input["direction"] as? String ?: "up"
                val ok = suspendCancellableCoroutine<Boolean> { cont ->
                    service.swipe(dir) { cont.resume(it) }
                }
                if (ok) ToolResult(true, "Swiped $dir")
                else ToolResult(false, "Swipe gesture cancelled.")
            }

            "tap" -> {
                val x = (input["x"] as? Number)?.toFloat()
                    ?: return ToolResult(false, "Provide 'x' coordinate.")
                val y = (input["y"] as? Number)?.toFloat()
                    ?: return ToolResult(false, "Provide 'y' coordinate.")
                val ok = suspendCancellableCoroutine<Boolean> { cont ->
                    service.tapAt(x, y) { cont.resume(it) }
                }
                if (ok) ToolResult(true, "Tapped at ($x, $y)")
                else ToolResult(false, "Tap at ($x, $y) failed.")
            }

            "back" -> {
                service.pressBack()
                delay(500) // wait for screen transition to complete
                ToolResult(true, "Pressed Back")
            }
            "home" -> {
                service.pressHome()
                delay(700) // wait for homescreen to fully load
                ToolResult(true, "Pressed Home")
            }
            "recents" -> {
                service.pressRecents()
                ToolResult(true, "Pressed Recents")
            }
            "notifications" -> {
                service.pressNotifications()
                ToolResult(true, "Opened notification shade")
            }

            else -> ToolResult(false, "Unknown action '$action'. Use: read, click, input, scroll, swipe, tap, back, home, recents")
        }
    }
}
