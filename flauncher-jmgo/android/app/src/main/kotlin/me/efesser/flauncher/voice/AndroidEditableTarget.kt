package me.efesser.flauncher.voice

import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import org.jmgo.input.core.EditableCandidate
import org.jmgo.input.core.EditableTargetPolicy

object AndroidEditableTarget {
    fun find(root: AccessibilityNodeInfo, originPackage: String): AccessibilityNodeInfo? {
        val nodes = ArrayList<AccessibilityNodeInfo>()
        flatten(root, nodes)
        val candidates = nodes.map { node ->
            EditableCandidate(
                node.packageName?.toString(),
                node.isEditable,
                node.isPassword,
                node.isVisibleToUser,
                node.isFocused || node.isAccessibilityFocused,
            )
        }
        return nodes.getOrNull(EditableTargetPolicy.select(candidates, originPackage))
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

    private fun flatten(node: AccessibilityNodeInfo, output: MutableList<AccessibilityNodeInfo>) {
        output += node
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { flatten(it, output) }
        }
    }
}
