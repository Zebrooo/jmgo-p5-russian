package me.efesser.flauncher

import kotlin.math.roundToInt

data class BatterySnapshot(
    val percent: Int?,
    val charging: Boolean,
)

object BatteryStatus {
    fun select(
        mcuCapacity: String?,
        mcuStatus: String?,
        broadcastLevel: Int?,
        broadcastScale: Int?,
        plugged: Boolean,
    ): BatterySnapshot {
        val mcuPercent = mcuCapacity
            ?.trim()
            ?.toIntOrNull()
            ?.takeIf { it in 0..100 }
        val broadcastPercent = if (
            broadcastLevel != null && broadcastLevel >= 0 &&
            broadcastScale != null && broadcastScale > 0
        ) {
            (broadcastLevel * 100.0 / broadcastScale)
                .roundToInt()
                .coerceIn(0, 100)
        } else {
            null
        }
        val charging = when (mcuStatus?.trim()?.lowercase()) {
            "charging", "full" -> true
            "discharging", "not charging" -> false
            else -> plugged
        }

        return BatterySnapshot(
            percent = mcuPercent ?: broadcastPercent,
            charging = charging,
        )
    }
}
