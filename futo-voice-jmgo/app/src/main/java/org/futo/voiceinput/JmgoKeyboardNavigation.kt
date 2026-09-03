package org.futo.voiceinput

import kotlin.math.roundToInt

data class JmgoKeyboardSelection(val row: Int, val column: Int)

enum class JmgoKeyboardDirection {
    LEFT,
    RIGHT,
    UP,
    DOWN,
}

enum class JmgoKeyboardKey {
    TEXT,
    LANGUAGE,
    NUMBERS,
    SPACE,
    BACKSPACE,
    HIDE,
    ENTER,
}

object JmgoKeyboardNavigation {
    private const val ACTION_KEY_COUNT = 6

    fun move(
        rows: List<List<String>>,
        selection: JmgoKeyboardSelection,
        direction: JmgoKeyboardDirection,
    ): JmgoKeyboardSelection {
        val lastRow = rows.size
        val currentRow = selection.row.coerceIn(0, lastRow)
        val currentCount = keyCount(rows, currentRow)
        val currentColumn = selection.column.coerceIn(0, currentCount - 1)

        return when (direction) {
            JmgoKeyboardDirection.LEFT ->
                JmgoKeyboardSelection(currentRow, (currentColumn - 1).coerceAtLeast(0))
            JmgoKeyboardDirection.RIGHT ->
                JmgoKeyboardSelection(currentRow, (currentColumn + 1).coerceAtMost(currentCount - 1))
            JmgoKeyboardDirection.UP -> moveVertically(rows, currentRow, currentColumn, -1)
            JmgoKeyboardDirection.DOWN -> moveVertically(rows, currentRow, currentColumn, 1)
        }
    }

    fun keyAt(rows: List<List<String>>, selection: JmgoKeyboardSelection): JmgoKeyboardKey {
        if (selection.row < rows.size) return JmgoKeyboardKey.TEXT
        return when (selection.column.coerceIn(0, ACTION_KEY_COUNT - 1)) {
            0 -> JmgoKeyboardKey.LANGUAGE
            1 -> JmgoKeyboardKey.NUMBERS
            2 -> JmgoKeyboardKey.SPACE
            3 -> JmgoKeyboardKey.BACKSPACE
            4 -> JmgoKeyboardKey.HIDE
            else -> JmgoKeyboardKey.ENTER
        }
    }

    fun textAt(rows: List<List<String>>, selection: JmgoKeyboardSelection): String? =
        rows.getOrNull(selection.row)?.getOrNull(selection.column)

    fun clamp(
        rows: List<List<String>>,
        selection: JmgoKeyboardSelection,
    ): JmgoKeyboardSelection {
        val row = selection.row.coerceIn(0, rows.size)
        return JmgoKeyboardSelection(row, selection.column.coerceIn(0, keyCount(rows, row) - 1))
    }

    private fun moveVertically(
        rows: List<List<String>>,
        row: Int,
        column: Int,
        delta: Int,
    ): JmgoKeyboardSelection {
        val targetRow = (row + delta).coerceIn(0, rows.size)
        if (targetRow == row) return JmgoKeyboardSelection(row, column)

        val oldCount = keyCount(rows, row)
        val newCount = keyCount(rows, targetRow)
        val relativePosition = if (oldCount <= 1) 0.0 else column.toDouble() / (oldCount - 1)
        val targetColumn = (relativePosition * (newCount - 1)).roundToInt()
        return JmgoKeyboardSelection(targetRow, targetColumn)
    }

    private fun keyCount(rows: List<List<String>>, row: Int): Int =
        if (row < rows.size) rows[row].size.coerceAtLeast(1) else ACTION_KEY_COUNT
}
