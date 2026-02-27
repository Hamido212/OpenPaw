package com.openpaw.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * OpenPaw AccessibilityService – lets the AI agent:
 *  - Read all visible text on screen
 *  - Click elements by text / contentDescription / resourceId
 *  - Input text into focused fields
 *  - Scroll in any direction
 *  - Press system keys (Back, Home, Recents)
 *
 * The service exposes a companion-object singleton so that domain-layer
 * ScreenTool can call it from the same process without a binder.
 */
class OpenPawAccessibilityService : AccessibilityService() {

    // ─── Singleton access ────────────────────────────────────────────────────

    companion object {
        private val _instance = MutableStateFlow<OpenPawAccessibilityService?>(null)
        val instance: StateFlow<OpenPawAccessibilityService?> = _instance.asStateFlow()

        fun isActive(): Boolean = _instance.value != null
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        _instance.value = this
    }

    override fun onInterrupt() { /* required, usually empty */ }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* passthrough */ }

    override fun onDestroy() {
        _instance.value = null
        super.onDestroy()
    }

    // ─── Screen reading ───────────────────────────────────────────────────────

    /**
     * Returns a structured dump of all visible text and interactive elements.
     * Format: each node on its own line as "[TYPE] text | id=... | desc=..."
     */
    fun readScreen(): String {
        val root = rootInActiveWindow ?: return "No active window available."
        val builder = StringBuilder()
        collectNodes(root, builder, depth = 0)
        root.recycle()
        return builder.toString().ifBlank { "Screen is empty or not readable." }
    }

    private fun collectNodes(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int) {
        val indent = "  ".repeat(depth.coerceAtMost(6))
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val cls = node.className?.toString()?.substringAfterLast('.') ?: ""
        val id = node.viewIdResourceName?.substringAfter('/') ?: ""
        val clickable = node.isClickable
        val editable = node.isEditable

        val label = when {
            !text.isNullOrEmpty() -> text
            !desc.isNullOrEmpty() -> desc
            else -> null
        }

        if (label != null || clickable || editable) {
            val type = when {
                editable -> "INPUT"
                clickable -> "BUTTON"
                else -> "TEXT"
            }
            sb.appendLine(buildString {
                append("$indent[$type] ")
                if (label != null) append(label)
                if (id.isNotEmpty()) append(" | id=$id")
                if (desc != null && desc != label) append(" | desc=$desc")
            })
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectNodes(child, sb, depth + 1)
                child.recycle()
            }
        }
    }

    // ─── Click ────────────────────────────────────────────────────────────────

    /**
     * Finds the first node matching [query] (text, contentDescription, or resourceId)
     * and performs a click on it.
     * @return true if element found and clicked, false otherwise.
     */
    fun clickElement(query: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNode(root, query)
        val clicked = node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
        node?.recycle()
        root.recycle()
        return clicked
    }

    // ─── Input text ───────────────────────────────────────────────────────────

    /**
     * Finds the first editable field matching [fieldHint] (or the currently focused field
     * if [fieldHint] is blank) and types [text] into it.
     */
    fun inputText(text: String, fieldHint: String = ""): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = if (fieldHint.isBlank()) {
            findFocusedEditable(root)
        } else {
            findEditableNode(root, fieldHint)
        }
        if (node == null) {
            root.recycle()
            return false
        }
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply {
            putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        node.recycle()
        root.recycle()
        return result
    }

    // ─── Scroll ───────────────────────────────────────────────────────────────

    /**
     * Scrolls the first scrollable container in the given direction.
     * [direction]: "up" | "down" | "left" | "right"
     */
    fun scroll(direction: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = when (direction.lowercase()) {
            "up" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            "down" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            "left" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            "right" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            else -> return false
        }
        val node = findScrollable(root)
        val result = node?.performAction(action) ?: false
        node?.recycle()
        root.recycle()
        return result
    }

    // ─── Swipe gesture ────────────────────────────────────────────────────────

    /**
     * Performs a swipe gesture across the screen center.
     * Useful for apps that don't expose accessibility nodes (e.g. games, maps).
     */
    fun swipe(direction: String, onResult: (Boolean) -> Unit) {
        val display = resources.displayMetrics
        val w = display.widthPixels.toFloat()
        val h = display.heightPixels.toFloat()

        val (startX, startY, endX, endY) = when (direction.lowercase()) {
            "up" -> listOf(w / 2, h * 0.7f, w / 2, h * 0.3f)
            "down" -> listOf(w / 2, h * 0.3f, w / 2, h * 0.7f)
            "left" -> listOf(w * 0.8f, h / 2, w * 0.2f, h / 2)
            "right" -> listOf(w * 0.2f, h / 2, w * 0.8f, h / 2)
            else -> { onResult(false); return }
        }

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) = onResult(true)
            override fun onCancelled(gestureDescription: GestureDescription) = onResult(false)
        }, null)
    }

    // ─── System actions ───────────────────────────────────────────────────────

    fun pressBack() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun pressHome() = performGlobalAction(GLOBAL_ACTION_HOME)
    fun pressRecents() = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun pressNotifications() = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

    // ─── Tap by coordinate ───────────────────────────────────────────────────

    fun tapAt(x: Float, y: Float, onResult: (Boolean) -> Unit) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription) = onResult(true)
            override fun onCancelled(g: GestureDescription) = onResult(false)
        }, null)
    }

    // ─── Node search helpers ─────────────────────────────────────────────────

    private fun findNode(root: AccessibilityNodeInfo, query: String): AccessibilityNodeInfo? {
        val lq = query.lowercase()
        return searchTree(root) { node ->
            node.text?.toString()?.lowercase()?.contains(lq) == true ||
            node.contentDescription?.toString()?.lowercase()?.contains(lq) == true ||
            node.viewIdResourceName?.substringAfter('/')?.lowercase()?.contains(lq) == true
        }
    }

    private fun findEditableNode(root: AccessibilityNodeInfo, hint: String): AccessibilityNodeInfo? {
        val lh = hint.lowercase()
        return searchTree(root) { node ->
            node.isEditable && (
                node.text?.toString()?.lowercase()?.contains(lh) == true ||
                node.contentDescription?.toString()?.lowercase()?.contains(lh) == true ||
                node.hintText?.toString()?.lowercase()?.contains(lh) == true
            )
        }
    }

    private fun findFocusedEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        searchTree(root) { it.isEditable && it.isFocused }
            ?: searchTree(root) { it.isEditable }

    private fun findScrollable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        searchTree(root) { it.isScrollable }

    /** BFS through the node tree, returns first node where [predicate] is true. */
    private fun searchTree(
        root: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (predicate(node)) return node
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    // ─── Bounding box helper ─────────────────────────────────────────────────

    fun getNodeBounds(query: String): Rect? {
        val root = rootInActiveWindow ?: return null
        val node = findNode(root, query) ?: return null
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        node.recycle()
        root.recycle()
        return bounds
    }
}
