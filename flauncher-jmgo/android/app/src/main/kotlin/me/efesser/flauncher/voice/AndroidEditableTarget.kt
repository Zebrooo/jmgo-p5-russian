package me.efesser.flauncher.voice

import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import org.jmgo.input.core.EditableCandidate
import org.jmgo.input.core.EditableTargetPolicy

object AndroidEditableTarget {
    /** Upper bound for one breadth-first walk so a huge WebView or list tree cannot stall the service. */
    const val MAX_NODES = 1_500

    /**
     * Finds the safe editable target inside [root] for [originPackage].
     *
     * Every node visited during the walk, [root] included, is released before returning except
     * the node that is handed back to the caller. The caller owns that node and must pass it to
     * [release] once it is no longer needed.
     */
    fun find(root: AccessibilityNodeInfo, originPackage: String, maxNodes: Int = MAX_NODES): AccessibilityNodeInfo? {
        val visited = ArrayList<AccessibilityNodeInfo>(minOf(maxNodes, 256))
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.addLast(root)
        while (queue.isNotEmpty() && visited.size < maxNodes) {
            val node = queue.removeFirst()
            visited += node
            val childCount = node.childCount
            for (index in 0 until childCount) {
                if (visited.size + queue.size >= maxNodes) break
                node.getChild(index)?.let(queue::addLast)
            }
        }
        val candidates = visited.map { node ->
            EditableCandidate(
                node.packageName?.toString(),
                node.isEditable,
                node.isPassword,
                node.isVisibleToUser,
                node.isFocused || node.isAccessibilityFocused,
            )
        }
        val target = visited.getOrNull(EditableTargetPolicy.select(candidates, originPackage))
        for (node in visited) if (node !== target) release(node)
        for (node in queue) release(node)
        return target
    }

    fun setTextAndSubmit(node: AccessibilityNodeInfo, text: String): Boolean {
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val changed = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        if (!changed) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && node.actionList.any {
                it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id
            }
        ) {
            node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
        }
        return true
    }

    /** Returns the node to the framework pool on firmware older than Android 13, where it is not a no-op. */
    fun release(node: AccessibilityNodeInfo?) {
        if (node == null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        @Suppress("DEPRECATION")
        runCatching { node.recycle() }
    }
}
