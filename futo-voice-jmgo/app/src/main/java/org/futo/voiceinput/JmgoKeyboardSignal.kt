package org.futo.voiceinput

object JmgoKeyboardSignal {
    const val ACTION_LEFT = "org.futo.voiceinput.jmgo.keyboard.LEFT"
    const val ACTION_RIGHT = "org.futo.voiceinput.jmgo.keyboard.RIGHT"
    const val ACTION_UP = "org.futo.voiceinput.jmgo.keyboard.UP"
    const val ACTION_DOWN = "org.futo.voiceinput.jmgo.keyboard.DOWN"
    const val ACTION_SELECT = "org.futo.voiceinput.jmgo.keyboard.SELECT"

    val actions = listOf(ACTION_LEFT, ACTION_RIGHT, ACTION_UP, ACTION_DOWN, ACTION_SELECT)

    fun directionForAction(action: String?): JmgoKeyboardDirection? = when (action) {
        ACTION_LEFT -> JmgoKeyboardDirection.LEFT
        ACTION_RIGHT -> JmgoKeyboardDirection.RIGHT
        ACTION_UP -> JmgoKeyboardDirection.UP
        ACTION_DOWN -> JmgoKeyboardDirection.DOWN
        else -> null
    }
}
