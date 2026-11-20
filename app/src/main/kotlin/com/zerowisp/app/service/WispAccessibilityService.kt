package com.zerowisp.app.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class WispAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        private var instance: WispAccessibilityService? = null

        val isEnabled: Boolean get() = instance != null

        /**
         * Paste text into the currently focused editable field.
         * Returns true if paste succeeded via AccessibilityService,
         * false if fallback to clipboard is needed.
         */
        fun pasteText(text: String): Boolean {
            val svc = instance ?: return false
            val node = svc.focusedEditableNode ?: return false

            return try {
                // Try ACTION_SET_TEXT first (preserves existing text via proper insert)
                // For SET_TEXT: append to existing text
                val existing = node.text?.toString() ?: ""
                val newText = if (existing.isEmpty()) text else "$existing $text"

                val args = Bundle()
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
                val setResult = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

                if (!setResult) {
                    // Fallback: clipboard paste
                    val clipboard = svc.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("zerowisp", text))
                    node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                } else {
                    true
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    @Volatile
    private var focusedEditableNode: AccessibilityNodeInfo? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                val node = event.source ?: return
                if (node.isEditable) {
                    focusedEditableNode = node
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Scan for focused editable in root
                val root = rootInActiveWindow ?: return
                focusedEditableNode = findFocusedEditable(root)
            }
        }
    }

    private fun findFocusedEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isEditable && root.isFocused) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findFocusedEditable(child)
            if (found != null) return found
        }
        return null
    }

    override fun onInterrupt() {
        focusedEditableNode = null
    }

    override fun onDestroy() {
        instance = null
        focusedEditableNode = null
        super.onDestroy()
    }
}
