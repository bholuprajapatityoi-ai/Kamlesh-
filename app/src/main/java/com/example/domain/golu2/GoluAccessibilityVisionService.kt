package com.example.domain.golu2

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Feature 1: Smart Screen Understanding AccessibilityService.
 * Strictly passive: does NOT record or upload anything automatically.
 * Only reads visible, non-password text when the user explicitly taps "Capture Current Screen".
 */
class GoluAccessibilityVisionService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Deliberately no background logging or silent monitoring.
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun captureVisibleText(): Pair<String, String> {
        val root = rootInActiveWindow ?: return "" to "No active window accessible."
        val pkg = root.packageName?.toString().orEmpty()
        val builder = StringBuilder()
        collectSafeNodes(root, builder, depth = 0)
        val result = builder.toString().trim()
        return pkg to result
    }

    private fun collectSafeNodes(node: AccessibilityNodeInfo, out: StringBuilder, depth: Int) {
        if (depth > 20 || out.length > 10_000) return
        // Never read password or sensitive obscured fields
        if (node.isPassword) return
        val txt = node.text?.toString()?.trim().orEmpty()
        val desc = node.contentDescription?.toString()?.trim().orEmpty()
        if (txt.isNotBlank()) {
            out.append(txt).append('\n')
        } else if (desc.isNotBlank() && node.childCount == 0) {
            out.append(desc).append('\n')
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectSafeNodes(child, out, depth + 1)
        }
    }

    companion object {
        @Volatile
        private var instance: GoluAccessibilityVisionService? = null

        fun isServiceEnabled(context: Context): Boolean {
            val enabled =
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                ) ?: return false
            return enabled.contains(context.packageName) &&
                enabled.contains(GoluAccessibilityVisionService::class.java.simpleName)
        }

        fun captureCurrentScreenIfAuthorized(): Pair<String, String>? {
            return instance?.captureVisibleText()
        }
    }
}
