package me.efesser.flauncher.voice

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class AndroidEditableTargetTest {
    private fun node(
        packageName: String = "video.app",
        editable: Boolean = false,
        password: Boolean = false,
        visible: Boolean = true,
        focused: Boolean = false,
    ): AccessibilityNodeInfo = AccessibilityNodeInfo.obtain().apply {
        this.packageName = packageName
        isEditable = editable
        isPassword = password
        setVisibleToUser(visible)
        isFocused = focused
    }

    private fun AccessibilityNodeInfo.withChildren(vararg children: AccessibilityNodeInfo) = apply {
        for (child in children) shadowOf(this).addChild(child)
    }

    @Test
    fun prefersTheFocusedEditableFieldOfTheOriginPackage() {
        val fallback = node(editable = true)
        val focused = node(editable = true, focused = true)
        val root = node().withChildren(
            node().withChildren(fallback),
            node(packageName = "other.app", editable = true, focused = true),
            focused,
        )

        assertSame(focused, AndroidEditableTarget.find(root, "video.app"))
    }

    @Test
    fun fallsBackToAVisibleEditableFieldAndNeverToPasswordOrHiddenOnes() {
        val fallback = node(editable = true)
        val root = node().withChildren(
            node(editable = true, password = true, focused = true),
            node(editable = true, visible = false, focused = true),
            fallback,
        )

        assertSame(fallback, AndroidEditableTarget.find(root, "video.app"))
        assertNull(AndroidEditableTarget.find(node().withChildren(node(editable = true, password = true)), "video.app"))
    }

    @Test
    fun stopsWalkingHugeTreesAtTheNodeBudget() {
        val root = node()
        repeat(60) { root.withChildren(node()) }
        val editable = node(editable = true, focused = true)
        root.withChildren(editable)

        assertNull(AndroidEditableTarget.find(root, "video.app", maxNodes = 50))
        assertSame(editable, AndroidEditableTarget.find(root, "video.app", maxNodes = 100))
    }

    @Test
    fun setsTextAndPressesImeEnterOnlyWhenTheNodeAdvertisesIt() {
        val plain = node(editable = true)
        assertTrue(AndroidEditableTarget.setTextAndSubmit(plain, "матрица"))
        assertEquals(listOf(AccessibilityNodeInfo.ACTION_SET_TEXT), shadowOf(plain).performedActions)
        val performed = shadowOf(plain).performedActionsWithArgs.single()
        assertEquals(AccessibilityNodeInfo.ACTION_SET_TEXT, performed.first)
        assertEquals(
            "матрица",
            performed.second.getCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE),
        )

        val search = node(editable = true).apply { addAction(AccessibilityAction.ACTION_IME_ENTER) }
        assertTrue(AndroidEditableTarget.setTextAndSubmit(search, "матрица"))
        assertEquals(
            listOf(AccessibilityNodeInfo.ACTION_SET_TEXT, AccessibilityAction.ACTION_IME_ENTER.id),
            shadowOf(search).performedActions,
        )
    }

    @Test
    fun reportsFailureWhenTheNodeRejectsSetText() {
        val rejecting = node(editable = true)
        shadowOf(rejecting).setOnPerformActionListener { _, _ -> false }

        assertFalse(AndroidEditableTarget.setTextAndSubmit(rejecting, "матрица"))
        assertEquals(listOf(AccessibilityNodeInfo.ACTION_SET_TEXT), shadowOf(rejecting).performedActions)
    }
}
